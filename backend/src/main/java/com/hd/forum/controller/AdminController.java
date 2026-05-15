package com.hd.forum.controller;

import com.hd.forum.common.Result;
import com.hd.forum.common.annotation.RequireAdmin;
import com.hd.forum.common.annotation.RequireSuperAdmin;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.dto.BoardRequest;
import com.hd.forum.dto.ModeratorAppointRequest;
import com.hd.forum.entity.Comment;
import com.hd.forum.entity.Post;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.CommentMapper;
import com.hd.forum.mapper.PostMapper;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.ICommentService;
import com.hd.forum.service.IUserService;
import com.hd.forum.service.impl.BoardServiceImpl;
import com.hd.forum.service.impl.PostServiceImpl;
import com.hd.forum.utils.SecurityUtils;
import com.hd.forum.vo.CommentManageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员后台接口。
 *
 * 主要能力：
 * - 帖子管理：删除任意帖子、置顶/取消置顶
 * - 板块管理：新增/删除板块
 * - 用户管理：任命/撤销版主、禁用/启用用户
 * - 评论管理：分页查询评论（用于后台审查）
 *
 * 权限说明：
 * - 通过 @RequireAdmin 进行管理员权限保护（AOP 拦截）
 */
@Tag(name = "管理员后台")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PostServiceImpl postService;
    private final BoardServiceImpl boardService;
    private final IUserService userService;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final ICommentService commentService;

    // 内容审核可以抽象为对不正当的帖子进行删除
    @RequireAdmin
    @Operation(summary = "删除任意帖子")
    @DeleteMapping("/post/{id}")
    public com.hd.forum.common.Result<String> deletePost(@PathVariable Long id) {
        postService.deletePostByAdmin(id);
        return com.hd.forum.common.Result.success("删除成功");
    }

    @RequireAdmin
    @Operation(summary = "置顶/取消置顶帖子")
    @PostMapping("/post/top/{id}")
    public com.hd.forum.common.Result<String> toggleTop(@PathVariable Long id) {
        postService.toggleTop(id);
        return com.hd.forum.common.Result.success("操作成功");
    }

    @RequireSuperAdmin
    @Operation(summary = "添加新板块")
    @PostMapping("/board/add")
    public com.hd.forum.common.Result<String> addBoard(@RequestBody @Valid BoardRequest req) {
        boardService.createBoard(req);
        return com.hd.forum.common.Result.success("板块创建成功");
    }

    @RequireSuperAdmin
    @Operation(summary = "修改板块信息")
    @PutMapping("/board/{id}")
    public com.hd.forum.common.Result<String> updateBoard(@PathVariable Integer id, @RequestBody @Valid BoardRequest req) {
        boardService.updateBoard(id, req);
        return com.hd.forum.common.Result.success("板块修改成功");
    }

    @RequireSuperAdmin
    @Operation(summary = "删除板块")
    @DeleteMapping("/board/{id}")
    public com.hd.forum.common.Result<String> deleteBoard(@PathVariable Integer id) {
        boardService.deleteBoard(id);
        return com.hd.forum.common.Result.success("板块删除成功");
    }

    @RequireSuperAdmin
    @Operation(summary = "修改用户状态 (禁用/启用)")
    @PostMapping("/user/status/{id}")
    public com.hd.forum.common.Result<String> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return com.hd.forum.common.Result.success("操作成功");
    }

    @RequireAdmin
    @Operation(summary = "分页查询评论(用于管理)")
    @GetMapping("/comment/list")
    public com.hd.forum.common.Result<Page<CommentManageVO>> commentList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) Long postId
    ) {
        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = userMapper.selectById(currentUserId);
        Integer managedBoardId = (currentUser != null && "MODERATOR".equals(currentUser.getRole())) ? currentUser.getManagedBoardId() : null;

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(postId != null, Comment::getPostId, postId);
        String k = keyword == null ? null : keyword.trim();
        boolean hasKeyword = k != null && !k.isEmpty();
        if (postId == null && hasKeyword) {
            if ("postId".equals(searchField)) {
                if (k.matches("^\\d+$")) {
                    wrapper.eq(Comment::getPostId, Long.parseLong(k));
                } else {
                    return com.hd.forum.common.Result.success(new Page<>());
                }
            } else if ("author".equals(searchField)) {
                List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                                .and(w -> w.like(User::getNickname, k).or().like(User::getUsername, k)))
                        .stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
                if (userIds.isEmpty()) {
                    return com.hd.forum.common.Result.success(new Page<>());
                }
                wrapper.in(Comment::getUserId, userIds);
            } else if ("postTitle".equals(searchField)) {
                LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<Post>()
                        .like(Post::getTitle, k);
                postWrapper.eq(managedBoardId != null, Post::getBoardId, managedBoardId);
                List<Long> postIds = postMapper.selectList(postWrapper).stream().map(Post::getId).collect(Collectors.toList());
                if (postIds.isEmpty()) {
                    return com.hd.forum.common.Result.success(new Page<>());
                }
                wrapper.in(Comment::getPostId, postIds);
            } else {
                wrapper.like(Comment::getContent, k);
            }
        }
        if (managedBoardId != null) {
            wrapper.inSql(Comment::getPostId, "SELECT id FROM forum_post WHERE board_id = " + managedBoardId);
        }
        wrapper.orderByDesc(Comment::getCreateTime).orderByDesc(Comment::getId);

        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return com.hd.forum.common.Result.success(new Page<>());
        }

        Set<Long> postIds = page.getRecords().stream().map(Comment::getPostId).collect(Collectors.toSet());
        Set<Long> userIds = page.getRecords().stream().map(Comment::getUserId).collect(Collectors.toSet());

        Map<Long, Post> postMap = postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (l, r) -> l));

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (l, r) -> l));

        // 批量查询父评论内容
        Set<Long> parentIds = page.getRecords().stream()
                .map(Comment::getParentId)
                .filter(pid -> pid != null && pid > 0)
                .collect(Collectors.toSet());
        Map<Long, String> parentContentMap = parentIds.isEmpty() ? Map.of() :
                commentMapper.selectBatchIds(parentIds).stream()
                        .collect(Collectors.toMap(Comment::getId, Comment::getContent, (l, r) -> l));

        List<CommentManageVO> voList = page.getRecords().stream().map(c -> {
            CommentManageVO vo = new CommentManageVO();
            BeanUtils.copyProperties(c, vo);
            Post p = postMap.get(c.getPostId());
            if (p != null) {
                vo.setPostTitle(p.getTitle());
                vo.setBoardId(p.getBoardId());
            }
            User u = userMap.get(c.getUserId());
            if (u != null) {
                vo.setAuthorName(u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername());
                vo.setAuthorAvatar(u.getAvatar());
            }
            // 填充父评论预览
            if (c.getParentId() != null && c.getParentId() > 0) {
                vo.setParentContent(parentContentMap.get(c.getParentId()));
            }
            return vo;
        }).collect(Collectors.toList());

        Page<CommentManageVO> result = new Page<>();
        BeanUtils.copyProperties(page, result, "records");
        result.setRecords(voList);
        return com.hd.forum.common.Result.success(result);
    }

    @RequireAdmin
    @Operation(summary = "删除评论(管理员/版主)")
    @DeleteMapping("/comment/{id}")
    public com.hd.forum.common.Result<String> deleteComment(@PathVariable Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return com.hd.forum.common.Result.success("删除成功");
        }

        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser != null && "MODERATOR".equals(currentUser.getRole())) {
            Post post = postMapper.selectById(comment.getPostId());
            if (post == null || !post.getBoardId().equals(currentUser.getManagedBoardId())) {
                throw new BusinessException(403, "无权删除该评论");
            }
        }

        commentService.deleteComment(id);
        return com.hd.forum.common.Result.success("删除成功");
    }
}

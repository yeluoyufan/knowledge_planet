package com.hd.forum.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.common.Result;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.entity.Board;
import com.hd.forum.entity.Favorite;
import com.hd.forum.entity.Post;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.BoardMapper;
import com.hd.forum.mapper.CommentMapper;
import com.hd.forum.mapper.FavoriteMapper;
import com.hd.forum.mapper.PostMapper;
import com.hd.forum.mapper.TagMapper;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.IMessageService;
import com.hd.forum.utils.SecurityUtils;
import com.hd.forum.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收藏接口。
 *
 * 功能：
 * - 收藏/取消收藏
 * - 获取当前用户的收藏列表（分页，返回 PostVO）
 *
 * 说明：
 * - 收藏关系存储在 Favorite 表中（userId -> postId）
 * - 收藏列表需要补全作者、板块、标签、评论数等信息，因此在接口内部做了批量查询组装
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "收藏")
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final BoardMapper boardMapper;
    private final IMessageService messageService;

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/toggle/{postId}")
    @Transactional
    public Result<Boolean> toggle(@PathVariable Long postId) {
        Long userId = SecurityUtils.getUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != null && post.getStatus() != 1) {
            throw new BusinessException("帖子未发布，暂不可收藏");
        }

        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getPostId, postId));
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());

            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .setSql("favorite_count = IF(IFNULL(favorite_count,0) > 0, favorite_count - 1, 0)"));
            return Result.success(false);
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        favoriteMapper.insert(favorite);

        // 发送通知
        if (!post.getUserId().equals(userId)) {
            User sender = userMapper.selectById(userId);
            String senderName = (sender != null && sender.getNickname() != null && !sender.getNickname().isBlank()) 
                ? sender.getNickname() : (sender != null ? sender.getUsername() : "未知用户");
            String today = java.time.LocalDate.now().toString();
            messageService.createSystemNotification(userId, post.getUserId(), "FAVORITE", 
                "用户 " + senderName + " 于 " + today + " 收藏了你的帖子《" + post.getTitle() + "》。");
        }

        return Result.success(true);
    }

    @Operation(summary = "查询是否已收藏")
    @GetMapping("/status/{postId}")
    public Result<Boolean> status(@PathVariable Long postId) {
        Long userId = SecurityUtils.getUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || (post.getStatus() != null && post.getStatus() != 1)) {
            return Result.success(false);
        }
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getPostId, postId));
        return Result.success(exist != null);
    }

    @Operation(summary = "分页获取我的收藏列表")
    @GetMapping("/list")
    public Result<Page<PostVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Long userId = SecurityUtils.getUserId();
        Page<Favorite> favoritePage = favoriteMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime)
                        .orderByDesc(Favorite::getId));

        if (favoritePage.getRecords().isEmpty()) {
            return Result.success(new Page<>());
        }

        List<Long> postIdsOrdered = favoritePage.getRecords().stream().map(Favorite::getPostId).collect(Collectors.toList());
        List<Post> posts = postMapper.selectBatchIds(postIdsOrdered).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .collect(Collectors.toList());

        if (posts.isEmpty()) {
            return Result.success(new Page<>());
        }

        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Set<Long> authorIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Set<Integer> boardIds = posts.stream().map(Post::getBoardId).collect(Collectors.toSet());

        Map<Long, User> userMap = authorIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : userMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (l, r) -> l));
        Map<Integer, Board> boardMap = boardIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : boardMapper.selectBatchIds(boardIds).stream()
                        .collect(Collectors.toMap(Board::getId, b -> b, (l, r) -> l));

        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p, (l, r) -> l));
        List<Post> ordered = postIdsOrdered.stream()
                .map(postMap::get)
                .filter(p -> p != null && p.getStatus() != null && p.getStatus() == 1)
                .collect(Collectors.toList());

        List<PostVO> voList = ordered.stream().map(p -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(p, vo);
            User u = userMap.get(p.getUserId());
            if (u != null) {
                vo.setAuthorName(u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername());
                vo.setAuthorAvatar(u.getAvatar());
            }
            Board b = boardMap.get(p.getBoardId());
            if (b != null) {
                vo.setBoardName(b.getName());
            }
            vo.setTags(p.getTags() == null ? List.of() : p.getTags());
            vo.setReplyCount(p.getReplyCount() == null ? 0 : p.getReplyCount());
            return vo;
        }).collect(Collectors.toList());

        Page<PostVO> result = new Page<>();
        BeanUtils.copyProperties(favoritePage, result, "records");
        result.setRecords(voList);
        return Result.success(result);
    }
}

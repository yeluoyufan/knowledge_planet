package com.hd.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.dto.CommentCreateRequest;
import com.hd.forum.entity.Comment;
import com.hd.forum.entity.Message;
import com.hd.forum.entity.Post;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.CommentMapper;
import com.hd.forum.mapper.PostMapper;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.ICommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.forum.service.IMessageService;
import com.hd.forum.utils.SecurityUtils;
import com.hd.forum.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论业务服务实现类。
 *
 * 关键点：
 * - 评论支持“楼中楼”，通过 parentId/rootId 维护层级关系
 * - 新增评论后会更新帖子的评论数，并触发站内消息通知（回复提醒）
 * - 删除评论时做作者权限校验，防止越权删除
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final IMessageService messageService;

    /**
     * 发表评论。
     * 
     * 关键流程：
     * 1. 帖子状态校验：仅允许在已发布 (status=1) 的帖子下评论。
     * 2. 层级计算：
     *    - 一级评论：parentId=0, rootId=0。
     *    - 二级及以上评论：继承根评论的 rootId，parentId 指向直接父级。
     * 3. 内容增强：回复他人时自动在内容前增加 "@昵称" 标识。
     * 4. 统计更新：帖子 reply_count 实时累加。
     * 5. 通知推送：
     *    - 一级评论：通知帖主。
     *    - 回复评论：通知被回复者。
     */
    @Transactional(rollbackFor = Exception.class)
    public void createComment(CommentCreateRequest req) {
        Post post = postMapper.selectById(req.postId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != null && post.getStatus() != 1) {
            throw new BusinessException("帖子未发布，暂不可评论");
        }

        String finalContent = req.content();
        Long rootId = 0L; // 默认为根评论

        // 如果是回复评论
        if (req.parentId() != null && req.parentId() != 0) {
            Comment parent = commentMapper.selectById(req.parentId());
            if (parent == null) {
                throw new BusinessException("回复的评论不存在了");
            }

            // A. 计算 rootId
            if (parent.getRootId() == 0) {
                // 父评论本身就是根 -> 那我就是它的儿子，rootId = 父ID
                rootId = parent.getId();
            } else {
                // 父评论也是子评论 -> 那我们同属一个根，rootId 继承
                rootId = parent.getRootId();
            }
        }

        Comment comment = new Comment();
        comment.setPostId(req.postId());
        comment.setContent(finalContent);
        comment.setUserId(SecurityUtils.getUserId());
        comment.setParentId(req.parentId());
        comment.setRootId(rootId); // 设置计算好的 rootId

        this.save(comment);

        // 帖子总回复数 +1
        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("reply_count = IFNULL(reply_count, 0) + 1").eq(Post::getId, req.postId());
        postMapper.update(null, updateWrapper);

        Long currentUserId = SecurityUtils.getUserId();

        // A. 场景：给“帖主”发通知 (前提：帖主不是我自己,并且是一级评论)
        if (!post.getUserId().equals(currentUserId) && comment.getRootId().equals(0L)) {
            String summary = req.content().length() > 20 ? req.content().substring(0, 20) + "..." : req.content();
            messageService.createSystemNotification(currentUserId, post.getUserId(), "COMMENT",
                    "你的帖子《" + post.getTitle() + "》收到了新评论：" + summary);
        }

        // B. 场景：给“被回复人”发通知 (前提：被回复人不是我自己)
        if (req.parentId() != null && req.parentId() != 0) {
            Comment parent = commentMapper.selectById(req.parentId());
            if (parent != null && !parent.getUserId().equals(currentUserId)) {
                String summary = req.content().length() > 20 ? req.content().substring(0, 20) + "..." : req.content();
                // 明确通知类型为 COMMENT
                messageService.createSystemNotification(currentUserId, parent.getUserId(), "COMMENT",
                        "你的评论收到了回复：" + summary);
            }
        }
    }

    /**
     * 删除评论。
     * 
     * 权限规则：
     * 1. 评论作者本人。
     * 2. 站点管理员 (ADMIN)。
     * 3. 评论所在帖子的板块版主 (MODERATOR)。
     * 
     * 特殊逻辑：
     * - 如果删除的是根评论 (rootId=0)，则会联带删除其下的所有回复。
     * - 删除后会扣减对应帖子的 reply_count 统计字段。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long currentUserId = SecurityUtils.getUserId();
        Comment target = this.getById(commentId);
        if (target == null) {
            throw new BusinessException("评论不存在");
        }

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isAuthor = target.getUserId().equals(currentUserId);
        
        // 版主权限判断：需校验该评论所在帖子的板块
        Post post = postMapper.selectById(target.getPostId());
        boolean isBoardModerator = "MODERATOR".equals(currentUser.getRole()) 
                && post != null && post.getBoardId().equals(currentUser.getManagedBoardId());

        if (!isAuthor && !isAdmin && !isBoardModerator) {
            throw new BusinessException(403, "无权删除该评论");
        }

        // 收集待删除的 ID
        List<Long> deleteIds = new ArrayList<>();
        deleteIds.add(commentId);

        // 递归逻辑：根评论删除则其子孙全部删除
        if (target.getRootId() == 0) {
            List<Comment> children = this.list(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getRootId, commentId));
            if (!children.isEmpty()) {
                deleteIds.addAll(children.stream().map(Comment::getId).collect(Collectors.toList()));
            }
        }

        // 物理删除 (或根据需求改为逻辑删除)
        this.removeBatchByIds(deleteIds);

        // 维护帖子回复数统计
        if (post != null) {
            LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("reply_count = CASE WHEN reply_count >= " + deleteIds.size() +
                            " THEN reply_count - " + deleteIds.size() + " ELSE 0 END")
                    .eq(Post::getId, post.getId());
            postMapper.update(null, updateWrapper);
        }
    }

    // 3. 获取某帖子的评论列表
    public List<CommentVO> getCommentsByPostId(Long postId) {
        Post post = postMapper.selectById(postId);
        ensureCanViewPost(post);

        // A. 只查根评论
        List<Comment> rootComments = this.list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getRootId, 0)
                .orderByDesc(Comment::getCreateTime)); // 根评论一般按时间倒序或热度

        if (rootComments.isEmpty()) {
            return new ArrayList<>();
        }

        return convertToVOList(rootComments, true);
    }

    // 4. 获取某个根评论下的子评论列表 (查 rootId = ?)
    /**
     * 获取指定根评论下的子评论列表。
     * 采用平铺结构返回，前端根据 parentId 展示层级关系。
     */
    public List<CommentVO> getSubComments(Long rootId) {
        Comment rootComment = this.getById(rootId);
        if (rootComment == null) {
            return new ArrayList<>();
        }
        Post post = postMapper.selectById(rootComment.getPostId());
        ensureCanViewPost(post);

        List<Comment> subComments = this.list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, rootId)
                .orderByAsc(Comment::getCreateTime)); // 子评论通常按时间正序排列

        if (subComments.isEmpty()) {
            return new ArrayList<>();
        }

        return convertToVOList(subComments, false);
    }

    /**
     * 将评论实体列表转换为 VO 列表。
     * 封装作者信息、子评论数量等前端展示所需的数据。
     */
    private List<CommentVO> convertToVOList(List<Comment> comments, boolean countChildren) {
        if (comments.isEmpty()) return new ArrayList<>();

        // 1. 批量查询当前评论的作者信息
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        
        // 2. 收集所有父评论 ID，用于查询被回复人信息
        Set<Long> parentIds = comments.stream()
                .map(Comment::getParentId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());
        
        Map<Long, Comment> parentCommentMap = parentIds.isEmpty() ? Map.of() :
                this.listByIds(parentIds).stream().collect(Collectors.toMap(Comment::getId, c -> c));
        
        // 将被回复人的用户 ID 也加入查询列表
        parentCommentMap.values().forEach(pc -> userIds.add(pc.getUserId()));

        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 3. 批量查询子评论数（仅对根评论有效）
        Map<Long, Long> childCountMap = new HashMap<>();
        if (countChildren) {
            List<Long> rootIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
            if (!rootIds.isEmpty()) {
                // 使用 QueryWrapper 配合 selectMaps 进行分组统计
                QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("root_id", rootIds)
                        .select("root_id as rootId", "COUNT(*) as count")
                        .groupBy("root_id");
                
                List<Map<String, Object>> counts = commentMapper.selectMaps(queryWrapper);
                for (Map<String, Object> map : counts) {
                    Long rId = ((Number) map.get("rootId")).longValue();
                    Long count = ((Number) map.get("count")).longValue();
                    childCountMap.put(rId, count);
                }
            }
        }

        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(c, vo);
            
            // 设置评论作者信息
            User u = userMap.get(c.getUserId());
            if (u != null) {
                vo.setAuthorName(u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername());
                vo.setAuthorAvatar(u.getAvatar());
            }
            
            // 设置被回复人信息
            if (c.getParentId() != null && c.getParentId() != 0) {
                Comment parent = parentCommentMap.get(c.getParentId());
                if (parent != null) {
                    User replyToUser = userMap.get(parent.getUserId());
                    if (replyToUser != null) {
                        vo.setReplyToUserId(replyToUser.getId());
                        vo.setReplyToUserNickname(replyToUser.getNickname() != null && !replyToUser.getNickname().isBlank() 
                                ? replyToUser.getNickname() : replyToUser.getUsername());
                    }
                }
            }
            
            if (countChildren) {
                vo.setChildCount(childCountMap.getOrDefault(c.getId(), 0L));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private void ensureCanViewPost(Post post) {
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != null && post.getStatus() == 1) {
            return;
        }
        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录或不存在");
        }
        boolean isAuthor = post.getUserId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isBoardModerator = "MODERATOR".equals(currentUser.getRole())
                && Objects.equals(post.getBoardId(), currentUser.getManagedBoardId());
        if (!isAuthor && !isAdmin && !isBoardModerator) {
            throw new BusinessException(403, "无权查看该帖评论");
        }
    }
}

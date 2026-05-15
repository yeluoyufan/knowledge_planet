package com.hd.forum.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hd.forum.common.Result;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.entity.Message;
import com.hd.forum.entity.Post;
import com.hd.forum.entity.PostLike;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.PostLikeMapper;
import com.hd.forum.mapper.PostMapper;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.IMessageService;
import com.hd.forum.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

/**
 * 点赞接口。
 *
 * 说明：
 * - 点赞关系存储在 PostLike 表中（userId -> postId）
 * - 同时维护帖子表的 like_count 统计字段，避免列表页频繁聚合查询
 * - 点赞成功后会给帖子作者发送通知（给自己点赞不通知）
 */
@Tag(name = "点赞")
@RestController
@RequestMapping("/api/like")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeMapper postLikeMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final IMessageService messageService;

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/toggle/{postId}")
    @Transactional
    public Result<Boolean> toggle(@PathVariable Long postId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != null && post.getStatus() != 1) {
            throw new BusinessException("帖子未发布，暂不可点赞");
        }

        PostLike exist = postLikeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getUserId, userId)
                .eq(PostLike::getPostId, postId));
        
        if (exist != null) {
            postLikeMapper.deleteById(exist.getId());

            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .setSql("like_count = IF(IFNULL(like_count,0) > 0, like_count - 1, 0)"));
            return Result.success(false);
        } else {
            // 点赞
            PostLike postLike = new PostLike();
            postLike.setUserId(userId);
            postLike.setPostId(postId);
            postLikeMapper.insert(postLike);
            
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .setSql("like_count = IFNULL(like_count, 0) + 1"));

            // 发送通知
            if (!post.getUserId().equals(userId)) {
                User sender = userMapper.selectById(userId);
                String senderName = (sender != null && sender.getNickname() != null && !sender.getNickname().isBlank()) 
                    ? sender.getNickname() : (sender != null ? sender.getUsername() : "未知用户");
                String today = java.time.LocalDate.now().toString();
                messageService.createSystemNotification(userId, post.getUserId(), "LIKE", 
                    "用户 " + senderName + " 于 " + today + " 点赞了你的帖子《" + post.getTitle() + "》。");
            }
            
            return Result.success(true);
        }
    }

    @Operation(summary = "查询是否已点赞")
    @GetMapping("/status/{postId}")
    public Result<Boolean> status(@PathVariable Long postId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return Result.success(false);
        }
        Post post = postMapper.selectById(postId);
        if (post == null || (post.getStatus() != null && post.getStatus() != 1)) {
            return Result.success(false);
        }
        PostLike exist = postLikeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getUserId, userId)
                .eq(PostLike::getPostId, postId));
        return Result.success(exist != null);
    }
}

package com.hd.forum.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.common.Result;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.entity.Follow;
import com.hd.forum.entity.Message;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.FollowMapper;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.IMessageService;
import com.hd.forum.utils.SecurityUtils;
import com.hd.forum.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注关系接口。
 *
 * 功能：
 * - 关注/取消关注
 * - 查询某用户的粉丝列表与关注列表（分页）
 *
 * 说明：
 * - Follow 表存储 followerId -> followeeId 的关系
 * - fansCount / followCount 为用户表冗余计数字段，关注/取关时同步更新
 */
@Tag(name = "关注")
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final IMessageService messageService;

    @Operation(summary = "关注/取消关注")
    @PostMapping("/toggle/{userId}")
    @Transactional
    public Result<Boolean> toggle(@PathVariable Long userId) {
        Long followerId = SecurityUtils.getUserId();
        if (followerId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (followerId.equals(userId)) {
            throw new BusinessException("不能关注自己");
        }

        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }

        Follow exist = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, userId));
        if (exist != null) {
            followMapper.deleteById(exist.getId());

            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, followerId)
                    .setSql("follow_count = IF(IFNULL(follow_count,0) > 0, follow_count - 1, 0)"));
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .setSql("fans_count = IF(IFNULL(fans_count,0) > 0, fans_count - 1, 0)"));
            return Result.success(false);
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(userId);
        followMapper.insert(follow);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, followerId)
                .setSql("follow_count = IFNULL(follow_count,0) + 1"));
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("fans_count = IFNULL(fans_count,0) + 1"));

        String targetName = target.getNickname() != null && !target.getNickname().isBlank()
                ? target.getNickname()
                : target.getUsername();
        messageService.createSystemNotification(userId, followerId, "CHAT",
                "[AUTO_FOLLOW_THANKS]你好，我是" + targetName + "，感谢你的关注，我将继续创作更好的内容！");

        return Result.success(true);
    }

    @Operation(summary = "查询是否已关注")
    @GetMapping("/status/{userId}")
    public Result<Boolean> status(@PathVariable Long userId) {
        Long followerId = SecurityUtils.getUserId();
        if (followerId == null) {
            return Result.success(false);
        }
        Follow exist = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, userId));
        return Result.success(exist != null);
    }

    @Operation(summary = "热门作者(按粉丝数)")
    @GetMapping("/hot")
    public Result<List<UserVO>> hot(@RequestParam(defaultValue = "3") Integer limit) {
        int size = limit == null ? 3 : Math.min(Math.max(limit, 1), 10);
        
        // 性能优化：只查询必要的字段，避免拉取 bio 等大字段
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname, User::getAvatar, User::getFansCount, User::getRole)
                .eq(User::getStatus, 0)
                .ne(User::getRole, "ADMIN")
                .orderByDesc(User::getFansCount)
                .orderByDesc(User::getId);
                
        Page<User> page = userMapper.selectPage(new Page<>(1, size), wrapper);
        
        List<UserVO> voList = page.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).collect(java.util.stream.Collectors.toList());
        
        return Result.success(voList);
    }

    @Operation(summary = "查询用户的粉丝列表")
    @GetMapping("/fans/{userId}")
    public Result<Page<UserVO>> fans(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<Follow> followPage = followMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Follow>().eq(Follow::getFolloweeId, userId).orderByDesc(Follow::getCreateTime));
        
        Page<UserVO> resultPage = new Page<>(pageNum, pageSize, followPage.getTotal());
        if (followPage.getRecords().isEmpty()) return Result.success(resultPage);

        List<Long> followerIds = followPage.getRecords().stream().map(Follow::getFollowerId).collect(java.util.stream.Collectors.toList());
        java.util.Map<Long, User> userMap = userMapper.selectBatchIds(followerIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (l, r) -> l));

        List<UserVO> voList = followerIds.stream()
                .map(userMap::get)
                .filter(java.util.Objects::nonNull)
                .map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).collect(java.util.stream.Collectors.toList());
        
        resultPage.setRecords(voList);
        return Result.success(resultPage);
    }

    @Operation(summary = "查询用户的关注列表")
    @GetMapping("/followees/{userId}")
    public Result<Page<UserVO>> followees(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<Follow> followPage = followMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId).orderByDesc(Follow::getCreateTime));

        Page<UserVO> resultPage = new Page<>(pageNum, pageSize, followPage.getTotal());
        if (followPage.getRecords().isEmpty()) return Result.success(resultPage);

        List<Long> followeeIds = followPage.getRecords().stream().map(Follow::getFolloweeId).collect(java.util.stream.Collectors.toList());
        java.util.Map<Long, User> userMap = userMapper.selectBatchIds(followeeIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (l, r) -> l));

        List<UserVO> voList = followeeIds.stream()
                .map(userMap::get)
                .filter(java.util.Objects::nonNull)
                .map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).collect(java.util.stream.Collectors.toList());

        resultPage.setRecords(voList);
        return Result.success(resultPage);
    }
}

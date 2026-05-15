package com.hd.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.dto.PasswordUpdateRequest;
import com.hd.forum.dto.UserUpdateRequest;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.IMessageService;
import com.hd.forum.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.forum.utils.SecurityUtils;
import com.hd.forum.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务服务实现类。
 *
 * 主要负责：
 * - 个人主页信息查询（返回 UserVO，避免暴露敏感字段）
 * - 修改个人资料、修改密码（从 Token 获取当前用户身份，防止越权）
 * - 管理后台用户管理能力（角色筛选、禁用/启用、任命版主等）
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private static final String DEFAULT_BIO = "这个用户很懒，没有介绍自己...";

    private final PasswordEncoder passwordEncoder;
    private final IMessageService messageService;

    /**
     * 获取用户详情（用于个人主页）。
     * 返回 UserVO，避免把 password 等敏感字段返回到前端。
     */
    @Override
    public UserVO getUserProfile(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 转换为 VO，自动脱敏密码等信息
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (!StringUtils.hasText(vo.getBio())) {
            vo.setBio(DEFAULT_BIO);
        }
        return vo;
    }

    /**
     * 修改个人资料。
     *
     * 业务规则：
     * - 从 SecurityContext 中获取当前用户 ID，确保只能修改自己的资料。
     * - 更新昵称、头像、邮箱和个人简介。
     * - 个人简介若为空，则填充默认值。
     */
    @Override
    @Transactional
    public void updateUserInfo(UserUpdateRequest req) {
        Long currentUserId = SecurityUtils.getUserId();
        User user = this.getById(currentUserId);

        if (user == null) {
            throw new BusinessException("用户不存在或未登录");
        }

        // 仅允许修改以下字段
        user.setNickname(req.getNickname());
        user.setAvatar(req.getAvatar());
        user.setEmail(req.getEmail());
        user.setBio(StringUtils.hasText(req.getBio()) ? req.getBio().trim() : DEFAULT_BIO);

        this.updateById(user);
    }

    // 分页查询用户列表 (支持昵称/用户名搜索, 角色筛选)
    @Override
    public Page<UserVO> getUserList(Page<User> page, String keyword, Integer managedBoardId, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 如果有搜索关键词，则模糊匹配 用户名 OR 昵称
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }

        // 如果传了 managedBoardId，按板块筛选 (通常用于查当前板主)
        if (managedBoardId != null) {
            wrapper.eq(User::getManagedBoardId, managedBoardId);
        }

        // 如果传了 role，按角色筛选
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }

        wrapper.last("ORDER BY CASE role WHEN 'ADMIN' THEN 1 WHEN 'MODERATOR' THEN 2 ELSE 3 END ASC, create_time DESC, id DESC");

        Page<User> userPage = this.page(page, wrapper);
        return convertToVOPage(userPage);
    }

    @Override
    public Page<UserVO> searchUsers(Page<User> page, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new Page<>();
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword))
                .eq(User::getStatus, 0) // 只能搜索到未封禁的用户
                .orderByDesc(User::getFansCount) // 按粉丝数排序，体现“达人”优先
                .orderByDesc(User::getCreateTime);

        Page<User> userPage = this.page(page, wrapper);
        return convertToVOPage(userPage);
    }

    private Page<UserVO> convertToVOPage(Page<User> userPage) {
        Page<UserVO> voPage = new Page<>();
        BeanUtils.copyProperties(userPage, voPage, "records");

        List<UserVO> voList = userPage.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            if (!StringUtils.hasText(vo.getBio())) {
                vo.setBio(DEFAULT_BIO);
            }
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    // 修改密码
    @Override
    @Transactional
    public void updatePassword(PasswordUpdateRequest req) {
        Long userId = SecurityUtils.getUserId();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 1. 校验两次新密码是否一致
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        // 2. 校验旧密码是否正确
        // passwordEncoder.matches(明文, 数据库里的密文)
        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 3. 校验新密码不能和旧密码相同 (可选，为了安全建议加上)
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        // 4. 加密并更新
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        this.updateById(user);
    }

    // 管理员：切换用户状态 (禁用/启用)
    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        assertAdminOnly();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 0-正常, 1-禁用
        int newStatus = (user.getStatus() == null || user.getStatus() == 0) ? 1 : 0;
        user.setStatus(newStatus);
        this.updateById(user);
        
        String today = java.time.LocalDate.now().toString();
        String statusText = (newStatus == 1) ? "封禁" : "解除封禁";
        messageService.createSystemNotification(0L, user.getId(), "SYSTEM",
                "你的账号状态已于 " + today + " 更新为：" + statusText + "。");
    }

    private void assertAdminOnly() {
        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = this.getById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录或不存在");
        }
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new BusinessException(403, "无权访问，需要管理员权限");
        }
    }
}

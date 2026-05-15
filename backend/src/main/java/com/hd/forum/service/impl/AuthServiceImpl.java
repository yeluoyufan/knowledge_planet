package com.hd.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.dto.LoginRequest;
import com.hd.forum.dto.LoginResult;
import com.hd.forum.dto.RegisterRequest;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.AuthService;
import com.hd.forum.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类。
 *
 * 主要负责：
 * - 注册：创建用户、加密密码、设置默认角色/头像
 * - 登录：校验账号与密码、生成 JWT，并返回给前端
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public void register(RegisterRequest req) {
        // 1. 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.username()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 创建新用户
        User user = new User();
        user.setUsername(req.username());
        user.setNickname(req.nickname());
        // 密码加密存储
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole("USER");
        user.setStatus(0); // 0-正常
        // 默认头像 (可以用一个随机头像API，或者放一张默认图)
        user.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
        user.setBio("这个用户很懒，没有介绍自己...");

        userMapper.insert(user);
    }

    /**
     * 用户登录。
     *
     * 校验步骤：
     * 1. 账号存在性。
     * 2. 账号状态：若 status=1 (禁用) 则禁止登录。
     * 3. 密码匹配：使用 passwordEncoder 校验哈希值。
     * 4. 生成令牌：签发包含用户 ID 和用户名的 JWT。
     */
    public LoginResult login(LoginRequest req) {
        // 1. 查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.username()));

        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }

        // 检查状态
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 2. 校验密码 (明文 vs 密文)
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        // 3. 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        
        // 安全处理：不要把密码哈希返回给前端（即使前端不使用，也属于敏感信息泄露）
        user.setPassword(null);
        
        return new LoginResult(token, user);
    }
}

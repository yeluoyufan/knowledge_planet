package com.hd.forum.service;

import com.hd.forum.dto.LoginRequest;
import com.hd.forum.dto.LoginResult;
import com.hd.forum.dto.RegisterRequest;

/**
 * 认证与授权相关服务接口。
 *
 * 包含：
 * - 注册
 * - 登录（返回 JWT 与用户信息）
 */
public interface AuthService {
    void register(RegisterRequest req);
    LoginResult login(LoginRequest req);
}

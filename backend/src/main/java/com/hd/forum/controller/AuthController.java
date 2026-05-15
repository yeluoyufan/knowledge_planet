package com.hd.forum.controller;

import com.hd.forum.common.Result;
import com.hd.forum.dto.LoginRequest;
import com.hd.forum.dto.LoginResult;
import com.hd.forum.dto.RegisterRequest;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口（登录/注册）。
 *
 * 返回说明：
 * - 登录成功返回 token 与 user 信息，前端保存 token 后后续请求携带 Authorization: Bearer <token>
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 用户注册接口。
     * 接收用户名、密码、昵称等信息，进行合法性校验并入库。
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    /**
     * 用户登录接口。
     * 校验账号密码，成功后签发 JWT 令牌，并返回用户信息。
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        LoginResult loginResult = authService.login(request);
        User user = loginResult.getUser();
        user.setPassword(null); // 安全脱敏
        String token = loginResult.getToken();
        return Result.success(Map.of("token", token,"user", user));
    }
}

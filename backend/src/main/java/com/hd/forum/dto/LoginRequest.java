package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体。
 *
 * - username：用户名
 * - password：明文密码（仅在登录接口中使用；后端会做加密比对）
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {}

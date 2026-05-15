package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求体。
 *
 * - username：登录用户名（唯一）
 * - password：明文密码（后端会加密后入库）
 * - nickname：展示昵称
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "昵称不能为空") String nickname
) {}

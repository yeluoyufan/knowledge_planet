package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/**
 * 修改密码请求体。
 *
 * 校验规则：
 * - oldPassword/newPassword/confirmPassword 必填
 * - newPassword 长度限制（6-20）
 * - newPassword 与 confirmPassword 是否一致通常在 Service 层二次校验
 */
public record PasswordUpdateRequest(
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Length(min = 6, max = 20, message = "密码长度需在6-20位之间")
        String newPassword,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {}

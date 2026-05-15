package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 修改个人资料请求体。
 *
 * 说明：
 * - nickname 必填且限制长度，用于前端展示名称
 * - avatar 为头像 URL（上传接口返回的访问地址）
 * - email 为可选字段，使用 @Email 做格式校验
 */
@Data
public class UserUpdateRequest {
    @NotBlank(message = "昵称不能为空")
    @Length(min = 2, max = 10, message = "昵称长度需在2-10个字符之间")
    private String nickname;

    private String avatar;

    @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
    private String email;

    @Length(max = 255, message = "个人简介长度不能超过255个字符")
    private String bio;
}

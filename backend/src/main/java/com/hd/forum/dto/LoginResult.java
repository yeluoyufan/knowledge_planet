package com.hd.forum.dto;

import com.hd.forum.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回对象。
 *
 * - token：JWT（前端保存并在后续请求中携带）
 * - user：用户信息（用于前端初始化头像、昵称、角色等展示）
 */
@Data
@AllArgsConstructor
public class LoginResult {
    String token;
    User user;
}

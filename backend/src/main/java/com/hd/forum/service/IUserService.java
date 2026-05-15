package com.hd.forum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.dto.PasswordUpdateRequest;
import com.hd.forum.dto.UserUpdateRequest;
import com.hd.forum.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hd.forum.vo.UserVO;

/**
 * 用户业务服务接口（Service 层）。
 *
 * 包含用户资料、权限相关业务：
 * - 查询个人主页信息
 * - 修改个人资料/密码
 * - 后台用户管理：分页列表、禁用/启用
 * - 版主管理：任命/撤销板主（需要管理员权限）
 */
public interface IUserService extends IService<User> {

    /**
     * 获取用户详情（个人主页）。
     */
    UserVO getUserProfile(Long userId);

    /**
     * 修改个人信息（只能修改当前登录用户自身的信息）。
     */
    void updateUserInfo(UserUpdateRequest req);

    /**
     * 分页查询用户列表（管理后台）。
     */
    Page<UserVO> getUserList(Page<User> page, String keyword, Integer managedBoardId, String role);

    /**
     * 公开搜索用户（任何人可见）。
     */
    Page<UserVO> searchUsers(Page<User> page, String keyword);

    /**
     * 修改密码（当前登录用户）。
     */
    void updatePassword(PasswordUpdateRequest req);

    /**
     * 切换用户状态（禁用/启用）。
     */
    void toggleUserStatus(Long userId);
}

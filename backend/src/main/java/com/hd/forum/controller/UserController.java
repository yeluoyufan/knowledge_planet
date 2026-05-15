package com.hd.forum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.common.Result;
import com.hd.forum.common.annotation.RequireSuperAdmin;
import com.hd.forum.dto.PasswordUpdateRequest;
import com.hd.forum.dto.UserUpdateRequest;
import com.hd.forum.service.IPostService;
import com.hd.forum.service.IUserService;
import com.hd.forum.vo.PostVO;
import com.hd.forum.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口。
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;
    private final IPostService postService;

    /**
     * 公开搜索用户。
     */
    @Operation(summary = "公开搜索用户")
    @GetMapping("/search")
    public Result<Page<UserVO>> search(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        Page<UserVO> page = userService.searchUsers(new Page<>(pageNum, pageSize), keyword);
        return Result.success(page);
    }

    /**
     * 分页查询用户列表。
     */
    @Operation(summary = "分页查询用户列表(用于管理或搜索)")
    @GetMapping("/list")
    @RequireSuperAdmin
    public Result<Page<UserVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(required = false, name = "managedBoardId") Integer managedBoardId,
            @RequestParam(required = false, name = "role") String role
    ) {
        Page<UserVO> page = userService.getUserList(new Page<>(pageNum, pageSize), keyword, managedBoardId, role);
        return Result.success(page);
    }

    /**
     * 获取指定用户点赞过的帖子列表。
     */
    @Operation(summary = "获取用户点赞过的帖子")
    @GetMapping("/liked-posts")
    public Result<Page<PostVO>> getLikedPosts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long userId
    ) {
        return Result.success(postService.getLikedPostList(new Page<>(pageNum, pageSize), userId));
    }

    /**
     * 获取指定用户收藏的帖子列表。
     */
    @Operation(summary = "获取用户收藏的帖子")
    @GetMapping("/favorite-posts")
    public Result<Page<PostVO>> getFavoritePosts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long userId
    ) {
        return Result.success(postService.getFavoritePostList(new Page<>(pageNum, pageSize), userId));
    }

    /**
     * 获取指定用户的公开资料。
     */
    @Operation(summary = "获取指定用户信息(个人主页)")
    @GetMapping("/{id:[0-9]+}")
    public Result<UserVO> getUserProfile(@PathVariable Long id) {
        return Result.success(userService.getUserProfile(id));
    }

    /**
     * 修改当前登录用户的个人信息。
     */
    @Operation(summary = "修改个人信息")
    @PutMapping("/update")
    public Result<String> updateInfo(@RequestBody @Valid UserUpdateRequest req) {
        userService.updateUserInfo(req);
        return Result.success("修改成功");
    }

    /**
     * 修改当前登录用户的密码。
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> updatePassword(
            @RequestBody @Valid PasswordUpdateRequest req,
            HttpServletRequest request
    ) {
        userService.updatePassword(req);
        return Result.success("密码修改成功，请重新登录");
    }
}

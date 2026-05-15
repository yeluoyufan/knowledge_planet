package com.hd.forum.aspect;

import com.hd.forum.common.annotation.RequireAdmin;
import com.hd.forum.common.annotation.RequireSuperAdmin;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 管理员权限切面（AOP）。
 *
 * 说明：
 * - 对标注了 @RequireAdmin / @RequireSuperAdmin 的方法进行拦截
 * - 从 SecurityContext 获取当前 userId，然后查询用户角色
 * - 允许 ADMIN 与 MODERATOR 访问（该项目将“后台管理”能力同时开放给版主）
 *
 * 提示：
 * - 这属于“横切关注点”，可避免在每个 Controller 方法里重复写权限判断
 * - 如果已全面使用 @PreAuthorize，也可以逐步减少对 AOP 的依赖
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminAspect {
    private final UserMapper userMapper;

    @Before("@annotation(requireAdmin)")
    public void checkAdminPermission(JoinPoint joinPoint, RequireAdmin requireAdmin) {
        User user = getCurrentUser();
        String role = user.getRole();
        if (!"ADMIN".equals(role) && !"MODERATOR".equals(role)) {
            log.warn("用户 {} 试图访问管理员/版主接口被拦截", user.getId());
            throw new BusinessException(403, "无权访问，需要管理员/版主权限");
        }
    }

    @Before("@annotation(requireSuperAdmin)")
    public void checkSuperAdminPermission(JoinPoint joinPoint, RequireSuperAdmin requireSuperAdmin) {
        User user = getCurrentUser();
        if (!"ADMIN".equals(user.getRole())) {
            log.warn("用户 {} 试图访问仅管理员接口被拦截", user.getId());
            throw new BusinessException(403, "无权访问，需要管理员权限");
        }
    }

    private User getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户未登录或不存在");
        }
        return user;
    }
}

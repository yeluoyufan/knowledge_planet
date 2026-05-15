package com.hd.forum.utils;

import com.hd.forum.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext 相关工具类。
 *
 * 本项目采用 JWT + Spring Security：
 * - JwtAuthenticationFilter 会解析 Token，并把 userId 放到 Authentication.principal 中
 * - 业务代码需要当前用户身份时，统一通过此类读取
 */
public class SecurityUtils {
    /**
     * 获取当前登录用户 ID。
     * 如果未登录或上下文异常，抛出 BusinessException(401)。
     */
    public static Long getUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                throw new BusinessException(401, "用户未登录");
            }
            Object principal = authentication.getPrincipal();
            return Long.valueOf(principal.toString());
        } catch (Exception e) {
            throw new BusinessException(401, "获取登录用户信息失败");
        }
    }
}

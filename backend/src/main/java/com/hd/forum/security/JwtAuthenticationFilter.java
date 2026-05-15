package com.hd.forum.security;

import com.hd.forum.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 鉴权过滤器（每个请求执行一次）。
 *
 * 工作流程：
 * 1) 从请求头 Authorization 读取 Bearer Token
 * 2) 使用 JwtUtils 解析 Token，拿到 userId/username 等信息
 * 3) 构造 Authentication 写入 SecurityContext，使后续业务代码可通过 SecurityUtils 获取当前用户
 *
 * 说明：
 * - 当前实现给了默认 ROLE_USER 权限；如果要区分 ADMIN/MODERATOR，需要把 role 写入 token 或从数据库加载
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. 获取请求头中的 Token
        String header = request.getHeader("Authorization");

        // 2. 判断格式是否正确 (Bearer xxxx)
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response); // 没有 Token，直接放行(让 Security 后面的过滤器去拦截)
            return;
        }

        // 3. 解析 Token
        String token = header.substring(7); // 去掉 "Bearer "
        Claims claims = jwtUtils.parseToken(token);

        // 4. 验证成功，将用户信息放入 SecurityContext
        if (claims != null) {
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();

            // 这里为了 MVP 简单，我们暂时只给一个默认 USER 权限
            // 如果后续需要管理员权限，可以从 claims 里取 role
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, // Principal (这里我们直接存 userId，方便后续获取)
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // Authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. 继续过滤器链
        chain.doFilter(request, response);
    }
}

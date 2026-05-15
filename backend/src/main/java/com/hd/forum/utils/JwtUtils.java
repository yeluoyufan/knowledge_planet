package com.hd.forum.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类：生成与解析 Token。
 *
 * Token 内容：
 * - subject：username
 * - userId：自定义 claim，用于快速获取当前登录用户 ID
 *
 * 注意：
 * - SECRET 在生产环境应放到配置文件/环境变量中，不建议硬编码在代码里
 * - parseToken 解析失败返回 null，调用方需要做空值判断
 */
@Component
public class JwtUtils {
    private final Key key;
    private final long expiration;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration:604800000}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }

    /**
     * 根据用户 ID 和用户名生成 JWT 令牌。
     */
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId) // 把 userId 存进 token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 JWT 令牌并获取负载信息（Claims）。
     * 若解析失败（如签名错误、过期等），则返回 null。
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null; // 解析失败或过期
        }
    }
}

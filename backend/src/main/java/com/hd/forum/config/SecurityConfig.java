package com.hd.forum.config;

import com.hd.forum.common.Result;
import com.hd.forum.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.PrintWriter;

/**
 * Spring Security 配置入口。
 *
 * 本项目使用 JWT 做无状态登录：
 * - 关闭 Session（STATELESS）
 * - 通过 JwtAuthenticationFilter 解析请求头 Token 并注入 SecurityContext
 * - 对未登录与无权限场景，统一返回 JSON 格式错误信息
 *
 * 说明：
 * - 公开接口在此处通过 permitAll 放行（如登录/注册、公共板块列表、静态资源等）
 * - 需要角色控制的接口建议使用方法级注解 @PreAuthorize（已开启 @EnableMethodSecurity）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 开启方法级安全校验
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            PrintWriter writer = response.getWriter();
                            writer.write(new ObjectMapper().writeValueAsString(Result.error(401, "请先登录")));
                            writer.flush();
                            writer.close();
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            PrintWriter writer = response.getWriter();
                            writer.write(new ObjectMapper().writeValueAsString(Result.error(403, "权限不足")));
                            writer.flush();
                            writer.close();
                        })
                );

        return http.build();
    }

    // 跨域配置保持不变
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration source = new CorsConfiguration();
        source.addAllowedOriginPattern("*");
        source.addAllowedHeader("*");
        source.addAllowedMethod("*");
        source.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource urlSource = new UrlBasedCorsConfigurationSource();
        urlSource.registerCorsConfiguration("/**", source);
        return urlSource;
    }
}

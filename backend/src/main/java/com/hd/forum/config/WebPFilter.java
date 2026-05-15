package com.hd.forum.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * WebP 自适应过滤器。
 *
 * 当浏览器 Accept 头包含 image/webp 时：
 * - 如果请求的是 jpg/jpeg/png，且同名 webp 文件存在，则转发到 webp
 *
 * 目的：
 * - 在不改动前端代码的前提下，优先返回体积更小的 WebP 图片
 */
@Component
public class WebPFilter implements Filter {

    @Value("${upload.path:D:/Code/Java/forum/uploads}")
    private String uploadPath;

    @Value("${upload.base-url:/uploads}")
    private String baseUrl;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String accept = httpRequest.getHeader("Accept");

        if (uri.startsWith(baseUrl) && accept != null && accept.contains("image/webp")) {
            String ext = uri.substring(uri.lastIndexOf(".")).toLowerCase();
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")) {
                String webpUri = uri.substring(0, uri.lastIndexOf(".")) + ".webp";
                String relativePath = webpUri.substring(baseUrl.length());
                File webpFile = new File(uploadPath, relativePath);
                
                if (webpFile.exists()) {
                    // 转发到 WebP 资源
                    request.getRequestDispatcher(webpUri).forward(request, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}

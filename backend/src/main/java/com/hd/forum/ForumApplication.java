package com.hd.forum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 启动入口。
 *
 * - @SpringBootApplication：开启组件扫描与自动配置
 * - @EnableScheduling：开启定时任务（例如文件清理任务）
 */
@EnableScheduling
@SpringBootApplication
public class ForumApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForumApplication.class, args);
    }
}

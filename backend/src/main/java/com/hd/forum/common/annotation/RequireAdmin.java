package com.hd.forum.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理员权限标记注解。
 *
 * 说明：
 * - 用于标记“只能管理员访问”的 Controller 方法
 * - 由 AdminAspect 在运行时拦截并做权限校验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}

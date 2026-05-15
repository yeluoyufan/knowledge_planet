package com.hd.forum.common.exception;

import com.hd.forum.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（RestControllerAdvice）。
 *
 * 目标：
 * - 将后端异常统一转换为 Result 格式返回，避免前端出现“非预期 HTML/堆栈信息”
 * - 对不同类型异常给出更友好的 msg（例如参数校验失败）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandle {
    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public Result<?> handleAccessDenied(Exception e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(403, "权限不足");
    }

    // 处理数据校验异常 (Spring Validation)
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("数据校验失败: {}", message);
        return Result.error(400, message);
    }

    // 处理系统兜底异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}

package com.hd.forum.common.exception;

import lombok.Getter;

/**
 * 业务异常（可控异常）。
 *
 * 用途：
 * - 当出现“用户不存在”“无权限”“参数不合法”等可预期的业务错误时抛出
 * - 由 GlobalExceptionHandle 捕获并转换为统一的 Result.error(code, msg)
 */
@Getter
public class BusinessException extends RuntimeException{
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}

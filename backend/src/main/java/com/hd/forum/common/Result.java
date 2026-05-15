package com.hd.forum.common;

import lombok.Data;

/**
 * 统一响应对象。
 *
 * 返回格式约定：
 * - code：业务状态码（本项目常用 200 表示成功；400/401/403/500 表示失败）
 * - msg：提示信息
 * - data：返回数据（可为空）
 *
 * 说明：Controller 层通常返回 Result<T>，前端只需要判断 code 即可。
 */
@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    /**
     * 成功响应（携带 data）。
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    /**
     * 成功响应（不需要返回 data）。
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败响应（自定义 code 与 msg）。
     */
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    /**
     * 失败响应（默认 500）。
     */
    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }
}

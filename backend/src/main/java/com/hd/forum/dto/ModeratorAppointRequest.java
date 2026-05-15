package com.hd.forum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任命/撤销板主请求体（管理员后台使用）。
 *
 * - userId：被任命/撤销的用户 ID（必填）
 * - boardId：板块 ID（可选；为空表示撤销板主，将用户降为普通用户）
 */
@Data
public class ModeratorAppointRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private Integer boardId;
}

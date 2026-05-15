package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建/修改板块的请求体。
 *
 * - name：板块名称（必填）
 * - description：板块描述（可选）
 * - sort：排序权重（可选；为空时由后端自动递增）
 */
public record BoardRequest(
        @NotBlank(message = "板块名称不能为空") String name,
        String description,
        Integer sort,
        Long moderatorId
) {}

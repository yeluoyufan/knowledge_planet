package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 发帖请求体。
 *
 * - title：标题（必填）
 * - content：正文 Markdown 源码（必填）
 * - boardId：板块 ID（必填）
 * - tagIds：已存在的标签 ID 列表（可选）
 * - customTags：用户自定义的标签名（可选；后端会做去重并自动创建）
 */
public record PostCreateRequest(
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "内容不能为空") String content,
        @NotNull(message = "必须选择板块") Integer boardId,
        List<Integer> tagIds,
        List<String> customTags
) {}

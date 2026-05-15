package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发表评论请求体。
 *
 * - postId：所属帖子 ID（必填）
 * - content：评论内容（必填）
 * - parentId：回复的评论 ID（可选；为空/0 表示一级评论）
 */
public record CommentCreateRequest(@NotNull(message = "必须指定帖子ID") Long postId,
                                   @NotBlank(message = "评论内容不能为空") String content,
                                   Long parentId
) {}

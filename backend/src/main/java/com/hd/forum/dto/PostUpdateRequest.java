package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑帖子请求体。
 *
 * 说明：
 * - id 为要编辑的帖子 ID（必填）
 * - title/content 为新的内容（必填）
 * - 是否允许编辑、编辑后是否重新进入审核，由 Service 层业务规则决定
 */
@Data
public class PostUpdateRequest {
    @NotNull(message = "帖子ID不能为空")
    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private java.util.List<Integer> tagIds;

    private java.util.List<String> customTags;
}

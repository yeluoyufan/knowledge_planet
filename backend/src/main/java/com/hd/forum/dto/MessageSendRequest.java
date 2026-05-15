package com.hd.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 发送私信请求体。
 *
 * - toUserId：接收者用户 ID
 * - content：消息内容
 */
public record MessageSendRequest(
        @NotNull(message = "接收者ID不能为空")
        @Positive(message = "接收者ID不合法")
        Long toUserId,
        @NotBlank(message = "内容不能为空")
        @Size(max = 255, message = "消息内容不能超过255个字符")
        String content
) {}

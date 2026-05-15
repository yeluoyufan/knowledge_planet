package com.hd.forum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项（私信左侧会话栏）。
 *
 * 用于展示：
 * - 对方用户信息
 * - 最新一条消息摘要
 * - 未读数量
 */
@Data
public class ConversationVO {
    private Long userId;
    private String nickname;
    private String avatar;

    private String latestMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestTime;

    private Integer unreadCount;
}

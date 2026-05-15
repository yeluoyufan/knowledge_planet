package com.hd.forum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息视图对象（返回给前端的消息数据结构）。
 *
 * 用途：
 * - 消息中心列表展示（私信、评论提醒、系统通知）
 * - 结合 type 字段做分类展示
 */
@Data
public class MessageVO {
    private Long id;
    private Long fromId;
    private String fromNickname;
    private String fromAvatar;
    private String content;
    private String type;
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

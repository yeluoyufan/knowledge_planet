package com.hd.forum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论视图对象（返回给前端的评论数据结构）。
 *
 * 说明：
 * - content 为展示文本（可能已经带上“回复 @xxx:”前缀，由后端组装）
 * - childCount 用于前端决定是否展示“查看回复”按钮
 */
@Data
public class CommentVO {
    private Long id;
    private Long postId;

    private String content;

    private Long userId;
    private String authorName;
    private String authorAvatar;

    private Long parentId;

    private Long replyToUserId;
    private String replyToUserNickname;

    private Long childCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

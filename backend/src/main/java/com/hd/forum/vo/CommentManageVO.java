package com.hd.forum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentManageVO {
    private Long id;
    private Long postId;
    private String postTitle;
    private Integer boardId;
    private String content;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String parentContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}


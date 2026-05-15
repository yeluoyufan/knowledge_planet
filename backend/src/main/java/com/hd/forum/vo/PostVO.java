package com.hd.forum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.hd.forum.entity.TagSimple;

/**
 * 帖子视图对象（返回给前端的帖子数据结构）。
 *
 * VO（View Object）与实体（Entity）的区别：
 * - Entity：对应数据库表结构，可能包含敏感或不适合直接暴露给前端的字段
 * - VO：根据页面展示需要进行裁剪/聚合（例如 authorName、boardName、tags）
 */
@Data
public class PostVO {
    private Long id;
    private String title;
    private String content;
    private Integer boardId;
    private String boardName;
    private List<TagSimple> tags;

    // 作者信息
    private Long userId;
    private String authorName;
    private String authorAvatar;

    // 统计信息
    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer status;
    private Boolean isTop;
    private String rejectReason;

    // 当前登录用户状态 (由 Service 动态填充)
    private Boolean hasLiked;
    private Boolean hasFavorited;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

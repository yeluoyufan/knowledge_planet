package com.hd.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 帖子实体（对应数据库表 forum_post）。
 *
 * 说明：
 * - content 存储 Markdown 源码，前端负责渲染
 * - status 表示审核状态：0 待审核、1 已发布、2 已拒绝
 * - tags 字段以 JSON 形式存储标签快照，合并了原有的中间表，提升查询效率
 */
@Getter
@Setter
@TableName(value = "forum_post", autoResultMap = true)
@Schema(name = "Post", description = "帖子表")
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "帖子ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "板块ID")
    @TableField("board_id")
    private Integer boardId;

    @Schema(description = "作者ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "标题")
    @TableField("title")
    private String title;

    @Schema(description = "内容(Markdown源码)")
    @TableField("content")
    private String content;

    @Schema(description = "浏览量")
    @TableField("view_count")
    private Integer viewCount;

    @Schema(description = "回复数")
    @TableField("reply_count")
    private Integer replyCount;

    @Schema(description = "点赞数")
    @TableField("like_count")
    private Integer likeCount;

    @Schema(description = "是否置顶")
    @TableField("is_top")
    private Boolean isTop;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @Schema(description = "状态: 0-待审核, 1-已发布, 2-已拒绝")
    @TableField("status")
    private Integer status;

    @Schema(description = "标签列表(JSON存储)")
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    private List<TagSimple> tags;

    @Schema(description = "拒绝理由")
    @TableField("reject_reason")
    private String rejectReason;

    @TableField("deleted")
    private Boolean deleted;
}

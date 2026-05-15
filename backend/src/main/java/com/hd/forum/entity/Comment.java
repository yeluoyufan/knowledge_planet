package com.hd.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 评论实体（对应数据库表 forum_comment）。
 *
 * 楼中楼结构说明：
 * - rootId = 0：表示一级评论（直接挂在帖子下）
 * - rootId != 0：表示子评论（属于某条一级评论的楼中楼）
 * - parentId：表示直接回复的那条评论
 */
@Getter
@Setter
@TableName("forum_comment")
@Schema(name = "Comment", description = "评论表")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属帖子")
    @TableField("post_id")
    private Long postId;

    @Schema(description = "根评论ID (0代表是一级评论)")
    private Long rootId;

    @Schema(description = "评论人")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "评论内容")
    @TableField("content")
    private String content;

    @Schema(description = "父评论ID(用于记录回复谁)")
    @TableField("parent_id")
    private Long parentId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("deleted")
    private Boolean deleted;
}

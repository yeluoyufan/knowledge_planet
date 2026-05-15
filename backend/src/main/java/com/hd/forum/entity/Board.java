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
 * 板块实体（对应数据库表 forum_board）。
 *
 * 板块用于对帖子进行分类管理：
 * - name：板块名称（如 Java、AI 等）
 * - description：板块说明
 * - sort：前台展示排序字段（越小越靠前）
 */
@Getter
@Setter
@TableName("forum_board")
@Schema(name = "Board", description = "板块表")
public class Board implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "板块ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "板块名称")
    @TableField("name")
    private String name;

    @Schema(description = "板块描述")
    @TableField("description")
    private String description;

    @Schema(description = "排序优先级")
    @TableField("sort")
    private Integer sort;

    @TableField("create_time")
    private LocalDateTime createTime;
}

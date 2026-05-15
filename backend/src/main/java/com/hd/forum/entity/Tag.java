package com.hd.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签实体（对应 forum_tag 表）。
 *
 * 标签用于对帖子进行主题归类：
 * - 支持预置标签（管理员创建）
 * - 支持用户发帖时提交自定义标签（后端会自动创建并做去重）
 */
@Getter
@Setter
@TableName("forum_tag")
@Schema(name = "Tag", description = "标签表")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("name")
    private String name;

    @TableField("create_time")
    private LocalDateTime createTime;
}

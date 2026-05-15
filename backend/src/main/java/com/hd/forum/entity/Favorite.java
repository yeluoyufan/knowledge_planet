package com.hd.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏关系实体（对应 forum_favorite 表）。
 *
 * userId -> postId：
 * - userId：收藏者
 * - postId：被收藏的帖子
 */
@Getter
@Setter
@TableName("forum_favorite")
public class Favorite implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("post_id")
    private Long postId;

    @TableField("create_time")
    private LocalDateTime createTime;
}

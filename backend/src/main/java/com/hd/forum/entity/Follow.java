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
 * 关注关系实体（对应 forum_follow 表）。
 *
 * followerId -> followeeId：
 * - followerId：发起关注的人
 * - followeeId：被关注的人
 */
@Getter
@Setter
@TableName("forum_follow")
public class Follow implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("follower_id")
    private Long followerId;

    @TableField("followee_id")
    private Long followeeId;

    @TableField("create_time")
    private LocalDateTime createTime;
}

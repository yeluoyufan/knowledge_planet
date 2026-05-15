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
 * 站内消息实体（对应数据库表 sys_message）。
 *
 * type 类型约定（见业务实现）：
 * - CHAT：私信
 * - COMMENT：评论/回复提醒
 * - SYSTEM：系统通知
 *
 * fromId = 0 表示系统发送。
 */
@Getter
@Setter
@TableName("sys_message")
@Schema(name = "Message", description = "消息通知表")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "发送者ID(0代表系统)")
    @TableField("from_id")
    private Long fromId;

    @Schema(description = "接收者ID")
    @TableField("to_id")
    private Long toId;

    @Schema(description = "类型: COMMENT/SYSTEM")
    @TableField("type")
    private String type;

    @Schema(description = "消息内容或关联ID")
    @TableField("content")
    private String content;

    @Schema(description = "是否已读")
    @TableField("is_read")
    private Boolean isRead;

    @TableField("create_time")
    private LocalDateTime createTime;
}

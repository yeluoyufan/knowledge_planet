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
 * 用户实体（对应数据库表 sys_user）。
 *
 * 说明：
 * - password 存储加密后的密码（BCrypt 等），绝不能返回给前端
 * - role 用于权限控制：ADMIN / MODERATOR / USER
 * - managedBoardId 表示版主所管理的板块（仅对 MODERATOR 有意义）
 * - fansCount / followCount 为冗余统计字段，用于个人主页快速展示
 */
@Getter
@Setter
@TableName("sys_user")
@Schema(name = "User", description = "用户表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "密码(加密)")
    @TableField("password")
    private String password;

    @Schema(description = "昵称")
    @TableField("nickname")
    private String nickname;

    @Schema(description = "头像URL")
    @TableField("avatar")
    private String avatar;

    @Schema(description = "邮箱")
    @TableField("email")
    private String email;

    @Schema(description = "个人简介")
    @TableField("bio")
    private String bio;

    @Schema(description = "角色: USER/ADMIN/MODERATOR")
    @TableField("role")
    private String role;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除: 0正常, 1删除")
    @TableField("deleted")
    private Boolean deleted;

    @Schema(description = "状态: 0正常, 1禁用")
    @TableField("status")
    private Integer status;

    @Schema(description = "管理的板块ID")
    @TableField("board_id")
    private Integer managedBoardId;

    @Schema(description = "关注数")
    @TableField("follow_count")
    private Integer followCount;

    @Schema(description = "粉丝数")
    @TableField("fans_count")
    private Integer fansCount;
}

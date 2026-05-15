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
 * 上传文件元信息实体（对应 sys_file 表）。
 *
 * 设计目的：
 * - 记录文件名、原始文件名、类型、大小、上传者
 * - refCount 用于记录“被引用次数”（例如被多少篇帖子引用）
 * - 配合定时任务清理无引用文件
 */
@Getter
@Setter
@TableName("sys_file")
public class SysFile implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("filename")
    private String filename;

    @TableField("original_name")
    private String originalName;

    @TableField("type")
    private String type;

    @TableField("size")
    private Long size;

    @TableField("user_id")
    private Long userId;

    @TableField("ref_count")
    private Integer refCount;

    @TableField("create_time")
    private LocalDateTime createTime;
}

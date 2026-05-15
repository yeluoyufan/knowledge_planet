package com.hd.forum.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 标签快照类。
 * 用于在帖子表中以文本形式存储标签的基本信息，避免序列化复杂日期类型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagSimple implements Serializable {
    private Integer id;
    private String name;
}

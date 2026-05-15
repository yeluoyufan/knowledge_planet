package com.hd.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.forum.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签表（forum_tag）数据访问层（Mapper）。
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}

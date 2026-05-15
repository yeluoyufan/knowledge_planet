package com.hd.forum.mapper;

import com.hd.forum.entity.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子表数据访问层（Mapper）。
 *
 * 说明：
 * - 继承 BaseMapper，提供通用 CRUD
 * - 帖子列表、搜索等复杂条件通常通过 Service 层 wrapper 组合实现
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

}

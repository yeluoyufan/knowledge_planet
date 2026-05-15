package com.hd.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.forum.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞关系表（forum_like）数据访问层（Mapper）。
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
}

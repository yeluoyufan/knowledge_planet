package com.hd.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.forum.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注关系表（forum_follow）数据访问层（Mapper）。
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}

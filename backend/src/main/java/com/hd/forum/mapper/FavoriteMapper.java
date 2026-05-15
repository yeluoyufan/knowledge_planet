package com.hd.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.forum.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏关系表（forum_favorite）数据访问层（Mapper）。
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}

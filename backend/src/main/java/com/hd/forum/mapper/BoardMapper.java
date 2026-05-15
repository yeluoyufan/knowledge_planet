package com.hd.forum.mapper;

import com.hd.forum.entity.Board;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 板块表数据访问层（Mapper）。
 *
 * 说明：
 * - 继承 BaseMapper，提供通用 CRUD
 * - 板块列表排序通常按 sort 字段在 Service 层实现
 */
@Mapper
public interface BoardMapper extends BaseMapper<Board> {

}

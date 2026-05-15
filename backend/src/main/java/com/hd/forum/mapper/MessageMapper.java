package com.hd.forum.mapper;

import com.hd.forum.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息表数据访问层（Mapper）。
 *
 * 说明：
 * - 继承 BaseMapper，提供通用 CRUD
 * - 会话列表、未读统计等聚合逻辑通常在 Service 层实现
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

}

package com.hd.forum.mapper;

import com.hd.forum.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表数据访问层（Mapper）。
 *
 * 说明：
 * - 继承 BaseMapper，提供通用 CRUD
 * - 用户筛选、角色查询等条件通常由 Service 层 wrapper 拼装
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}

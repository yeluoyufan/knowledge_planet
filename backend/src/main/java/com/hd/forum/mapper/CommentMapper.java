package com.hd.forum.mapper;

import com.hd.forum.entity.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论表数据访问层（Mapper）。
 *
 * 说明：
 * - 继承 MyBatis-Plus 的 BaseMapper，可直接使用通用 CRUD 方法
 * - 复杂查询通常在 Service 层通过 LambdaQueryWrapper 组合完成
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

}

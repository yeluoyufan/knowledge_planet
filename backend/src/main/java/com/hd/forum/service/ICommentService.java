package com.hd.forum.service;

import com.hd.forum.dto.CommentCreateRequest;
import com.hd.forum.entity.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hd.forum.vo.CommentVO;

import java.util.List;

/**
 * 评论业务服务接口。
 *
 * 支持评论树结构：
 * - 一级评论：直接挂在帖子下
 * - 子评论：楼中楼回复，通过 rootId/parentId 组织层级
 */
public interface ICommentService extends IService<Comment> {

    /**
     * 发表评论（一级评论或回复）。
     */
    void createComment(CommentCreateRequest req);

    /**
     * 删除评论（通常仅允许作者删除；后台可由管理员/版主删除）。
     */
    void deleteComment(Long commentId);

    /**
     * 获取指定帖子的一级评论列表（通常会包含子评论的统计信息）。
     */
    List<CommentVO> getCommentsByPostId(Long postId);

    /**
     * 获取某条一级评论下的子评论列表（楼中楼）。
     */
    List<CommentVO> getSubComments(Long rootId);
}

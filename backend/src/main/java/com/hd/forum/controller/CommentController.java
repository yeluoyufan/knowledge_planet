package com.hd.forum.controller;

import com.hd.forum.common.Result;
import com.hd.forum.dto.CommentCreateRequest;
import com.hd.forum.service.ICommentService;
import com.hd.forum.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论相关接口。
 *
 * 支持：
 * - 发表评论（一级评论/楼中楼回复由 Service 层根据 parent/root 字段决定）
 * - 删除评论（只能删除自己的评论，或由管理员/版主在后台删除）
 * - 查询某帖子的评论树
 */
@Tag(name = "评论管理")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final ICommentService commentService;

    /**
     * 发表评论接口。
     * 支持一级评论（直接评论帖子）和二级评论（楼中楼回复）。
     */
    @Operation(summary = "发表评论")
    @PostMapping("/create")
    public Result<String> create(@RequestBody @Valid CommentCreateRequest request) {
        commentService.createComment(request);
        return Result.success("评论成功");
    }

    /**
     * 删除评论接口。
     * 只有评论作者本人可以调用此接口删除。
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功");
    }

    /**
     * 获取指定帖子的一级评论列表。
     * 一级评论是指直接回复帖子的根评论。
     */
    @Operation(summary = "获取帖子的一级评论")
    @GetMapping("/list/{postId}")
    public Result<List<CommentVO>> list(@PathVariable Long postId) {
        List<CommentVO> list = commentService.getCommentsByPostId(postId);
        return Result.success(list);
    }

    /**
     * 获取指定根评论下的所有子评论（楼中楼）。
     * 用于展开查看详细的对话记录。
     */
    @Operation(summary = "获取子评论(楼中楼)")
    @GetMapping("/sub-list/{rootId}")
    public Result<List<CommentVO>> subList(@PathVariable Long rootId) {
        List<CommentVO> list = commentService.getSubComments(rootId);
        return Result.success(list);
    }
}

package com.hd.forum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.common.Result;
import com.hd.forum.common.annotation.RequireAdmin;
import com.hd.forum.common.annotation.RequireSuperAdmin;
import com.hd.forum.dto.PostCreateRequest;
import com.hd.forum.dto.PostUpdateRequest;
import com.hd.forum.service.IPostService;
import com.hd.forum.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子相关接口。
 *
 * 主要职责：
 * - 提供帖子发布、编辑、查询等面向前端的 HTTP API
 * - 对“审核/置顶/后台删除”等敏感操作做权限保护
 *
 * 说明：
 * - 具体业务规则（例如：只能删除自己的帖子、审核状态流转）放在 Service 层实现
 * - Controller 只负责参数接收、权限声明与统一返回格式
 */
@Tag(name = "帖子管理")
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final IPostService postService;

    /**
     * 发布新帖子。
     * 接收标题、内容、板块、标签等信息，新帖子默认为待审核状态。
     */
    @Operation(summary = "发布帖子")
    @PostMapping("/create")
    public Result<String> create(@RequestBody @Valid PostCreateRequest request) {
        postService.createPost(request);
        return Result.success("发布成功");
    }

    /**
     * 获取帖子列表。
     * 支持分页、板块过滤、用户过滤、关键词搜索及多种排序规则。
     */
    @Operation(summary = "获取帖子列表")
    @GetMapping("/list")
    public Result<Page<PostVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer boardId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder
    ) {
        Page<PostVO> result = postService.getPostList(new Page<>(pageNum, pageSize), boardId, userId, keyword, status, sortField, sortOrder);
        return Result.success(result);
    }

    /**
     * 获取帖子详细信息。
     * 返回帖子正文、作者信息、板块信息、标签列表及当前用户的互动状态（是否点赞/收藏）。
     */
    @Operation(summary = "获取帖子详情")
    @GetMapping("/{id:\\d+}")
    public Result<PostVO> detail(@PathVariable Long id,
                                 @RequestParam(defaultValue = "true") boolean increaseView) {
        return Result.success(postService.getPostDetail(id, increaseView));
    }

    /**
     * 用户删除自己的帖子。
     */
    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id:\\d+}")
    public Result<String> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success("删除成功");
    }

    /**
     * 作者修改已发布的帖子内容。
     */
    @Operation(summary = "编辑帖子")
    @PutMapping("/update")
    public Result<String> update(@RequestBody @Valid PostUpdateRequest req) {
        postService.updatePost(req);
        return Result.success("修改成功");
    }

    /**
     * 管理员/版主审核帖子。
     * 可通过或拒绝帖子发布申请。
     */
    @Operation(summary = "审核帖子")
    @PostMapping("/audit")
    @RequireAdmin
    public Result<Void> auditPost(@RequestParam Long postId, @RequestParam Integer status, @RequestParam(required = false) String rejectReason) {
        postService.auditPost(postId, status, rejectReason);
        return Result.success();
    }

    /**
     * 管理员在后台强制删除违规帖子。
     */
    @Operation(summary = "管理员删除帖子")
    @DeleteMapping("/admin/{id}")
    @RequireAdmin
    public Result<Void> deletePostByAdmin(@PathVariable Long id) {
        postService.deletePostByAdmin(id);
        return Result.success();
    }

    /**
     * 切换帖子的置顶状态。
     */
    @Operation(summary = "置顶/取消置顶")
    @PostMapping("/top/{id}")
    @RequireAdmin
    public Result<Void> toggleTop(@PathVariable Long id) {
        postService.toggleTop(id);
        return Result.success();
    }

    /**
     * 获取指定板块下的热门帖子。
     */
    @Operation(summary = "获取热门帖子")
    @GetMapping("/hot")
    public Result<List<PostVO>> getHotPosts(@RequestParam(required = false) Integer boardId, @RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(postService.getHotPosts(boardId, limit));
    }
}

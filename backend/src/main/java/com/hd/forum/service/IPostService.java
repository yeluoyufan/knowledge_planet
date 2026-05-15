package com.hd.forum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.forum.dto.PostCreateRequest;
import com.hd.forum.dto.PostUpdateRequest;
import com.hd.forum.entity.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hd.forum.vo.PostVO;

import java.util.List;

/**
 * 帖子业务服务接口（Service 层）。
 *
 * Service 层职责：
 * - 承载业务规则与权限校验（例如：只能删除自己的帖子；管理员/版主可以审核）
 * - 负责事务边界（需要事务的方法在实现类上使用 @Transactional）
 * - 组装返回给前端的 VO（例如 PostVO）
 */
public interface IPostService extends IService<Post> {

    /**
     * 发布帖子。
     * 通常在这里设置默认状态（如待审核）、处理标签、统计字段初始化等。
     */
    void createPost(PostCreateRequest req);

    /**
     * 分页查询帖子列表。
     * 支持按板块、作者、关键字、状态以及排序字段进行筛选/排序。
     */
    Page<PostVO> getPostList(Page<Post> page, Integer boardId,Long userId,String keyword, Integer status, String sortField, String sortOrder);

    /**
     * 获取帖子详情。
     * 可在实现中做浏览量累加、权限判断等。
     */
    PostVO getPostDetail(Long id, boolean increaseView);

    /**
     * 删除帖子（作者删除）。
     * 需要校验当前登录用户是否为作者。
     */
    void deletePost(Long postId);

    /**
     * 编辑帖子（作者编辑）。
     * 需要校验当前登录用户是否为作者。
     */
    void updatePost(PostUpdateRequest req);

    /**
     * 管理员/版主：切换置顶状态。
     */
    void toggleTop(Long postId);

    /**
     * 管理员/版主：后台删除帖子（不校验作者）。
     */
    void deletePostByAdmin(Long postId);

    /**
     * 管理员/版主：审核帖子。
     * status 通常约定：0 待审核、1 已发布、2 已拒绝。
     */
    void auditPost(Long postId, Integer status, String rejectReason);

    /**
     * 获取热门帖子（不受分页影响）。
     * 通常按浏览量/点赞/评论等热度指标排序。
     */
    List<PostVO> getHotPosts(Integer boardId, Integer limit);

    /**
     * 获取用户点赞过的帖子列表（分页）。
     */
    Page<PostVO> getLikedPostList(Page<Post> page, Long userId);

    /**
     * 获取用户收藏过的帖子列表（分页）。
     */
    Page<PostVO> getFavoritePostList(Page<Post> page, Long userId);
}

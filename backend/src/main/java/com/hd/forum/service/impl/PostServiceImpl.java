package com.hd.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.forum.common.exception.BusinessException;
import com.hd.forum.dto.PostCreateRequest;
import com.hd.forum.dto.PostUpdateRequest;
import com.hd.forum.entity.Board;
import com.hd.forum.entity.Comment;
import com.hd.forum.entity.Favorite;
import com.hd.forum.entity.Post;
import com.hd.forum.entity.PostLike;
import com.hd.forum.entity.SysFile;
import com.hd.forum.entity.Tag;
import com.hd.forum.entity.TagSimple;
import com.hd.forum.entity.User;
import com.hd.forum.mapper.BoardMapper;
import com.hd.forum.mapper.CommentMapper;
import com.hd.forum.mapper.FavoriteMapper;
import com.hd.forum.mapper.PostLikeMapper;
import com.hd.forum.mapper.PostMapper;
import com.hd.forum.mapper.SysFileMapper;
import com.hd.forum.mapper.TagMapper;
import com.hd.forum.mapper.UserMapper;
import com.hd.forum.service.IPostService;
import com.hd.forum.service.IMessageService;
import com.hd.forum.utils.SecurityUtils;
import com.hd.forum.vo.PostVO;
import com.hd.forum.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子业务服务实现类。
 *
 * 本类是帖子模块的核心业务实现，主要包含：
 * - 发帖/改帖/删帖（含作者权限校验）
 * - 帖子列表查询与排序（含置顶逻辑）
 * - 组装 PostVO（作者信息、板块信息、标签信息、评论数等）
 * - 点赞/收藏/评论相关的统计与通知触发
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    private final UserMapper userMapper;
    private final BoardMapper boardMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final SysFileMapper sysFileMapper;
    private final PostLikeMapper postLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final IMessageService messageService;

    // 1. 发布帖子
    @Transactional
    public void createPost(PostCreateRequest req) {
        // 校验板块是否存在
        Board board = boardMapper.selectById(req.boardId());
        if (board == null) {
            throw new BusinessException("板块不存在");
        }

        // 构建帖子对象
        Post post = new Post();
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setBoardId(req.boardId());
        post.setUserId(SecurityUtils.getUserId()); // 关键：从 Token 获取当前用户ID
        post.setViewCount(0);
        post.setReplyCount(0);
        post.setIsTop(false); // 默认不置顶
        post.setStatus(0); // 默认待审核

        // 处理已选标签 + 自定义标签
        List<TagSimple> finalTags = resolveTags(req.tagIds(), req.customTags());
        post.setTags(finalTags);

        this.save(post);

        // 通知本板块版主：有新帖子待审核
        User moderator = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "MODERATOR")
                .eq(User::getManagedBoardId, req.boardId())
                .last("LIMIT 1"));
        if (moderator != null) {
            String today = java.time.LocalDate.now().toString();
            messageService.createSystemNotification(0L, moderator.getId(), "SYSTEM",
                    "【待审核】" + today + " 板块有新帖子待审核：《" + req.title() + "》");
        }

        // 更新图片引用计数
        updateImageRefCount(null, post.getContent());
    }

    /**
     * 获取帖子列表（带分页与多种筛选/排序方式）。
     *
     * 关键逻辑：
     * 1. 权限隔离：
     *    - 已发布的帖子 (status=1) 公开可见。
     *    - 待审核 (status=0) 或 被拒绝 (status=2) 仅作者本人、管理员、或对应板块版主可见。
     * 2. 搜索增强：支持按关键字模糊搜索标题、正文、标签名及作者昵称。
     * 3. 排序策略：默认置顶帖 (is_top) 优先。在管理后台审核场景下，待审核帖 (status=0) 拥有最高优先级。
     */
    @Override
    public Page<PostVO> getPostList(Page<Post> page, Integer boardId, Long userId, String keyword, Integer status, String sortField, String sortOrder) {
        User currentUser = getCurrentUserSafely();
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRole());
        boolean isModerator = currentUser != null && "MODERATOR".equals(currentUser.getRole());
        boolean queryOwnPosts = currentUserId != null && userId != null && currentUserId.equals(userId);

        // A. 查询帖子基本数据 (优化：只查询必要的列，content 只查询前 300 字符用于摘要)
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.select("id", "board_id", "user_id", "title", "LEFT(content, 300) as content", 
                       "view_count", "reply_count", "like_count", "is_top", "create_time", 
                       "status", "tags");

        // 如果传了 boardId 就按板块筛选
        wrapper.eq(boardId != null, "board_id", boardId);
        // 如果传了userId 就按照userId筛选
        wrapper.eq(userId != null && userId != 0, "user_id", userId);
        // 如果传了 status 就按状态筛选
        if (status != null && status != -1) {
            if (status != 1 && !queryOwnPosts && !isAdmin && !isModerator) {
                throw new BusinessException(403, "无权查看该状态的帖子");
            }
            wrapper.eq("status", status);
        } else if (status == null) {
            wrapper.eq("status", 1);
        } else if (!queryOwnPosts && !isAdmin && !isModerator) {
            throw new BusinessException(403, "无权查看全部状态的帖子");
        }

        if (isModerator && !isAdmin && status != null && status != 1 && !queryOwnPosts) {
            wrapper.eq("board_id", currentUser.getManagedBoardId());
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> {
                q.like("title", keyword)
                        .or()
                        .like("content", keyword)
                        .or()
                        .like("tags", keyword);

                List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                                .and(u -> u.like(User::getNickname, keyword).or().like(User::getUsername, keyword)))
                        .stream()
                        .map(User::getId)
                        .collect(Collectors.toList());

                if (!matchedUserIds.isEmpty()) {
                    q.or().in("user_id", matchedUserIds);
                }
            });
        }

        // 管理场景下，待审核帖子优先于置顶帖，方便后台优先处理审核任务。
        boolean prioritizePendingForManagement = !queryOwnPosts
                && (isAdmin || isModerator)
                && status != null
                && status == -1;

        String pendingPriorityOrder = prioritizePendingForManagement
                ? "CASE WHEN status = 0 THEN 0 ELSE 1 END ASC, "
                : "";

        // 排序逻辑：默认置顶帖子排在最前；管理场景下先按待审核优先，再看置顶与其他排序字段。
        // 使用 last 强制处理排序，确保 NULL 值也能正确处理。
        if ("random".equals(sortField)) {
            wrapper.last("ORDER BY " + pendingPriorityOrder + "COALESCE(is_top, 0) DESC, RAND()");
        } else {
            String baseOrder = "ORDER BY " + pendingPriorityOrder + "COALESCE(is_top, 0) DESC";
            if ("viewCount".equals(sortField)) {
                wrapper.last(baseOrder + ", view_count DESC, create_time DESC, id DESC");
            } else if ("replyCount".equals(sortField)) {
                wrapper.last(baseOrder + ", IFNULL(reply_count, 0) DESC, create_time DESC, id DESC");
            } else {
                wrapper.last(baseOrder + ", create_time DESC, id DESC");
            }
        }

        Page<Post> postPage = this.page(page, wrapper);

        // 如果没有数据，直接返回空
        if (postPage.getRecords().isEmpty()) {
            return new Page<>();
        }

        // B. 批量查询关联数据 (User, Board) - 避免循环查库
        // 收集所有 userId, boardId
        Set<Long> userIds = postPage.getRecords().stream().map(Post::getUserId).collect(Collectors.toSet());
        Set<Integer> boardIds = postPage.getRecords().stream().map(Post::getBoardId).collect(Collectors.toSet());

        // 查 User Map
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        // 查 Board Map
        Map<Integer, Board> boardMap = boardIds.isEmpty() ? Map.of() : boardMapper.selectBatchIds(boardIds).stream()
                .collect(Collectors.toMap(Board::getId, board -> board));
        
        // C. 组装 VO
        List<PostVO> voList = postPage.getRecords().stream().map(post -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(post, vo); // 复制基本属性

            // 填充作者信息
            User user = userMap.get(post.getUserId());
            if (user != null) {
                vo.setAuthorName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
                vo.setAuthorAvatar(user.getAvatar());
            }

            // 填充板块名称
            Board board = boardMap.get(post.getBoardId());
            if (board != null) {
                vo.setBoardName(board.getName());
            }

            // 直接使用 post 实体自带的标签列表
            vo.setTags(post.getTags() == null ? List.of() : post.getTags());
            vo.setReplyCount(post.getReplyCount() == null ? 0 : post.getReplyCount());

            // 列表页内容太长可以截取 (可选)
            if (vo.getContent().length() > 200) {
                vo.setContent(vo.getContent().substring(0, 200) + "...");
            }
            return vo;
        }).collect(Collectors.toList());

        // D. 重新封装 Page 对象返回
        Page<PostVO> resultPage = new Page<>();
        BeanUtils.copyProperties(postPage, resultPage, "records");
        resultPage.setRecords(voList);

        return resultPage;
    }

    /**
     * 获取帖子详情。
     * 
     * 安全逻辑：
     * - 调用 ensureCanViewPost 校验当前用户是否有权查看该状态下的帖子。
     * - 仅对“已发布”状态的帖子增加阅读量统计，防止通过预览未审核帖子刷点击。
     */
    @Transactional
    public PostVO getPostDetail(Long id, boolean increaseView) {
        Post post = this.getById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        ensureCanViewPost(post);

        if (increaseView && post.getStatus() != null && post.getStatus() == 1) {
            this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, id)
                    .setSql("view_count = IFNULL(view_count, 0) + 1"));
            post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        }

        // 转换 VO
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);

        // 填充作者
        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            vo.setAuthorName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            vo.setAuthorAvatar(user.getAvatar());
        }

        // 填充板块
        Board board = boardMapper.selectById(post.getBoardId());
        if (board != null) {
            vo.setBoardName(board.getName());
        }

        // 填充标签 (直接从实体获取)
        vo.setTags(post.getTags() == null ? List.of() : post.getTags());
        vo.setReplyCount(post.getReplyCount() == null ? 0 : post.getReplyCount());

        // 填充当前用户的互动状态
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null) {
            vo.setHasLiked(postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                    .eq(PostLike::getPostId, id)
                    .eq(PostLike::getUserId, currentUserId)) > 0);
            vo.setHasFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getPostId, id)
                    .eq(Favorite::getUserId, currentUserId)) > 0);
        } else {
            vo.setHasLiked(false);
            vo.setHasFavorited(false);
        }

        return vo;
    }

    private User getCurrentUserSafely() {
        try {
            Long userId = SecurityUtils.getUserId();
            return userMapper.selectById(userId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void ensureCanViewPost(Post post) {
        if (post.getStatus() != null && post.getStatus() == 1) {
            return;
        }
        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录或不存在");
        }
        boolean isAuthor = post.getUserId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isBoardModerator = "MODERATOR".equals(currentUser.getRole())
                && Objects.equals(post.getBoardId(), currentUser.getManagedBoardId());
        if (!isAuthor && !isAdmin && !isBoardModerator) {
            throw new BusinessException(403, "无权查看该帖子");
        }
    }

    private void removePostCascade(Post post) {
        if (post == null) {
            return;
        }

        updateImageRefCount(post.getContent(), null);

        Long postId = post.getId();
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId));
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, postId));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId));
        this.removeById(postId);
    }

    private List<TagSimple> resolveTags(List<Integer> selectedTagIds, List<String> customTags) {
        Set<TagSimple> finalTags = new java.util.LinkedHashSet<>();

        // 1. 处理已选标签 ID
        if (selectedTagIds != null && !selectedTagIds.isEmpty()) {
            List<Tag> existingTags = tagMapper.selectBatchIds(selectedTagIds);
            for (Tag t : existingTags) {
                finalTags.add(new TagSimple(t.getId(), t.getName()));
            }
        }

        // 2. 处理自定义标签名称
        if (customTags != null && !customTags.isEmpty()) {
            List<String> normalizedCustomTags = customTags.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(tagName -> !tagName.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            if (!normalizedCustomTags.isEmpty()) {
                // 查找数据库中已存在的自定义标签
                Map<String, Tag> existingTagMap = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                                .in(Tag::getName, normalizedCustomTags))
                        .stream()
                        .collect(Collectors.toMap(Tag::getName, tag -> tag, (left, right) -> left));

                for (String tagName : normalizedCustomTags) {
                    Tag tag = existingTagMap.get(tagName);
                    if (tag == null) {
                        // 如果不存在则创建
                        tag = new Tag();
                        tag.setName(tagName);
                        tagMapper.insert(tag);
                    }
                    finalTags.add(new TagSimple(tag.getId(), tag.getName()));
                }
            }
        }

        return new ArrayList<>(finalTags);
    }

    private void updateImageRefCount(String oldContent, String newContent) {
        Set<String> oldImages = extractImageFilenames(oldContent);
        Set<String> newImages = extractImageFilenames(newContent);

        // 新增的引用
        Set<String> added = new java.util.HashSet<>(newImages);
        added.removeAll(oldImages);
        for (String filename : added) {
            sysFileMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysFile>()
                    .eq(SysFile::getFilename, filename)
                    .setSql("ref_count = ref_count + 1"));
        }

        // 减少的引用
        Set<String> removed = new java.util.HashSet<>(oldImages);
        removed.removeAll(newImages);
        for (String filename : removed) {
            sysFileMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysFile>()
                    .eq(SysFile::getFilename, filename)
                    .setSql("ref_count = IF(ref_count > 0, ref_count - 1, 0)"));
        }
    }

    private Set<String> extractImageFilenames(String content) {
        if (!StringUtils.hasText(content)) {
            return java.util.Collections.emptySet();
        }
        Set<String> filenames = new java.util.HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("!\\[.*?\\]\\(.*?/uploads/(.*?)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            filenames.add(matcher.group(1));
        }
        return filenames;
    }

    /**
     * 删除帖子（支持作者本人、超级管理员、或板块版主）。
     *
     * 权限逻辑：
     * 1. 帖子作者：可以删除自己的任何帖子。
     * 2. 超级管理员 (ADMIN)：可以删除全站任何帖子。
     * 3. 板块版主 (MODERATOR)：可以删除自己管理板块下的任何帖子。
     */
    @Override
    @Transactional
    public void deletePost(Long postId) {
        Long currentUserId = SecurityUtils.getUserId();
        Post post = this.getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }

        boolean isAuthor = post.getUserId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isBoardModerator = "MODERATOR".equals(currentUser.getRole())
                && post.getBoardId().equals(currentUser.getManagedBoardId());

        if (!isAuthor && !isAdmin && !isBoardModerator) {
            throw new BusinessException(403, "无权删除该帖子");
        }

        removePostCascade(post);
    }

    /**
     * 修改帖子（仅限作者本人）。
     *
     * 业务规则：
     * - 校验帖子是否存在。
     * - 校验当前操作人是否为帖子作者。
     * - 更新标题与内容。
     * - 如果帖子曾因“拒绝”而处于状态 2，修改后重置为“待审核”(状态 0)。
     * - 重新计算图片引用计数。
     * - 给板块版主发送内容更新通知。
     */
    @Override
    @Transactional
    public void updatePost(PostUpdateRequest req) {
        Long currentUserId = SecurityUtils.getUserId();
        Post post = this.getById(req.getId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        // 越权校验：只有作者可以编辑
        if (!post.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权修改他人的帖子");
        }

        // 保存旧内容用于更新图片引用计数
        String oldContent = post.getContent();
        
        // 处理标签更新
        List<TagSimple> finalTags = resolveTags(req.getTagIds(), req.getCustomTags());

        // 状态流转：修改后统一进入待审核状态 (0)，并清空之前的拒绝理由
        // 使用实体更新以确保 tags 字段能通过 JacksonTypeHandler 正确序列化为 JSON
        // 同时使用 UpdateWrapper 强制将 rejectReason 设为 null
        Post updateEntity = new Post();
        updateEntity.setId(req.getId());
        updateEntity.setTitle(req.getTitle());
        updateEntity.setContent(req.getContent());
        updateEntity.setStatus(0);
        updateEntity.setTags(finalTags);
        updateEntity.setUpdateTime(LocalDateTime.now());

        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Post::getId, req.getId())
                .set(Post::getRejectReason, null);

        this.update(updateEntity, updateWrapper);

        // 更新图片引用计数 (sys_file.ref_count)
        updateImageRefCount(oldContent, req.getContent());

        // 构造一个临时的 post 对象用于发送通知
        post.setTitle(req.getTitle());
        post.setBoardId(post.getBoardId()); // 保持原板块

        // 发送通知给版主
        notifyModeratorOfUpdate(post, true);
    }

    /**
     * 通知板块版主关于帖子更新的情况。
     */
    private void notifyModeratorOfUpdate(Post post, boolean isResubmit) {
        User moderator = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "MODERATOR")
                .eq(User::getManagedBoardId, post.getBoardId())
                .last("LIMIT 1"));
        
        if (moderator != null) {
            String today = java.time.LocalDate.now().toString();
            String type = "SYSTEM";
            String title = "【待审核】";
            String msg = title + today + " 板块有帖子内容变动，需重新审核：《" + post.getTitle() + "》";
            messageService.createSystemNotification(0L, moderator.getId(), type, msg);
        }
    }

    /**
     * 审核帖子。
     * 
     * 业务规则：
     * - 0 待审核, 1 已发布, 2 已拒绝。
     * - 审核通过后，如果是被拒绝后重新提交的，清空拒绝理由。
     * - 审核结果会通过系统通知告知作者。
     */
    @Override
    @Transactional
    public void auditPost(Long postId, Integer status, String rejectReason) {
        Post post = this.getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        // 使用 UpdateWrapper 确保状态更新和拒绝理由的清空（特别是 null 的情况）
        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Post::getId, postId)
                .set(Post::getStatus, status)
                .set(Post::getUpdateTime, LocalDateTime.now());
        
        if (status == 2) {
            updateWrapper.set(Post::getRejectReason, rejectReason);
        } else if (status == 1) {
            updateWrapper.set(Post::getRejectReason, null);
        }

        this.update(updateWrapper);

        // 发送系统通知给作者
        String statusText = (status == 1) ? "已通过" : "被拒绝";
        String reasonText = (status == 2 && StringUtils.hasText(rejectReason)) ? "。理由：" + rejectReason : "";
        messageService.createSystemNotification(0L, post.getUserId(), "SYSTEM",
                "你的帖子《" + post.getTitle() + "》审核结果：" + statusText + reasonText);
    }

    /**
     * 管理员/版主：强制删除帖子。
     */
    @Override
    @Transactional
    public void deletePostByAdmin(Long postId) {
        Post post = this.getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        removePostCascade(post);
    }

    /**
     * 切换置顶状态。
     */
    @Override
    @Transactional
    public void toggleTop(Long postId) {
        Post post = this.getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        boolean newTopStatus = !Boolean.TRUE.equals(post.getIsTop());
        post.setIsTop(newTopStatus);
        this.updateById(post);

        if (newTopStatus) {
            messageService.createSystemNotification(0L, post.getUserId(), "SYSTEM",
                    "你的帖子《" + post.getTitle() + "》已被管理员置顶。");
        }
    }

    /**
     * 获取热门帖子。
     * 排序规则：置顶优先，其次按回复数、浏览量、创建时间综合排序。
     */
    @Override
    public List<PostVO> getHotPosts(Integer boardId, Integer limit) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        // 性能优化：热门文章列表不需要 content 字段
        wrapper.select(Post::getId, Post::getTitle, Post::getBoardId, Post::getUserId, 
                      Post::getCreateTime, Post::getIsTop, Post::getReplyCount, Post::getViewCount, Post::getTags);
        
        wrapper.eq(Post::getStatus, 1) // 必须是已发布的
                .eq(boardId != null, Post::getBoardId, boardId)
                .orderByDesc(Post::getIsTop)
                .orderByDesc(Post::getReplyCount)
                .orderByDesc(Post::getViewCount)
                .orderByDesc(Post::getCreateTime)
                .last("LIMIT " + limit);

        List<Post> posts = this.list(wrapper);
        if (posts.isEmpty()) return List.of();

        // 批量填充作者名
        Set<Long> uIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, String> uMap = userMapper.selectBatchIds(uIds).stream()
                .collect(Collectors.toMap(User::getId, u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername()));

        return posts.stream().map(p -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(p, vo);
            vo.setAuthorName(uMap.get(p.getUserId()));
            vo.setTags(p.getTags() == null ? List.of() : p.getTags());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户点赞过的帖子列表（分页）。
     */
    @Override
    public Page<PostVO> getLikedPostList(Page<Post> page, Long userId) {
        // 1. 先查出该用户点赞的所有帖子 ID
        List<Long> postIds = postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .orderByDesc(PostLike::getCreateTime))
                .stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return new Page<>();
        }

        // 2. 分页查询帖子详情
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Post::getId, postIds)
                .eq(Post::getStatus, 1); // 只看已发布的

        // 维持点赞顺序
        String idStr = postIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        wrapper.last("ORDER BY FIELD(id, " + idStr + ")");

        Page<Post> postPage = this.page(page, wrapper);
        return convertToVOPage(postPage);
    }

    @Override
    public Page<PostVO> getFavoritePostList(Page<Post> page, Long userId) {
        // 1. 先查出该用户收藏的所有帖子 ID
        List<Long> postIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime))
                .stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return new Page<>();
        }

        // 2. 分页查询帖子详情
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Post::getId, postIds)
                .eq(Post::getStatus, 1); // 只看已发布的

        // 维持收藏顺序
        String idStr = postIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        wrapper.last("ORDER BY FIELD(id, " + idStr + ")");

        Page<Post> postPage = this.page(page, wrapper);
        return convertToVOPage(postPage);
    }

    /**
     * 将 Post 分页对象转换为 PostVO 分页对象。
     */
    private Page<PostVO> convertToVOPage(Page<Post> postPage) {
        if (postPage.getRecords().isEmpty()) {
            return new Page<>();
        }

        // 批量查询 User 和 Board
        Set<Long> uIds = postPage.getRecords().stream().map(Post::getUserId).collect(Collectors.toSet());
        Set<Integer> bIds = postPage.getRecords().stream().map(Post::getBoardId).collect(Collectors.toSet());

        Map<Long, User> uMap = uIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(uIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Integer, Board> bMap = bIds.isEmpty() ? Map.of() : boardMapper.selectBatchIds(bIds).stream()
                .collect(Collectors.toMap(Board::getId, b -> b));

        List<PostVO> voList = postPage.getRecords().stream().map(p -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(p, vo);
            User u = uMap.get(p.getUserId());
            if (u != null) {
                vo.setAuthorName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
                vo.setAuthorAvatar(u.getAvatar());
            }
            Board b = bMap.get(p.getBoardId());
            if (b != null) vo.setBoardName(b.getName());
            vo.setTags(p.getTags() == null ? List.of() : p.getTags());
            return vo;
        }).collect(Collectors.toList());

        Page<PostVO> resultPage = new Page<>();
        BeanUtils.copyProperties(postPage, resultPage, "records");
        resultPage.setRecords(voList);
        return resultPage;
    }
}

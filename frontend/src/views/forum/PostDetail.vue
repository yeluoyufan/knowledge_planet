<template>
    <div class="post-detail-container">
      <el-row :gutter="20">
        <el-col :span="17" :xs="24">
          <el-skeleton v-if="loading && !post" animated>
            <template #template>
              <el-card class="post-card" shadow="never">
                <div class="post-header">
                  <div class="title-row">
                    <el-skeleton-item variant="h3" style="width: 60%" />
                    <el-skeleton-item variant="button" style="width: 90px; height: 32px" />
                  </div>
                  <div class="post-meta" style="margin-top: 12px">
                    <el-skeleton-item variant="text" style="width: 120px" />
                    <el-skeleton-item variant="text" style="width: 160px" />
                    <el-skeleton-item variant="text" style="width: 200px" />
                  </div>
                </div>
                <el-divider />
                <el-skeleton-item variant="text" style="width: 96%" />
                <el-skeleton-item variant="text" style="width: 92%" />
                <el-skeleton-item variant="text" style="width: 88%" />
              </el-card>
            </template>
          </el-skeleton>

          <el-card class="post-card" shadow="never" v-else-if="post">
            <div class="post-header">
              <div class="title-row">
                <h1 class="post-title">{{ post.title }}</h1>
                <div class="header-actions">
                  <el-button
                    :type="isLiked ? 'primary' : 'info'"
                    plain
                    :loading="likeLoading"
                    :disabled="interactionDisabled"
                    @click.stop="handleToggleLike"
                  >
                    <el-icon style="margin-right: 6px">
                      <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor" aria-hidden="true">
                        <path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14 1 7.59 7.41C7.22 7.78 7 8.3 7 8.83V19c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z" />
                      </svg>
                    </el-icon>
                    {{ isLiked ? '已点赞' : '点赞' }} {{ post.likeCount || 0 }}
                  </el-button>
                  <el-button
                    type="warning"
                    plain
                    :icon="isFavorited ? StarFilled : Star"
                    :loading="favoriteLoading"
                    :disabled="interactionDisabled"
                    @click.stop="handleToggleFavorite"
                  >
                    {{ isFavorited ? '已收藏' : '收藏' }}
                  </el-button>
                </div>
              </div>
              <div class="post-meta">
                <el-tag size="small">{{ post.boardName }}</el-tag>
                <span class="meta-text">作者: {{ post.authorName }}</span>
                <span class="meta-text">发布于: {{ formatTime(post.createTime) }}</span>
                <span class="meta-text">阅读: {{ post.viewCount }}</span>
              </div>
            </div>
            
            <el-divider />
            
            <v-md-preview :text="post.content"></v-md-preview>
          </el-card>
  
          <el-card class="comment-card" shadow="never">
            <div class="comment-header">
              <h3>评论 ({{ post?.replyCount || 0 }})</h3>
            </div>
  
            <div class="comment-input-area">
              <el-input
                v-model="mainCommentContent"
                type="textarea"
                :rows="3"
                placeholder="写下你的评论..."
                resize="none"
                :disabled="interactionDisabled"
              />
              <div class="input-actions">
                <el-button type="primary" @click="submitMainComment" :loading="submitting" :disabled="interactionDisabled">发表评论</el-button>
              </div>
            </div>
  
            <div class="comment-list">
              <el-empty v-if="comments.length === 0" description="暂无评论，快来抢沙发" />
              
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="avatar-col">
                  <el-avatar :src="comment.authorAvatar || defaultAvatar" class="clickable-avatar" @click.stop="goToUser(comment.userId)" />
                </div>
                
                <div class="content-col">
                  <div class="user-info">
                    <span class="clickable-name" @click="goToUser(comment.userId)">{{ comment.authorName }}</span>
                    <span class="time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  
                  <div class="comment-text">{{ comment.content }}</div>
                  
                  <div class="action-bar">
                    <el-button link type="primary" size="small" @click="openReplyBox(comment)" :disabled="interactionDisabled">回复</el-button>
                    <el-button 
                      v-if="isCommentOwner(comment.userId)" 
                      link type="danger" size="small" 
                      @click="handleDelete(comment.id)"
                    >删除</el-button>
                  </div>
  
                  <div class="sub-comment-wrapper" v-if="comment.childCount > 0 || (comment.subComments && comment.subComments.length > 0)">
                    
                    <div v-if="comment.showReplies" class="sub-list">
                        <div v-for="sub in comment.subComments" :key="sub.id" class="sub-item">
                            <div class="sub-item-content">
                              <span class="sub-user" @click="goToUser(sub.userId)">{{ sub.authorName }}</span>
                              <template v-if="sub.replyToUserNickname && sub.parentId !== comment.id">
                                <span class="reply-label"> 回复 </span>
                                <span class="sub-user" @click="goToUser(sub.replyToUserId!)">{{ sub.replyToUserNickname }}</span>
                              </template>
                              <span class="sub-sep">：</span>
                              <span class="sub-text">{{ stripReplyPrefix(sub.content) }}</span>
                            </div>
                            
                            <div class="sub-footer">
                                <span class="sub-time">{{ formatTime(sub.createTime) }}</span>
                                <div class="sub-actions">
                                    <el-button link size="small" @click="openReplyBox(comment, sub)" :disabled="interactionDisabled">回复</el-button>
                                    <el-button 
                                    v-if="isCommentOwner(sub.userId)" 
                                    link type="danger" size="small" 
                                    @click="handleDelete(sub.id, comment)"
                                    >删除</el-button>
                                </div>
                            </div>
                        </div>
                    </div>
  
                    <div class="expand-btn" v-if="comment.childCount > 0">
                      <el-button 
                        v-if="!comment.showReplies" 
                        link type="info" 
                        @click="fetchSubComments(comment)"
                        :loading="comment.loadingReplies"
                      >
                        查看 {{ comment.childCount }} 条回复 <el-icon><ArrowDown /></el-icon>
                      </el-button>
                      <el-button 
                        v-else 
                        link type="info" 
                        @click="comment.showReplies = false"
                      >
                        收起回复 <el-icon><ArrowUp /></el-icon>
                      </el-button>
                    </div>
                  </div>
  
                  <div v-if="activeReplyId === comment.id" class="inline-reply-box">
                    <el-input 
                      v-model="replyContent" 
                      :placeholder="replyPlaceholder" 
                      size="small"
                      @keyup.enter="submitReply(comment)"
                    >
                      <template #append>
                        <el-button @click="submitReply(comment)" :loading="replySubmitting">发送</el-button>
                      </template>
                    </el-input>
                  </div>
  
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
  
        <el-col :span="7" class="hidden-xs-only">
          <div class="sticky-sidebar">
            <el-card shadow="hover" v-if="post">
              <template #header>作者</template>
              <div class="author-card">
                <el-avatar :size="60" :src="post.authorAvatar || defaultAvatar" class="clickable-avatar" @click="goToUser(post.userId)" />
                <h3 style="margin-top: 10px" class="clickable-name" @click="goToUser(post.userId)">{{ post.authorName }}</h3>
              </div>
            </el-card>

            <el-card shadow="hover" v-if="toc.length > 0">
              <template #header>
                <div class="card-header">
                  <span>📑 文章目录</span>
                </div>
              </template>
              <div class="toc-list">
                <div
                  v-for="item in toc"
                  :key="item.id"
                  class="toc-item"
                  :class="[`toc-h${item.level}`, { active: activeHeading === item.id }]"
                  @click="scrollToHeading(item.id)"
                >
                  {{ item.text }}
                </div>
              </div>
            </el-card>
          </div>
        </el-col>
      </el-row>
      <el-backtop :right="24" :bottom="40" />
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, computed } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { useUserStore } from '@/store/userStore'
  import { getPostDetail, type PostVO } from '@/api/post'
  import { getCommentList, 
    getSubCommentList, 
    createComment, 
    deleteComment,
    type CommentVO 
  } from '@/api/comment'
  import { ArrowDown, ArrowUp, Star, StarFilled } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { getFavoriteStatus, toggleFavorite } from '@/api/favorite'
  import { getLikeStatus, toggleLike } from '@/api/like'
  import 'element-plus/theme-chalk/display.css'
  
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const postId = Number(route.params.id)
  const isCommentOwner = (commentUserId: number | undefined) =>
    Number(userStore.userInfo?.id) > 0 && Number(userStore.userInfo?.id) === Number(commentUserId)

  // --- 状态 ---
  const loading = ref(false)
  const post = ref<PostVO | null>(null)
  const comments = ref<CommentVO[]>([])
  const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  const isFavorited = ref(false)
  const favoriteLoading = ref(false)
  const isLiked = ref(false)
  const likeLoading = ref(false)
  const interactionDisabled = computed(() => (post.value?.status ?? 1) !== 1)

  // 目录导航相关
  interface TocItem {
    id: string
    text: string
    level: number
  }
  const toc = ref<TocItem[]>([])
  const activeHeading = ref<string>('')
  let tocObserver: IntersectionObserver | null = null
  
  // 发表评论相关
  const mainCommentContent = ref('')
  const submitting = ref(false)
  
  // 回复相关
  const activeReplyId = ref<number | null>(null) // 当前正在回复哪个一级评论
  const replyContent = ref('')
  const replyPlaceholder = ref('')
  const replyTargetId = ref<number | null>(null) // 实际要回复的 parentId (可能是层主，也可能是楼中楼ID)
  const replySubmitting = ref(false)

  const formatTime = (timeStr: string | undefined) => {
        if (!timeStr) return ''
        // 将 2023-11-22T12:00:00 转换为 2023-11-22 12:00
        return timeStr.replace('T', ' ').substring(0, 16)
    }

    // ✅ 新增：去除评论内容中可能存在的旧版“回复 @xxx : ”前缀
    const stripReplyPrefix = (content: string) => {
      if (!content) return ''
      return content.replace(/^回复\s*@.*?\s*:\s*/, '')
    }

    // 跳转到用户主页
    const goToUser = (userId: number) => {
    if (userId) {
        router.push(`/user/${userId}`)
    }
    }

    const stripMarkdown = (str: string) => {
        return str
            .replace(/^#+\s*/, '')
            .replace(/\*\*(.*?)\*\*/g, '$1')
            .replace(/\*(.*?)\*/g, '$1')
            .replace(/__(.*?)__/g, '$1')
            .replace(/_(.*?)_/g, '$1')
            .trim()
    }

    // 从 Markdown 内容中提取目录
    const generateToc = (content: string) => {
        if (!content) return
        
        // 移除代码块干扰，避免匹配到代码块内部的 # 标题
        const cleanContent = content.replace(/```[\s\S]*?```/g, '')
        // 允许标题前有空格，并确保匹配到行尾
        const headingRegex = /^\s*(#{1,5})\s+(.+)$/gm
        const items: TocItem[] = []
        let match
        while ((match = headingRegex.exec(cleanContent)) !== null) {
            const level = (match[1] || '#').length
            const rawText = (match[2] || '').trim()
            if (!rawText) continue
            const text = stripMarkdown(rawText)
            // 保持 ID 简单稳定，仅依赖索引
            const id = `heading-${items.length}`
            items.push({ id, text, level })
        }
        toc.value = items
    }

    const scrollToHeading = (id: string) => {
      const element = document.getElementById(id)
      if (element) {
        const offset = 80 // 避开顶栏遮挡
        const bodyRect = document.body.getBoundingClientRect().top
        const elementRect = element.getBoundingClientRect().top
        const elementPosition = elementRect - bodyRect
        const offsetPosition = elementPosition - offset

        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        })
      } else {
        console.warn(`未找到目标标题元素: ${id}`)
      }
    }

    const setupTocHighlight = () => {
        if (tocObserver) {
            tocObserver.disconnect()
        }

        tocObserver = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        activeHeading.value = entry.target.id
                    }
                })
            },
            { rootMargin: '-80px 0px -70% 0px', threshold: 0 }
        )

        // 使用多次检查机制，确保长文本渲染完成后绑定 ID
        let checkCount = 0
        const maxChecks = 10
        
        const doBind = () => {
            const container = document.querySelector('.v-md-editor-preview')
            if (!container) return

            const elements = container.querySelectorAll('h1, h2, h3, h4, h5')
            
            // 如果 DOM 中的标题数量还没达到解析出的数量，且重试次数未达上限，则继续等待
            if (elements.length < toc.value.length && checkCount < maxChecks) {
                checkCount++
                setTimeout(doBind, 500)
                return
            }

            elements.forEach((el, index) => {
                const id = `heading-${index}`
                el.id = id
                tocObserver?.observe(el)
            })
        }

        setTimeout(doBind, 600)
    }
  
  const initData = async () => {
    loading.value = true
    try {
      // 1. 获取帖子详情 (后端现在会返回 hasLiked 和 hasFavorited 状态)
      const postRes: any = await getPostDetail(postId)
      if (postRes.code === 0 || postRes.code === 200) {
        post.value = postRes.data
        // ✅ 动态修改浏览器标题
        document.title = `${post.value.title} - 知识星球`
        isFavorited.value = !!postRes.data.hasFavorited
        isLiked.value = !!postRes.data.hasLiked
        generateToc(postRes.data.content)
        setupTocHighlight()
      }
      
      // 2. 获取一级评论
      await refreshComments()
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }

  const handleToggleFavorite = async () => {
    if (interactionDisabled.value) {
      ElMessage.warning('帖子待审核期间不可收藏')
      return
    }
    favoriteLoading.value = true
    try {
      const res: any = await toggleFavorite(postId)
      if (res.code === 0 || res.code === 200) {
        isFavorited.value = !!res.data
        ElMessage.success(isFavorited.value ? '已收藏' : '已取消收藏')
      }
    } finally {
      favoriteLoading.value = false
    }
  }

  const handleToggleLike = async () => {
    if (interactionDisabled.value) {
      ElMessage.warning('帖子待审核期间不可点赞')
      return
    }
    likeLoading.value = true
    try {
      const res: any = await toggleLike(postId)
      if (res.code === 0 || res.code === 200) {
        isLiked.value = !!res.data
        ElMessage.success(isLiked.value ? '已点赞' : '已取消点赞')
        // 刷新帖子详情以更新点赞数
        const pRes: any = await getPostDetail(postId, false)
        if (pRes.data) post.value = pRes.data
      }
    } finally {
      likeLoading.value = false
    }
  }
  
  const refreshComments = async () => {
    const res: any = await getCommentList(postId)
    if (res.code === 0 || res.code === 200) {
      comments.value = res.data.map((item: CommentVO) => ({
        ...item,
        showReplies: false, // 默认收起
        subComments: [],
        loadingReplies: false
      }))
    }
  }
  
  // --- 楼中楼逻辑 ---
  const fetchSubComments = async (comment: CommentVO) => {
    comment.loadingReplies = true
    try {
      // 传入 rootId (即一级评论的 ID)
      const res: any = await getSubCommentList(comment.id)
      if (res.code === 0 || res.code === 200) {
        comment.subComments = res.data
        comment.showReplies = true
      }
    } catch (error) {
      console.error(error)
    } finally {
      comment.loadingReplies = false
    }
  }
  
  // 辅助：获取被回复人的名字 (在前端简单查找)
  const getTargetName = (parentId: number, subComments: CommentVO[] = []) => {
    const target = subComments.find(c => c.id === parentId)
    return target ? target.authorName : '未知用户'
  }
  
  // --- 交互操作 ---
  
  // 1. 发表一级评论
  const submitMainComment = async () => {
    if (interactionDisabled.value) {
      ElMessage.warning('帖子待审核期间不可评论')
      return
    }
    if (!mainCommentContent.value.trim()) return ElMessage.warning('请输入内容')
    
    submitting.value = true
    try {
      const res: any = await createComment({
        postId: postId,
        content: mainCommentContent.value,
        parentId: 0 // 0 代表一级评论 (视后端约定，有的可能不传)
      })
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('评论成功')
        mainCommentContent.value = ''
        await refreshComments()
        // 刷新帖子详情以更新评论数
        const pRes: any = await getPostDetail(postId, false)
        if (pRes.data) post.value = pRes.data
      }
    } finally {
      submitting.value = false
    }
  }
  
  // 2. 打开回复框 (回复层主 或 回复楼中楼)
  // rootComment: 该楼层的层主评论对象 (一级评论)
  // targetSub: 具体的回复目标 (如果是回复层主，则为 null)
  const openReplyBox = (rootComment: CommentVO, targetSub?: CommentVO) => {
    if (interactionDisabled.value) {
      ElMessage.warning('帖子待审核期间不可评论')
      return
    }
    // 如果已经打开且点击的是同一个，则关闭
    if (activeReplyId.value === rootComment.id && replyTargetId.value === (targetSub?.id || rootComment.id)) {
      activeReplyId.value = null
      return
    }
  
    activeReplyId.value = rootComment.id // 回复框挂载在一级评论下面
    replyContent.value = ''
    
    if (targetSub) {
      // 回复楼中楼
      replyTargetId.value = targetSub.id
      replyPlaceholder.value = `回复 @${targetSub.authorName}`
    } else {
      // 回复层主
      replyTargetId.value = rootComment.id
      replyPlaceholder.value = `回复 @${rootComment.authorName}`
    }
  }
  
  // 3. 提交子回复
  const submitReply = async (rootComment: CommentVO) => {
    if (interactionDisabled.value) {
      ElMessage.warning('帖子待审核期间不可评论')
      return
    }
    if (!replyContent.value.trim()) return ElMessage.warning('请输入内容')
    if (!replyTargetId.value) return
  
    replySubmitting.value = true
    try {
      const res: any = await createComment({
        postId: postId,
        content: replyContent.value,
        parentId: replyTargetId.value // 传入具体的 parentId
      })
      
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('回复成功')
        replyContent.value = ''
        activeReplyId.value = null // 关闭回复框
        
        // 重新加载该楼层的子评论
        await fetchSubComments(rootComment)
        // 可选：更新 childCount 显示 (简单 +1，或者依赖重新 fetch)
        // rootComment.childCount++ 
      }
    } finally {
      replySubmitting.value = false
    }
  }
  
  // 4. 删除评论
  const handleDelete = (id: number, parentComment?: CommentVO) => {
    ElMessageBox.confirm('确定删除这条评论吗？', '提示', {
      type: 'warning'
    }).then(async () => {
      const res: any = await deleteComment(id)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('删除成功')
        if (parentComment) {
          // 如果是子评论，刷新子列表
          fetchSubComments(parentComment)
        } else {
          // 如果是一级评论，刷新主列表
          refreshComments()
        }
      }
    })
  }
  
  onMounted(() => {
    initData()
  })
  </script>
  
  <style scoped lang="scss">
  .post-detail-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px 20px 40px;
  }
  
  .post-card {
    margin-bottom: 20px;
    min-height: 400px;
    border-radius: 16px;
    overflow: hidden;

    :deep(.el-card__body) {
      padding: 20px;
    }
    
    .post-header {
      .title-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;

        @media (max-width: 768px) {
          flex-direction: column;
          align-items: flex-start;
          
          .header-actions {
            width: 100%;
            justify-content: flex-start;
            margin-bottom: 12px;
          }
        }
        
        .header-actions {
          display: flex;
          gap: 10px;
          flex-shrink: 0;
        }
      }
      .post-title {
        font-size: 24px;
        margin: 0;
        line-height: 1.4;
      }
      .post-meta {
        color: #94a3b8;
        font-size: 13px;
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }
    }
  }

  :deep(.v-md-editor-preview) {
    font-size: 15px;
    line-height: 1.9;
    color: #1f2937;

    h1, h2, h3, h4, h5 {
      scroll-margin-top: 90px;
    }

    h1 { font-size: 26px; margin: 22px 0 14px; }
    h2 { font-size: 22px; margin: 20px 0 12px; }
    h3 { font-size: 18px; margin: 18px 0 10px; }

    p { margin: 10px 0; }
    ul, ol { padding-left: 22px; }

    blockquote {
      margin: 14px 0;
      padding: 10px 14px;
      border-left: 4px solid #cfe7ff;
      background: #f6fbff;
      color: #475569;
    }

    code {
      background: #f8fafc;
      padding: 2px 6px;
      border-radius: 6px;
    }
  }
  
  .comment-card {
    border-radius: 16px;
    overflow: hidden;

    :deep(.el-card__body) {
      padding: 20px;
    }

    .comment-input-area {
      margin-bottom: 30px;
      background: #f8fafc;
      padding: 16px;
      border-radius: 12px;
      border: 1px solid #f1f5f9;
      
      .input-actions {
        margin-top: 10px;
        text-align: right;
      }
    }
    
    .comment-item {
      display: flex;
      gap: 16px;
      padding: 18px 0;
      border-bottom: 1px solid #f1f5f9;

      &:last-child {
        border-bottom: none;
      }
      
      .avatar-col {
        flex-shrink: 0;
      }
      
      .content-col {
        flex-grow: 1;
        
        .user-info {
          display: flex;
          justify-content: space-between;
          margin-bottom: 6px;
          
          .username {
            font-weight: 600;
            font-size: 14px;
            color: #333;
          }
          .time {
            color: #94a3b8;
            font-size: 12px;
          }
        }
        
        .comment-text {
          font-size: 14px;
          color: #333;
          line-height: 1.6;
          margin-bottom: 8px;
        }
        
        .action-bar {
          margin-bottom: 10px;
        }
        
        .sub-comment-wrapper {
          background-color: #f7f8fa;
          padding: 14px;
          border-radius: 12px;
          margin-top: 10px;
          
          .sub-item {
            font-size: 13px;
            margin-bottom: 8px;
            padding: 10px 12px;
            border-radius: 8px;
            background: #fff;
            box-shadow: 0 1px 2px rgba(0,0,0,0.02);
            
            &:last-child { margin-bottom: 0; }
            
            .sub-item-content {
              margin-bottom: 6px;
              line-height: 1.5;
              
              .sub-user {
                font-weight: 700;
                color: #3b82f6;
                cursor: pointer;
                &:hover { text-decoration: underline; }
              }
              
              .sub-sep {
                color: #64748b;
                margin: 0 2px;
              }
              
              .reply-label {
                margin: 0 4px;
                color: #94a3b8;
                font-size: 12px;
              }
              
              .sub-text {
                color: #334155;
                word-break: break-all;
              }
            }
            
            .sub-footer {
              display: flex;
              justify-content: space-between;
              align-items: center;
              
              .sub-time {
                color: #94a3b8;
                font-size: 12px;
              }
              
              .sub-actions {
                display: flex;
                gap: 8px;
              }
            }
          }
          
          .expand-btn {
            margin-top: 8px;
            padding-top: 8px;
            border-top: 1px dashed #e2e8f0;
          }
        }
        
        .inline-reply-box {
          margin-top: 10px;
        }
      }
    }
  }
  
  .author-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px 0;
  }
  /* ✅ 新增：可点击的头像样式 */
.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s; /* 加个小动效更精致 */
}

.clickable-avatar:hover {
  transform: scale(1.1); /* 鼠标悬停放大一点点 */
}

/* ✅ 新增：可点击的名字样式（侧边栏） */
.clickable-name {
  cursor: pointer;
  transition: color 0.2s;
}

.clickable-name:hover {
  color: #409eff; /* 鼠标悬停变蓝 */
}

.sticky-sidebar {
  position: sticky;
  top: 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-right: 4px;

  /* 移除外部滚动，让内部 TOC 滚动 */
}

.toc-list {
  max-height: calc(100vh - 350px);
  overflow-y: auto;
  padding-right: 5px;
  padding-bottom: 20px;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #e2e8f0;
    border-radius: 2px;
  }
  &:hover {
    &::-webkit-scrollbar-thumb {
      background: #cbd5e1;
    }
  }

  .toc-item {
      cursor: pointer;
    padding: 8px 12px;
    font-size: 14px;
    color: #606266;
    border-left: 2px solid transparent;
    transition: all 0.3s;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    
    &:hover {
      color: #409eff;
      background-color: #f5f7fa;
    }
      
      &.active {
        color: #409eff;
        border-left-color: #409eff;
        background-color: #ecf5ff;
        font-weight: bold;
      }
    
    &.toc-h1 { font-weight: 600; }
    &.toc-h2 { padding-left: 24px; font-size: 13px; }
    &.toc-h3 { padding-left: 36px; font-size: 12px; color: #909399; }
    &.toc-h4 { padding-left: 48px; font-size: 12px; color: #909399; }
    &.toc-h5 { padding-left: 60px; font-size: 12px; color: #909399; }
  }
}

.card-header {
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .post-detail-container {
    padding: 14px 14px 30px;
  }

  .post-card {
    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  .comment-card {
    :deep(.el-card__body) {
      padding: 16px;
    }
  }
}
  </style>

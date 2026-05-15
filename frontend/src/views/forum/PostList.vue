<template>
    <div class="forum-container">
      <el-row :gutter="20">
        <el-col :span="17" :xs="24" class="main-col">
          <el-card class="post-list-card" shadow="never">
            
            <div class="toolbar">
              <div class="list-title">
                <el-icon><Document /></el-icon>
                <span>{{ activeBoardName }}</span>
              </div>
  
              <div class="toolbar-right">
                <div class="sort-tabs">
                  <span 
                    class="sort-tab" 
                    :class="{ active: activeSort === 'createTime' }"
                    @click="handleSortChange('createTime')"
                  >最新发布</span>
                  <span class="sort-divider">|</span>
                  <span 
                    class="sort-tab" 
                    :class="{ active: activeSort === 'viewCount' }"
                    @click="handleSortChange('viewCount')"
                  >最多浏览</span>
                  <span class="sort-divider">|</span>
                  <span 
                    class="sort-tab" 
                    :class="{ active: activeSort === 'replyCount' }"
                    @click="handleSortChange('replyCount')"
                  >最多评论</span>
                </div>
                
                <div class="search-box">
                  <el-input 
                    v-model="queryParams.keyword" 
                    placeholder="搜索..." 
                    prefix-icon="Search"
                    clearable
                    @clear="handleSearch"
                    @keyup.enter="handleSearch"
                    style="width: 200px"
                  >
                    <template #append>
                      <el-button :icon="Search" @click="handleSearch" />
                    </template>
                  </el-input>
                </div>
              </div>
            </div>
  
            <PostListSkeleton v-if="loading && postList.length === 0" />
            <div class="post-items" v-else v-infinite-scroll="loadMore" :infinite-scroll-disabled="infiniteDisabled" :infinite-scroll-distance="160">
              <el-empty v-if="postList.length === 0" description="暂无相关帖子" />
  
              <div v-for="post in postList" :key="post.id" class="post-item" @click="goToDetail(post.id)">
                <h3 class="post-title">
                  <el-tag v-if="post.isTop" size="small" type="danger" effect="dark" class="sticky-tag">置顶</el-tag>
                  <el-tag size="small" effect="plain" class="board-tag" v-if="post.boardName">
                    {{ post.boardName }}
                  </el-tag>
                  {{ post.title }}
                </h3>
                
                <div class="post-tags" v-if="post.tags && post.tags.length > 0">
                  <el-tag 
                    v-for="tag in post.tags" 
                    :key="tag.id" 
                    size="small" 
                    type="info" 
                    class="tag-item"
                  >
                    {{ tag.name }}
                  </el-tag>
                </div>

                <p class="post-summary">{{ stripMarkdown(post.content).substring(0, 100) }}...</p>
                
                <div class="post-meta">
                  <span class="meta-item">
                     <el-avatar :size="20" :src="post.authorAvatar || defaultAvatar" />
                     <span class="author-name">{{ post.authorName || '匿名用户' }}</span>
                  </span>
                  <span class="meta-divider">|</span>
                  <span class="meta-item">{{ formatTime(post.createTime) }}</span>
                  
                  <div class="post-stats">
                     <span class="stat-item">
                      <el-icon>
                        <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor" aria-hidden="true">
                          <path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14 1 7.59 7.41C7.22 7.78 7 8.3 7 8.83V19c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z" />
                        </svg>
                      </el-icon>
                      {{ post.likeCount || 0 }}
                     </span>
                     <span class="stat-item"><el-icon><View /></el-icon> {{ post.viewCount }}</span>
                     <span class="stat-item"><el-icon><ChatLineRound /></el-icon> {{ post.replyCount }}</span>
                  </div>
                </div>
              </div>
              
              <div class="load-more" v-if="postList.length > 0">
                <span v-if="loadingMore">加载中...</span>
                <span v-else-if="!hasMore">没有更多了</span>
                <el-button v-else-if="loadMoreError" link type="danger" @click="retryLoadMore">加载失败，点击重试</el-button>
              </div>
            </div>
          </el-card>
          <el-backtop :right="24" :bottom="40" />
        </el-col>
  
        <el-col :span="7" class="hidden-xs-only side-col">
          <div class="sticky-sidebar">
            <el-card class="sidebar-card welcome-card" shadow="hover">
              <div class="welcome-container">
                <div class="welcome-icon">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M2 12C2 12 5 13 8 13C11 13 13 11 16 11C19 11 22 12 22 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M7 8C7.55228 8 8 7.55228 8 7C8 6.44772 7.55228 6 7 6C6.44772 6 6 6.44772 6 7C6 7.55228 6.44772 8 7 8Z" fill="currentColor"/>
                    <path d="M16 17C16.5523 17 17 16.5523 17 16C17 15.4477 16.5523 15 16 15C15.4477 15 15 15.4477 15 16C15 16.5523 15.4477 17 16 17Z" fill="currentColor"/>
                  </svg>
                </div>
                <h3 class="welcome-title">欢迎来到知识星球</h3>
                <p class="welcome-desc">发现优质内容，分享经验与思考，和更多同频的人一起交流成长。</p>
              </div>
            </el-card>

            <el-card class="sidebar-card" shadow="hover">
              <template #header>
                <div class="card-header">
                  <span>📌 板块导航</span>
                </div>
              </template>
              <div class="board-tags">
                 <el-tag 
                   v-for="board in boardList" 
                   :key="board.id" 
                   class="topic-tag" 
                   :effect="activeTab === board.id ? 'dark' : 'light'"
                   :type="activeTab === board.id ? 'primary' : 'info'"
                   @click="handleTabChange(board.id)"
                 >
                   {{ board.name }}
                 </el-tag>
              </div>
            </el-card>
  
            <!-- 热门文章卡片 -->
            <el-card class="sidebar-card" shadow="hover" v-if="hotPosts.length > 0">
              <template #header>
                <div class="card-header">
                  <span>🔥 热门文章</span>
                </div>
              </template>
              <div class="hot-post-list">
                <div v-for="(post, index) in hotPosts" :key="post.id" class="hot-post-item" @click="goToDetail(post.id)">
                  <span class="rank-num" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                  <span class="hot-title">{{ post.title }}</span>
                </div>
              </div>
            </el-card>

            <el-card class="sidebar-card" shadow="hover" v-if="hotAuthors.length > 0">
              <template #header>
                <div class="card-header">
                  <span>🌟 热门作者</span>
                </div>
              </template>
              <div class="author-list">
                <div class="author-item" v-for="u in hotAuthors.slice(0, 3)" :key="u.id" @click="goToUser(u.id)">
                  <el-avatar :size="34" :src="u.avatar || defaultAvatar" />
                  <div class="author-info">
                    <div class="author-name">{{ u.nickname || u.username }}</div>
                    <div class="author-meta">粉丝 {{ u.fansCount || 0 }}</div>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </el-col>
      </el-row>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, reactive, onMounted, computed, watch } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { View, ChatLineRound, Search, Document, PriceTag } from '@element-plus/icons-vue' 
  import { getPostList, getHotPosts, type PostVO, type PostQuery } from '@/api/post'
  import { getBoardList, type Board } from '@/api/board'
  import { getHotAuthors, type HotAuthor } from '@/api/follow'
  import PostListSkeleton from '@/components/PostListSkeleton.vue'
  import 'element-plus/theme-chalk/display.css'
  
  const router = useRouter()
  const route = useRoute()
  const loading = ref(false)
  const activeTab = ref(Number(route.query.boardId) || 0) 
  const boardList = ref<Board[]>([]) 
  const postList = ref<PostVO[]>([]) 
  const hotPosts = ref<PostVO[]>([])
  const hotAuthors = ref<HotAuthor[]>([])
  const total = ref(0)
  const loadingMore = ref(false)
  const loadMoreError = ref(false)
  const skipRouteWatch = ref(false)
  const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

  const getScrollKey = () => `scroll_post_list_${route.fullPath}`
  const saveScroll = () => {
    sessionStorage.setItem(getScrollKey(), String(window.scrollY || 0))
  }
  const restoreScroll = () => {
    const raw = sessionStorage.getItem(getScrollKey())
    if (!raw) return
    const y = Number(raw)
    if (!Number.isFinite(y) || y <= 0) return
    requestAnimationFrame(() => window.scrollTo({ top: y, behavior: 'auto' }))
  }
  const clearScroll = () => {
    sessionStorage.removeItem(getScrollKey())
  }
  
  const getRandomCacheKey = () => `random_cache_post_list_${activeTab.value}`
  const saveRandomCache = () => {
    if (activeSort.value !== 'random') return
    const t = typeof route.query.t === 'string' ? route.query.t : String(Date.now())
    sessionStorage.setItem(getRandomCacheKey(), JSON.stringify({
      t,
      pageNum: queryParams.pageNum,
      total: total.value,
      postList: postList.value,
      scrollY: window.scrollY || 0
    }))
  }
  const restoreRandomCache = () => {
    const raw = sessionStorage.getItem(getRandomCacheKey())
    if (!raw) return false
    try {
      const data = JSON.parse(raw)
      if (!Array.isArray(data.postList) || data.postList.length === 0) return false
      activeSort.value = 'random'
      queryParams.sortField = 'random'
      queryParams.pageNum = Number(data.pageNum) || 1
      total.value = Number(data.total) || 0
      postList.value = data.postList
      skipRouteWatch.value = true
      router.replace({ query: { ...route.query, sortField: 'random', t: data.t } }).finally(() => {
        skipRouteWatch.value = false
      })
      const y = Number(data.scrollY) || 0
      requestAnimationFrame(() => window.scrollTo({ top: y, behavior: 'auto' }))
      return true
    } catch (e) {
      return false
    }
  }
  
  // 计算当前板块名称
  const activeBoardName = computed(() => {
    if (activeTab.value === 0) return '全部内容'
    const board = boardList.value.find(b => b.id === activeTab.value)
    return board ? board.name : '全部内容'
  })

  // ✅ 动态修改浏览器标题
  watch(activeBoardName, (newVal) => {
    document.title = `${newVal} - 知识星球`
  }, { immediate: true })

  // 当前排序方式
  const activeSort = ref(route.query.sortField as string || 'createTime')
  const sortLabel = computed(() => {
    const map: any = { createTime: '最新发布', viewCount: '最多浏览', replyCount: '最多评论' }
    return map[activeSort.value] || '最新发布'
  })
  
  // ✅ 修改：在 queryParams 中添加 keyword
  const queryParams = reactive<any>({
    pageNum: 1,
    pageSize: 10,
    boardId: Number(route.query.boardId) || undefined,
    keyword: route.query.keyword as string || '',
    sortField: activeSort.value,
    sortOrder: 'desc'
  })

  const fetchBoards = async () => {
    try {
      const res: any = await getBoardList()
      if (res.code === 0 || res.code === 200) {
        boardList.value = res.data
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchHotPosts = async () => {
    try {
      const boardId = activeTab.value === 0 ? undefined : activeTab.value
      const res: any = await getHotPosts(boardId, 5)
      if (res.code === 0 || res.code === 200) {
        hotPosts.value = res.data
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchHotAuthors = async () => {
    try {
      const res: any = await getHotAuthors(3)
      if (res.code === 0 || res.code === 200) {
        hotAuthors.value = res.data || []
      }
    } catch (e) {
      console.error('Fetch Hot Authors Error:', e)
    }
  }
  
  const fetchPosts = async (append: boolean = false) => {
    loading.value = true
    try {
      const params = { ...queryParams }
      if (activeTab.value === 0) {
        delete params.boardId
      } else {
        params.boardId = activeTab.value
      }
  
      const res: any = await getPostList(params)
      if (res.code === 0 || res.code === 200) {
        postList.value = append ? postList.value.concat(res.data.records) : res.data.records
        total.value = res.data.total
        if (queryParams.sortField === 'random') {
          saveRandomCache()
        }
      }
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }
  
  const hasMore = computed(() => postList.value.length < total.value)
  const infiniteDisabled = computed(() => loading.value || loadingMore.value || !hasMore.value)
  
  const loadMore = async () => {
    if (infiniteDisabled.value) return
    loadingMore.value = true
    loadMoreError.value = false
    try {
      queryParams.pageNum += 1
      await fetchPosts(true)
    } catch (e) {
      queryParams.pageNum -= 1
      loadMoreError.value = true
    } finally {
      loadingMore.value = false
    }
  }

  const retryLoadMore = () => {
    loadMore()
  }
  
  const handleSearch = () => {
    router.push({
      path: '/search',
      query: {
        keyword: queryParams.keyword || undefined,
        boardId: activeTab.value === 0 ? undefined : activeTab.value,
        sortField: activeSort.value
      }
    })
  }

  const fetchInitialData = async () => {
    loading.value = true
    try {
      // 并行执行所有初始请求
      await Promise.all([
        fetchBoards(),
        fetchPosts(false),
        fetchHotPosts(),
        fetchHotAuthors()
      ])
    } catch (error) {
      console.error('初始化首页数据失败:', error)
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    fetchInitialData()
    const fromDetail = sessionStorage.getItem('nav_from_post_detail') === '1'
    if (fromDetail) {
      sessionStorage.removeItem('nav_from_post_detail')
      restoreScroll()
    }
  })

  // 处理板块切换
  const handleTabChange = (name: any) => {
    queryParams.pageNum = 1
    const boardId = activeTab.value === name ? undefined : name
    queryParams.boardId = boardId
    activeTab.value = boardId || 0
    
    router.replace({
      query: { ...route.query, boardId: boardId }
    })
    
    postList.value = []
    clearScroll()
    
    // 切换板块时也并行请求
    Promise.all([
      fetchPosts(),
      fetchHotPosts()
    ])
  }

  // 处理排序方式变化
  const handleSortChange = (sortField: string) => {
    if (activeSort.value === sortField) {
      if (restoreRandomCache()) return
      activeSort.value = 'random'
      queryParams.sortField = 'random'
      queryParams.pageNum = 1
      postList.value = []
      const t = String(Date.now())
      skipRouteWatch.value = true
      router.replace({ query: { ...route.query, sortField: 'random', t } }).finally(() => {
        skipRouteWatch.value = false
      })
      clearScroll()
      fetchPosts()
      return
    }
    if (activeSort.value === 'random') {
      saveRandomCache()
    }
    activeSort.value = sortField
    queryParams.sortField = sortField
    queryParams.pageNum = 1
    postList.value = []
    clearScroll()
    router.replace({
      query: { ...route.query, sortField: sortField }
    })
    fetchPosts()
  }

  // ✅ 监听路由变化
  watch(() => route.query, (newQuery) => {
    if (skipRouteWatch.value) return
    const bId = Number(newQuery.boardId) || 0
    const sort = (newQuery.sortField as string) || 'createTime'
    const keyword = (newQuery.keyword as string) || ''
    
    activeTab.value = bId
    activeSort.value = sort
    
    queryParams.boardId = bId === 0 ? undefined : bId
    queryParams.sortField = sort
    queryParams.keyword = keyword
    queryParams.pageNum = 1
    postList.value = []
    clearScroll()
    
    // 路由变化时并行请求
    Promise.all([
      fetchPosts(),
      fetchHotPosts(),
      fetchHotAuthors()
    ])
  }, { deep: true })

  const goToDetail = (id: number) => {
    saveScroll()
    sessionStorage.setItem('nav_from_post_detail', '1')
    router.push(`/post/${id}`)
  }
 
  const goToUser = (id: number) => {
    router.push(`/user/${id}`)
  }
  
  const stripMarkdown = (content: string) => {
    if (!content) return ''
    return content
      .replace(/#+\s/g, '')             // 过滤标题
      .replace(/(\**|__)(.*?)\1/g, '$2') // 过滤粗体
      .replace(/(\*|_)(.*?)\1/g, '$2')    // 过滤斜体
      .replace(/`{1,3}(.*?)`{1,3}/g, '$1') // 过滤代码块
      .replace(/\[(.*?)\]\(.*?\)/g, '$1')  // 过滤链接
      .replace(/>\s+/g, '')              // 过滤引用
      .replace(/<[^>]*>?/gm, '')         // 过滤 HTML
      .replace(/\n+/g, ' ')              // 换行变空格
      .trim()
  }
  
  const formatTime = (timeStr: string) => {
    if (!timeStr) return ''
    return timeStr.replace('T', ' ').substring(0, 16)
  }
  </script>
  
  <style scoped lang="scss">
  .forum-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
  }
  
  .post-list-card {
    margin-bottom: 20px;
    border-radius: 16px;
    overflow: hidden;
    :deep(.el-card__body) {
      padding: 0;
    }
  }
  
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 16px 12px 16px;
    border-bottom: 1px solid #f1f5f9;
  }
  
  .list-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 18px;
    font-weight: bold;
  }
  
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 20px;
  }
  
  .sort-tabs {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 14px;
    color: #606266;
  }
  
  .sort-tab {
    cursor: pointer;
    transition: color 0.3s;
  }
  
  .sort-tab:hover, .sort-tab.active {
    color: #409eff;
    font-weight: bold;
  }
  
  .sort-divider {
    color: #dcdfe6;
  }
  
  .search-box {
    display: flex;
    align-items: center;
    
    :deep(.el-input__wrapper) {
      border-radius: 12px 0 0 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      transition: all 0.3s;
      
      &:hover, &.is-focus {
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
      }
    }

    :deep(.el-input-group__append) {
      border-radius: 0 12px 12px 0;
      background-color: #409eff;
      color: #fff;
      border: none;
      padding: 0;
      overflow: hidden;

      .el-button {
        color: #fff;
        margin: 0;
        border: none;
        height: 100%;
        padding: 0 15px;
        border-radius: 0;
        
        &:hover {
          background-color: #66b1ff;
        }
      }
    }
  }
  
  .post-item {
    padding: 18px 16px;
    border-bottom: 1px solid #f1f5f9;
    cursor: pointer;
    transition: background-color 0.2s;
    
    &:hover {
      background-color: #f8fafc;
    }

    &:last-child {
      border-bottom: none;
    }
  }
  
  .post-title {
    font-size: 18px;
    font-weight: bold;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 10px;

    .sticky-tag {
      border-radius: 6px;
    }

    .board-tag {
      border-radius: 6px;
    }
  }
  
  .post-summary {
    font-size: 14px;
    color: #606266;
    margin-bottom: 15px;
    line-height: 1.6;
  }
  
  .post-meta {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: #909399;
  }
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 5px;
  }
  
  .author-name {
    color: #303133;
  }
  
  .meta-divider {
    margin: 0 10px;
  }
  
  .post-stats {
    margin-left: auto;
    display: flex;
    gap: 15px;
  }
  
  .stat-item {
    display: flex;
    align-items: center;
    gap: 5px;
  }
  
  .sidebar-card {
    margin-bottom: 20px;
    border-radius: 16px;
  }
  
  .card-header {
    font-weight: bold;
  }
  
  .board-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .topic-tag {
    cursor: pointer;
    border-radius: 8px;
    transition: all 0.3s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
    }
  }
  
  .hot-post-list, .author-list {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }
  
  .hot-post-item {
    display: flex;
    gap: 10px;
    cursor: pointer;
  }
  
  .rank-num {
    font-weight: bold;
    color: #909399;
  }
  
  .rank-1 { color: #f56c6c; }
  .rank-2 { color: #e6a23c; }
  .rank-3 { color: #409eff; }
  
  .hot-title {
    font-size: 14px;
    color: #606266;
  }
  
  .hot-title:hover {
    color: #409eff;
  }
  
  .author-item {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
  }
  
  .author-name {
    font-size: 14px;
    font-weight: bold;
  }
  
  .author-meta {
    font-size: 12px;
    color: #909399;
  }
  
  .load-more {
    text-align: center;
    padding: 20px 0;
    color: #909399;
  }
  
  .sticky-sidebar {
    position: sticky;
    top: 80px;
  }

  .post-tags {
    margin-bottom: 10px;
  }
  
  .tag-item {
    margin-right: 5px;
    border-radius: 6px;
  }

  .welcome-card {
    position: relative;
    border: none;
    background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 100%);
    box-shadow: 0 10px 25px -10px rgba(59, 130, 246, 0.15);
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 15px 35px -12px rgba(59, 130, 246, 0.2);
    }

    &::before {
      content: '';
      position: absolute;
      top: -20px;
      right: -20px;
      width: 100px;
      height: 100px;
      background: radial-gradient(circle, rgba(64, 158, 255, 0.08) 0%, transparent 70%);
      border-radius: 50%;
    }

    .welcome-container {
      padding: 24px 20px;
      text-align: center;
      position: relative;
      z-index: 1;
    }

    .welcome-icon {
      width: 48px;
      height: 48px;
      margin: 0 auto 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 100%);
      color: #3b82f6;
      border-radius: 14px;
      
      svg {
        width: 24px;
        height: 24px;
      }
    }

    .welcome-title {
      margin: 0 0 10px 0;
      font-size: 18px;
      font-weight: 800;
      color: #1e293b;
      letter-spacing: 0.5px;
    }

    .welcome-desc {
      margin: 0;
      font-size: 14px;
      color: #64748b;
      line-height: 1.6;
    }
  }

  @media (max-width: 768px) {
    .forum-container {
      padding: 14px;
    }

    .toolbar {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .toolbar-right {
      width: 100%;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }
  }
  </style>

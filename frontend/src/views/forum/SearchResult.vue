<template>
  <div class="forum-container">
    <el-row :gutter="20">
      <el-col :span="24" :xs="24">
        <el-card class="post-list-card" shadow="never">
          
          <div class="toolbar">
            <div class="list-title">
              <el-icon><Search /></el-icon>
              <span v-if="query.keyword">“{{ query.keyword }}” 的搜索结果</span>
              <span v-else>全部内容</span>
            </div>

            <div class="toolbar-right">
              <div class="sort-tabs" v-if="activeTab === 'posts'">
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
                  v-model="query.keyword" 
                  placeholder="搜索..." 
                  prefix-icon="Search"
                  clearable
                  @clear="applySearch"
                  @keyup.enter="applySearch"
                  style="width: 200px"
                >
                  <template #append>
                    <el-button :icon="Search" @click="applySearch" />
                  </template>
                </el-input>
              </div>
            </div>
          </div>

          <div class="search-tabs-container">
            <el-tabs v-model="activeTab" class="search-tabs" @tab-change="handleTabChange">
              <el-tab-pane label="帖子" name="posts">
                <PostListSkeleton v-if="loading && postList.length === 0" />
                <div class="post-items" v-else v-infinite-scroll="loadMore" :infinite-scroll-disabled="infiniteDisabled" :infinite-scroll-distance="160">
                  <el-empty v-if="postList.length === 0" description="未找到相关帖子，换个关键词试试吧" />

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
              </el-tab-pane>

              <el-tab-pane label="用户" name="users">
                <div class="user-results" v-infinite-scroll="loadMoreUsers" :infinite-scroll-disabled="userInfiniteDisabled">
                  <el-empty v-if="!userLoading && userList.length === 0" description="未找到相关用户" />
                  
                  <div class="user-grid">
                    <div v-for="user in userList" :key="user.id" class="user-card" @click="goToUser(user.id)">
                      <div class="user-card-inner">
                        <el-avatar :size="64" :src="user.avatar || defaultAvatar" class="user-avatar" />
                        <div class="user-info-main">
                          <h4 class="user-nickname">{{ user.nickname || user.username }}</h4>
                          <p class="user-bio">{{ user.bio || '这个用户很懒，什么都没有留下' }}</p>
                          <div class="user-stats">
                            <span><b>{{ user.fansCount || 0 }}</b> 粉丝</span>
                            <span class="dot">·</span>
                            <span><b>{{ user.followCount || 0 }}</b> 关注</span>
                          </div>
                        </div>
                        <div class="user-actions" @click.stop>
                          <el-button 
                            v-if="userStore.userInfo?.id !== user.id"
                            :type="user.isFollowing ? 'info' : 'primary'" 
                            round 
                            size="small"
                            @click="handleFollow(user)"
                          >
                            {{ user.isFollowing ? '已关注' : '关注' }}
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="load-more" v-if="userList.length > 0">
                    <span v-if="userLoadingMore">加载中...</span>
                    <span v-else-if="!userHasMore">没有更多了</span>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
        <el-backtop :right="24" :bottom="40" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, View, ChatLineRound } from '@element-plus/icons-vue'
import { getPostList, type PostVO } from '@/api/post'
import { searchUsers, type UserVO } from '@/api/user'
import { toggleFollow, getFollowStatus } from '@/api/follow'
import { useUserStore } from '@/store/userStore'
import PostListSkeleton from '@/components/PostListSkeleton.vue'
import { ElMessage } from 'element-plus'
import 'element-plus/theme-chalk/display.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref((route.query.tab as string) || 'posts')
const loading = ref(false)
const postList = ref<PostVO[]>([])
const total = ref(0)
const loadingMore = ref(false)
const loadMoreError = ref(false)

const userList = ref<(UserVO & { isFollowing?: boolean })[]>([])
const userTotal = ref(0)
const userLoading = ref(false)
const userLoadingMore = ref(false)
const userPageNum = ref(1)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const activeSort = ref((route.query.sortField as string) || 'createTime')
const skipRouteWatch = ref(false)

const query = reactive({
  pageNum: Number(route.query.pageNum) || 1,
  pageSize: 10,
  keyword: (route.query.keyword as string) || '',
  sortField: activeSort.value,
  sortOrder: 'desc'
})

const handleTabChange = (tab: any) => {
  router.replace({
    query: { ...route.query, tab }
  })
  if (tab === 'users' && userList.value.length === 0) {
    userPageNum.value = 1
    fetchUsers()
  }
}

const fetchUsers = async (append: boolean = false) => {
  if (!query.keyword) return
  userLoading.value = true
  try {
    const res: any = await searchUsers({
      pageNum: userPageNum.value,
      pageSize: 12,
      keyword: query.keyword
    })
    if (res.code === 0 || res.code === 200) {
      const records = res.data.records
      
      // 并行检查关注状态
      if (userStore.token) {
        await Promise.all(records.map(async (u: any) => {
          const statusRes: any = await getFollowStatus(u.id)
          u.isFollowing = statusRes.data
        }))
      }

      userList.value = append ? userList.value.concat(records) : records
      userTotal.value = res.data.total
    }
  } finally {
    userLoading.value = false
  }
}

const handleFollow = async (user: any) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const res: any = await toggleFollow(user.id)
    if (res.code === 0 || res.code === 200) {
      user.isFollowing = res.data
      if (user.isFollowing) {
        user.fansCount = (user.fansCount || 0) + 1
        ElMessage.success('关注成功')
      } else {
        user.fansCount = Math.max(0, (user.fansCount || 0) - 1)
        ElMessage.success('取消关注')
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const userHasMore = computed(() => userList.value.length < userTotal.value)
const userInfiniteDisabled = computed(() => userLoading.value || userLoadingMore.value || !userHasMore.value)

const loadMoreUsers = async () => {
  if (userInfiniteDisabled.value) return
  userLoadingMore.value = true
  try {
    userPageNum.value += 1
    await fetchUsers(true)
  } finally {
    userLoadingMore.value = false
  }
}
 
const getRandomCacheKey = () => `random_cache_search_${query.keyword || ''}`
const saveRandomCache = () => {
  if (activeSort.value !== 'random') return
  const t = typeof route.query.t === 'string' ? route.query.t : String(Date.now())
  sessionStorage.setItem(getRandomCacheKey(), JSON.stringify({
    t,
    pageNum: query.pageNum,
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
    query.sortField = 'random'
    query.pageNum = Number(data.pageNum) || 1
    total.value = Number(data.total) || 0
    postList.value = data.postList
    skipRouteWatch.value = true
    router.replace({ path: '/search', query: { keyword: query.keyword || undefined, sortField: 'random', pageNum: query.pageNum, t: data.t } }).finally(() => {
      skipRouteWatch.value = false
    })
    const y = Number(data.scrollY) || 0
    requestAnimationFrame(() => window.scrollTo({ top: y, behavior: 'auto' }))
    return true
  } catch (e) {
    return false
  }
}

const fetchPosts = async (append: boolean = false) => {
  loading.value = true
  try {
    const res: any = await getPostList({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword,
      sortField: query.sortField,
      sortOrder: query.sortOrder
    })
    if (res.code === 0 || res.code === 200) {
      postList.value = append ? postList.value.concat(res.data.records) : res.data.records
      total.value = res.data.total
      
      // ✅ 动态修改浏览器标题
      if (query.keyword) {
        document.title = `搜索: ${query.keyword} - 知识星球`
      } else {
        document.title = `全部帖子 - 知识星球`
      }

      if (query.sortField === 'random') {
        saveRandomCache()
      }
    }
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
    query.pageNum += 1
    await fetchPosts(true)
  } catch (e) {
    query.pageNum -= 1
    loadMoreError.value = true
  } finally {
    loadingMore.value = false
  }
}
 
const retryLoadMore = () => {
  loadMore()
}

const getScrollKey = () => `scroll_search_${route.fullPath}`
const restoreScroll = () => {
  const raw = sessionStorage.getItem(getScrollKey())
  if (!raw) return
  const y = Number(raw)
  if (!Number.isFinite(y) || y <= 0) return
  requestAnimationFrame(() => window.scrollTo({ top: y, behavior: 'auto' }))
}

const applySearch = () => {
  query.pageNum = 1
  userPageNum.value = 1
  postList.value = []
  userList.value = []
  sessionStorage.removeItem(getScrollKey())
  router.replace({
    path: '/search',
    query: {
      ...route.query,
      keyword: query.keyword || undefined,
      pageNum: 1
    }
  })
  if (activeTab.value === 'posts') fetchPosts()
  else fetchUsers()
}

const resetAll = () => {
  query.keyword = ''
  activeSort.value = 'createTime'
  query.sortField = 'createTime'
  applySearch()
}

const handleSortChange = (sortField: string) => {
  if (activeSort.value === sortField) {
    if (restoreRandomCache()) return
    activeSort.value = 'random'
    query.sortField = 'random'
    query.pageNum = 1
    postList.value = []
    const t = String(Date.now())
    skipRouteWatch.value = true
    router.replace({ path: '/search', query: { ...route.query, sortField: 'random', pageNum: 1, t } }).finally(() => {
      skipRouteWatch.value = false
    })
    sessionStorage.removeItem(getScrollKey())
    fetchPosts()
    return
  }
  if (activeSort.value === 'random') {
    saveRandomCache()
  }
  activeSort.value = sortField
  query.sortField = sortField
  applySearch()
}

const goToDetail = (id: number) => {
  sessionStorage.setItem(getScrollKey(), String(window.scrollY || 0))
  router.push(`/post/${id}`)
}

const goToUser = (id: number) => {
  router.push(`/user/${id}`)
}

const stripMarkdown = (content: string) => {
  if (!content) return ''
  return content
    .replace(/#+\s/g, '')
    .replace(/(\*\*|__)(.*?)\1/g, '$2')
    .replace(/(\*|_)(.*?)\1/g, '$2')
    .replace(/`{1,3}(.*?)`{1,3}/g, '$1')
    .replace(/\[(.*?)\]\(.*?\)/g, '$1')
    .replace(/>\s+/g, '')
    .replace(/<[^>]*>?/gm, '')
    .replace(/\n+/g, ' ')
    .trim()
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

watch(() => route.query, () => {
  if (skipRouteWatch.value) return
  query.pageNum = Number(route.query.pageNum) || 1
  query.keyword = (route.query.keyword as string) || ''
  activeSort.value = (route.query.sortField as string) || 'createTime'
  activeTab.value = (route.query.tab as string) || 'posts'
  query.sortField = activeSort.value
  
  if (activeTab.value === 'posts') {
    postList.value = []
    fetchPosts()
  } else {
    userList.value = []
    userPageNum.value = 1
    fetchUsers()
  }
  sessionStorage.removeItem(getScrollKey())
}, { deep: true })

onMounted(async () => {
  if (activeTab.value === 'posts') {
    await fetchPosts()
  } else {
    await fetchUsers()
  }
  restoreScroll()
})
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

.search-tabs-container {
  padding: 0 16px;
  
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  
  :deep(.el-tabs__header) {
    margin-bottom: 0;
    border-bottom: 1px solid #f1f5f9;
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

.post-tags {
  margin-bottom: 10px;
}

.tag-item {
  margin-right: 5px;
  border-radius: 6px;
}

// 用户搜索结果样式
.user-results {
  padding: 20px 0;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
  padding: 0 10px;
}

.user-card {
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    transform: translateY(-2px);
  }

  .user-card-inner {
    display: flex;
    align-items: flex-start;
    gap: 16px;
  }

  .user-avatar {
    flex-shrink: 0;
  }

  .user-info-main {
    flex: 1;
    min-width: 0;
  }

  .user-nickname {
    margin: 0 0 4px 0;
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .user-username {
    margin: 0 0 8px 0;
    font-size: 13px;
    color: #64748b;
  }

  .user-bio {
    margin: 0 0 12px 0;
    font-size: 13px;
    color: #475569;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .user-stats {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #94a3b8;

    b {
      color: #1e293b;
    }

    .dot {
      color: #cbd5e1;
    }
  }

  .user-actions {
    flex-shrink: 0;
  }
}

.load-more {
  text-align: center;
  padding: 20px 0;
  color: #909399;
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

  .user-grid {
    grid-template-columns: 1fr;
  }
}
</style>

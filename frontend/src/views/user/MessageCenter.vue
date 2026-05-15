<template>
    <div class="message-center-container">
      <el-card class="message-layout-card" :body-style="{ padding: '0px', height: '100%' }">
        <div class="message-layout">
          
          <div class="sidebar">
            <div class="sidebar-header">
              <el-icon><Promotion /></el-icon> 消息中心
            </div>
            <ul class="nav-menu">
              <li 
                :class="['nav-item', { active: activeTab === MessageType.CHAT }]" 
                @click="changeTab(MessageType.CHAT)"
              >
                我的私信
                <span class="count-badge" v-if="unreadMap.CHAT > 0">
                  {{ unreadMap.CHAT > 99 ? '99+' : unreadMap.CHAT }}
                </span>
              </li>
              <li 
                :class="['nav-item', { active: activeTab === MessageType.COMMENT }]" 
                @click="changeTab(MessageType.COMMENT)"
              >
                回复我的
                <span class="count-badge" v-if="unreadMap.COMMENT > 0">
                  {{ unreadMap.COMMENT > 99 ? '99+' : unreadMap.COMMENT }}
                </span>
              </li>
              <li 
                :class="['nav-item', { active: activeTab === MessageType.LIKE }]" 
                @click="changeTab(MessageType.LIKE)"
              >
                收到的赞
                <span class="count-badge" v-if="unreadMap.LIKE > 0">
                  {{ unreadMap.LIKE > 99 ? '99+' : unreadMap.LIKE }}
                </span>
              </li>
              <li 
                :class="['nav-item', { active: activeTab === MessageType.FAVORITE }]" 
                @click="changeTab(MessageType.FAVORITE)"
              >
                收藏通知
                <span class="count-badge" v-if="unreadMap.FAVORITE > 0">
                  {{ unreadMap.FAVORITE > 99 ? '99+' : unreadMap.FAVORITE }}
                </span>
              </li>
              <li 
                :class="['nav-item', { active: activeTab === MessageType.SYSTEM }]" 
                @click="changeTab(MessageType.SYSTEM)"
              >
                系统通知
                <span class="count-badge" v-if="unreadMap.SYSTEM > 0 || unreadMap.TOP > 0 || unreadMap.AUDIT > 0">
                  {{ (unreadMap.SYSTEM + unreadMap.TOP + unreadMap.AUDIT) > 99 ? '99+' : (unreadMap.SYSTEM + unreadMap.TOP + unreadMap.AUDIT) }}
                </span>
              </li>
            </ul>
          </div>
  
          <div class="chat-wrapper" v-if="activeTab === 'CHAT'">
             <ChatWindow />
          </div>
  
          <div class="content-area" v-else>
            <div class="content-header">
              <span class="header-title">{{ getTitle() }}</span>
              
              <el-button 
                type="primary" 
                link 
                size="small" 
                @click="handleReadAll" 
                :loading="readingAll"
              >
                <el-icon style="margin-right: 4px"><Brush /></el-icon> 一键已读
              </el-button>
            </div>
  
            <MessageListSkeleton v-if="loading && messageList.length === 0" />
            <div class="message-list" v-else v-infinite-scroll="loadMore" :infinite-scroll-disabled="infiniteDisabled" :infinite-scroll-distance="160">
              <el-empty v-if="messageList.length === 0" description="暂无消息" />
  
              <div 
                v-for="msg in messageList" 
                :key="msg.id" 
                class="message-item"
                :class="{ 'unread': !msg.isRead }"
                @click="handleMessageClick(msg)"
              >
                <template v-if="msg.type === 'COMMENT'">
                  <el-avatar :size="40" :src="msg.fromAvatar || defaultAvatar" class="avatar"></el-avatar>
                  <div class="msg-body">
                    <div class="msg-top">
                      <span class="nickname">{{ msg.fromNickname }}</span>
                      <span class="action-text">回复了你的评论</span>
                    </div>
                    <div class="msg-content">{{ parseCOMMENTMsg(msg.content).text }}</div>
                    <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
                  </div>
                </template>
  
                <template v-if="msg.type === 'SYSTEM' || msg.type === 'LIKE' || msg.type === 'FAVORITE' || msg.type === 'AUDIT' || msg.type === 'TOP'">
                  <div class="system-icon">
                    <el-icon v-if="msg.type === 'LIKE'"><StarFilled /></el-icon>
                    <el-icon v-else-if="msg.type === 'FAVORITE'"><CollectionTag /></el-icon>
                    <el-icon v-else><BellFilled /></el-icon>
                  </div>
                  <div class="msg-body">
                    <div class="msg-top">
                      <span class="system-title">{{ getSystemTitle(msg.type) }}</span>
                    </div>
                    <div class="msg-content">{{ msg.content }}</div>
                    <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
                  </div>
                </template>
                
                <div class="unread-dot" v-if="!msg.isRead"></div>
              </div>
              
              <div class="load-more" v-if="messageList.length > 0">
                <span v-if="loadingMore">加载中...</span>
                <span v-else-if="!hasMore">没有更多了</span>
                <el-button v-else-if="loadMoreError" link type="danger" @click="retryLoadMore">加载失败，点击重试</el-button>
              </div>
            </div>
          </div>
  
        </div>
      </el-card>
    </div>
    <el-backtop :right="24" :bottom="40" />
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, reactive, onUnmounted, watch, computed } from 'vue'
  import { Promotion, BellFilled, Brush, StarFilled, CollectionTag } from '@element-plus/icons-vue'
  import { 
    getMessageList, 
    markMessageRead, 
    markAllRead, 
    getUnreadDetails, // ✅ 引入新接口
    MessageType,
    type MessageVO
  } from '@/api/message'
  import { useUserStore } from '@/store/userStore'
  import { useRoute, useRouter } from 'vue-router'
  import ChatWindow from './components/ChatWindow.vue'
  import { ElMessage } from 'element-plus'
  import MessageListSkeleton from '@/components/MessageListSkeleton.vue'
  
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const loading = ref(false)
  const readingAll = ref(false)
  type UnreadTab = MessageType
  const activeTab = ref<UnreadTab>(MessageType.CHAT) 
  const messageList = ref<MessageVO[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const loadingMore = ref(false)
  const loadMoreError = ref(false)
  const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  
  // 轮询定时器
  let statsTimer: any = null
  
  // 这里的 key 对应前端的 TAB 名称，值对应数量
  const unreadMap = reactive<Record<string, number>>({
    CHAT: 0, 
    COMMENT: 0, 
    SYSTEM: 0,
    LIKE: 0,
    FAVORITE: 0,
    AUDIT: 0,
    TOP: 0
  })
  
  const parseCOMMENTMsg = (content: string) => {
    const regex = /^POST:(\d+):(.*)$/
    const match = content.match(regex)
    if (match) return { postId: match[1], text: match[2] }
    return { postId: null, text: content }
  }
  
  const changeTab = (tab: UnreadTab) => {
    activeTab.value = tab
    if (tab !== 'CHAT') {
      pageNum.value = 1
      messageList.value = []
      total.value = 0
      fetchMessages(false)
    }
  }
  
  const applyTabFromRoute = () => {
    const tab = route.query.tab
    if (tab === MessageType.CHAT || tab === MessageType.COMMENT || tab === MessageType.SYSTEM) {
      changeTab(tab)
    }
  }
  
  const getTitle = () => {
    const map: Record<string, string> = {
      CHAT: '我的私信', 
      COMMENT: '回复我的', 
      SYSTEM: '系统通知',
      LIKE: '收到的赞',
      FAVORITE: '收藏通知',
      AUDIT: '审核通知',
      TOP: '置顶通知'
    }
    return map[activeTab.value]
  }

  const getSystemTitle = (type: string) => {
    if (type === 'LIKE') return '点赞通知'
    if (type === 'FAVORITE') return '收藏通知'
    if (type === 'AUDIT') return '审核通知'
    if (type === 'TOP') return '置顶通知'
    return '系统通知'
  }
  
  // ✅ 修改：使用后端新接口直接获取统计
  const fetchUnreadStats = async () => {
    try {
      const res: any = await getUnreadDetails()
      if (res.code === 0 || res.code === 200) {
        const data = res.data
        // 将后端字段映射到前端的 Tab Key
        unreadMap.CHAT = data.chat
        unreadMap.COMMENT = data.comment
        unreadMap.SYSTEM = data.system
        unreadMap.LIKE = data.like
        unreadMap.FAVORITE = data.favorite
        unreadMap.AUDIT = data.audit
        unreadMap.TOP = data.top
      }
    } catch (e) {
      console.error('获取未读数失败', e)
    }
  }
  
  const fetchMessages = async (append: boolean) => {
    if (activeTab.value === 'CHAT') return
    loading.value = true
    try {
      const res: any = await getMessageList({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        type: activeTab.value
      })
      if (res.code === 0 || res.code === 200) {
        messageList.value = append ? messageList.value.concat(res.data.records) : res.data.records
        total.value = res.data.total
        
        // 注意：这里不再需要手动计算当前页未读数了，
        // 因为轮询 fetchUnreadStats 会自动更新侧边栏红点
      }
    } finally {
      loading.value = false
    }
  }
  
  const hasMore = computed(() => messageList.value.length < total.value)
  const infiniteDisabled = computed(() => loading.value || loadingMore.value || !hasMore.value)
  
  const loadMore = async () => {
    if (infiniteDisabled.value) return
    loadingMore.value = true
    loadMoreError.value = false
    try {
      pageNum.value += 1
      await fetchMessages(true)
    } catch (e) {
      pageNum.value -= 1
      loadMoreError.value = true
    } finally {
      loadingMore.value = false
    }
  }
  
  const retryLoadMore = () => {
    loadMore()
  }
  
  const handleReadAll = async () => {
    readingAll.value = true
    try {
      const res: any = await markAllRead()
      if (res.code === 0 || res.code === 200) {
        ElMessage({
          message: '已全部标记为已读',
          type: 'success',
          duration: 1000
        })
        pageNum.value = 1
        messageList.value = []
        fetchMessages(false)
        // 立即刷新一下侧边栏统计
        fetchUnreadStats()
      }
    } catch (error) {
      console.error(error)
    } finally {
      readingAll.value = false
    }
  }
  
  const handleMessageClick = async (msg: MessageVO) => {
    if (!msg.isRead) {
      msg.isRead = true
      // 本地扣减未读数 (提升体验，不用等轮询)
      const key = msg.type as UnreadTab
      if (unreadMap[key] > 0) unreadMap[key]--
      
      try { await markMessageRead(msg.id) } catch (e) { console.error(e) }
    }
    if (msg.type === 'COMMENT') {
      const { postId } = parseCOMMENTMsg(msg.content)
      if (postId) router.push(`/post/${postId}`)
    }
  }
  
  const formatTime = (time: string) => {
    if (!time) return ''
    return time.replace('T', ' ').substring(0, 16)
  }
  
  onMounted(() => {
    // 初始化加载
    fetchUnreadStats()
    applyTabFromRoute()
    if (activeTab.value !== 'CHAT') fetchMessages(false)
    
    // 每1.5秒轮询一次未读数状态
    statsTimer = setInterval(fetchUnreadStats, 1500)
  })

  watch(() => route.query.tab, () => {
    applyTabFromRoute()
  })
  
  onUnmounted(() => {
    if (statsTimer) clearInterval(statsTimer)
  })
  </script>
  
  <style scoped lang="scss">
  .message-center-container {
    max-width: 1000px; margin: 20px auto; height: 600px;
  }
  .message-layout-card {
    height: 100%; display: flex; flex-direction: column;
  }
  .message-layout {
    display: flex; height: 100%;
    
    .sidebar {
      width: 200px; border-right: 1px solid #f0f0f0; background-color: #fafafa; display: flex; flex-direction: column;
      .sidebar-header {
        height: 60px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;
        font-size: 16px; font-weight: 600; color: #333; gap: 8px; border-bottom: 1px solid #eee;
      }
      .nav-menu {
        list-style: none; padding: 10px 0; margin: 0; flex: 1;
        .nav-item {
          padding: 15px 20px; cursor: pointer; color: #666; font-size: 14px; position: relative; transition: all 0.3s;
          display: flex; align-items: center; justify-content: space-between; 
          
          &:hover { background-color: #f0f2f5; color: #409eff; }
          &.active { color: #409eff; background-color: #e6f7ff; border-right: 3px solid #409eff; }
          
          .count-badge {
            background-color: #f56c6c;
            color: #fff;
            font-size: 12px;
            min-width: 18px;
            height: 18px;
            line-height: 18px;
            text-align: center;
            padding: 0 5px;
            border-radius: 10px;
            font-weight: bold;
            transform: scale(0.9); 
          }
        }
      }
    }
    
    .chat-wrapper {
      flex: 1; height: 100%; overflow: hidden;
      :deep(.chat-window) { height: 100%; border: none; border-radius: 0; }
    }
  
    .content-area {
      flex: 1; display: flex; flex-direction: column; height: 100%; overflow: hidden;
      .content-header {
        height: 60px; flex-shrink: 0; line-height: 60px; padding: 0 20px;
        border-bottom: 1px solid #eee;
        display: flex; justify-content: space-between; align-items: center;
        .header-title { font-size: 16px; font-weight: 500; color: #333; }
      }
      .message-list {
        padding: 0 20px; flex: 1; overflow-y: auto;
        .message-item {
          padding: 20px 0; border-bottom: 1px solid #f5f5f5; display: flex; gap: 15px; position: relative; cursor: pointer; transition: background 0.2s;
          &:hover { background-color: #fafafa; }
          &.unread { background-color: #fcfcfc; }
          .avatar { flex-shrink: 0; }
          .system-icon {
            width: 40px; height: 40px; border-radius: 50%; background-color: #edf2fc;
            color: #409eff; display: flex; align-items: center; justify-content: center; font-size: 20px;
          }
          .msg-body {
            flex: 1;
            .msg-top { display: flex; align-items: center; margin-bottom: 8px; gap: 10px; .nickname { font-weight: 600; font-size: 14px; color: #333; } .action-text { color: #999; font-size: 13px; } .system-title { font-weight: 600; color: #333; } }
            .msg-content { font-size: 14px; color: #666; line-height: 1.5; }
            .msg-time { margin-top: 8px; font-size: 12px; color: #ccc; }
          }
          .unread-dot { position: absolute; top: 20px; right: 0; width: 8px; height: 8px; background: #f56c6c; border-radius: 50%; }
        }
      }

      .load-more {
        padding: 16px 0 6px;
        text-align: center;
        font-size: 12px;
        color: #94a3b8;
      }
    }
  }
  
  </style>

<template>
    <div class="app-wrapper">
      <header class="app-header">
        <div class="header-content">
          <div class="logo" @click="handleLogoClick">
            <el-icon class="logo-icon"><School /></el-icon>
            <span>知识星球</span>
          </div>
          <div class="user-actions">
            <el-button type="primary" round :icon="EditPen" @click="handlePublish">发布</el-button>

            <div class="nav-btn" @click="goToChat">
              <el-badge :value="unreadChat" :max="99" :hidden="unreadChat === 0" class="badge">
                <el-icon :size="20"><ChatDotRound /></el-icon>
              </el-badge>
              <span class="nav-text">消息</span>
            </div>

            <div class="nav-btn" @click="goToSystemNotice">
              <el-badge :value="unreadSystem" :max="99" :hidden="unreadSystem === 0" class="badge">
                <el-icon :size="20"><Bell /></el-icon>
              </el-badge>
              <span class="nav-text">通知</span>
            </div>

            <div class="nav-btn" @click="goToCreatorCenter">
              <el-icon :size="20"><Document /></el-icon>
              <span class="nav-text">创作中心</span>
            </div>

            <div class="nav-btn" @click="goToFavorites">
              <el-icon :size="20"><Star /></el-icon>
              <span class="nav-text">收藏</span>
            </div>
            
            <el-dropdown class="user-dropdown" @command="handleCommand">
              <span class="el-dropdown-link">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                <span class="username">{{ userStore.userInfo?.nickname || '同学' }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item 
                        v-if="userStore.userInfo?.role === 'MODERATOR'" 
                        command="moderator" 
                        divided
                    >
                        板主工作台
                    </el-dropdown-item>
                  <el-dropdown-item 
                        v-if="userStore.userInfo?.role === 'ADMIN'" 
                        command="admin" 
                        divided
                    >
                        后台管理
                    </el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </header>
  
      <main class="app-main">
        <router-view />
      </main>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, onUnmounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { useUserStore } from '@/store/userStore'
  import { School, EditPen, ArrowDown, Bell, Star, ChatDotRound, Document } from '@element-plus/icons-vue'
  import { getUnreadDetails } from '@/api/message'
  const router = useRouter()
  const userStore = useUserStore()

  // --- 消息轮询逻辑 ---
    const unreadChat = ref(0)
    const unreadSystem = ref(0)
    let pollTimer: any = null

    const fetchUnreadCount = async () => {
    // 只有登录状态下才轮询
    if (!userStore.token) return
    try {
        const res: any = await getUnreadDetails()
        if (res.code === 0 || res.code === 200) {
            const data = res.data
            unreadChat.value = data?.chat || 0
            // 通知红点 = 评论 + 点赞 + 收藏 + 系统 + 审核 + 置顶
            unreadSystem.value = (data?.comment || 0) + (data?.like || 0) + 
                                (data?.favorite || 0) + (data?.system || 0) + 
                                (data?.audit || 0) + (data?.top || 0)
        }
    } catch (error) {
        console.error('获取未读数失败', error)
    }
    }
 
    const goToChat = () => {
        if (!userStore.token) return router.push('/login')
        router.push({ path: '/message', query: { tab: 'CHAT' } })
    }
 
    const goToSystemNotice = () => {
        if (!userStore.token) return router.push('/login')
        router.push({ path: '/message', query: { tab: 'SYSTEM' } })
    }
 
    const goToFavorites = () => {
        if (!userStore.token) return router.push('/login')
        const rawId = userStore.userInfo?.id ?? userStore.userInfo?.username
        router.push({ path: `/user/${rawId}`, query: { tab: 'favorites' } })
    }
 
    const goToCreatorCenter = () => {
        if (!userStore.token) return router.push('/login')
        const rawId = userStore.userInfo?.id ?? userStore.userInfo?.username
        router.push({ path: `/user/${rawId}`, query: { tab: 'posts' } })
    }

    // 组件挂载开始轮询
    onMounted(() => {
    fetchUnreadCount() // 立即执行一次
        pollTimer = setInterval(fetchUnreadCount, 1500) // 每1.5秒轮询，提升响应速度
    })

    // 组件卸载清除定时器
    onUnmounted(() => {
        if (pollTimer) clearInterval(pollTimer)
    })
  
  const handlePublish = () => {
    router.push('/post/create')
  }

  const handleLogoClick = () => {
    // 跳转到首页，并带上随机排序参数，重置板块ID
    router.push({
      path: '/',
      query: { 
        sortField: 'random',
        t: Date.now() // 加上时间戳强制刷新
      }
    })
  }
  
  const handleCommand = (command: string) => {
    if (command === 'logout') {
      userStore.logout()
    } else if (command === 'profile') {
      const rawId = userStore.userInfo?.id ?? userStore.userInfo?.username
      router.push(`/user/${rawId}`)
    } else if (command === 'admin') {
        // 跳转到管理页
        router.push('/admin')
    } else if(command === 'moderator') router.push('/moderator')
  }
  </script>
  
  <style scoped lang="scss">
  .app-wrapper {
    min-height: 100vh;
    background-color: #f5f7fa; // 浅灰背景，护眼
    overflow-x: hidden;
  }
  
  .app-header {
    background-color: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    position: sticky;
    top: 0;
    z-index: 100;
    
    .header-content {
      max-width: 1200px;
      margin: 0 auto;
      height: 60px;
      display: flex;
      align-items: center;
      padding: 0 20px;
      
      .logo {
        display: flex;
        align-items: center;
        font-size: 20px;
        font-weight: bold;
        color: #409eff;
        cursor: pointer;
        gap: 8px;
      }
      
      .user-actions {
        margin-left: auto;
        display: flex;
        align-items: center;
        gap: 14px;
        
        .user-dropdown {
          cursor: pointer;
          padding: 4px 12px;
          border-radius: 10px;
          transition: background-color 0.3s;
          
          &:hover {
            background-color: #f1f5f9;
          }
          
          .el-dropdown-link {
            display: flex;
            align-items: center;
            gap: 8px;
          }
        }
        
        .nav-btn {
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 6px;
          color: #606266;
          transition: all 0.3s;
          padding: 8px 12px;
          border-radius: 10px;
          user-select: none;
 
          &:hover {
            background: #f1f5f9;
            color: #409eff;
          }
 
          .nav-text {
            font-size: 14px;
          }
        }

        .badge {
          display: inline-flex;
          align-items: center;
        }
      }
    }
  }
  
  .app-main {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }
  </style>

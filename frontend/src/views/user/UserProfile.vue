  <template>
    <div class="user-profile-container" v-loading="loading">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card class="info-card" shadow="never">
            <div class="profile-banner">
              <div class="profile-header">
                <div class="avatar-section">
                  <el-avatar :size="88" :src="userInfo.avatar || defaultAvatar" />
                </div>
                <div class="info-section">
                  <h2 class="username">{{ userInfo.nickname || userInfo.username }}</h2>
                  <div class="meta-row">
                    <el-tag effect="dark" size="small" type="success">{{ getRoleName(userInfo.role) }}</el-tag>
                    <span class="meta-split">·</span>
                    <span class="bio">加入时间: {{ formatTime(userInfo.createTime) }}</span>
                    <span class="meta-split">·</span>
                    <span class="bio cursor-pointer" @click="handleTabChange('fans')">粉丝: {{ userInfo.fansCount || 0 }}</span>
                    <span class="meta-split">·</span>
                    <span class="bio cursor-pointer" @click="handleTabChange('followees')">关注: {{ userInfo.followCount || 0 }}</span>
                  </div>
                  <div class="intro-text">个人简介：{{ userInfo.bio || '这个用户很懒，没有介绍自己...' }}</div>
                </div>
                
                <div class="action-section">
                  <template v-if="isOwner">
                    <el-button type="primary" :icon="Edit" @click="editDialogVisible = true">
                      编辑资料
                    </el-button>
                    <el-button type="warning" plain :icon="Lock" @click="passwordDialogVisible = true">
                      修改密码
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button type="primary" :icon="ChatDotRound" @click="handleGoToChat">
                      发送消息
                    </el-button>
                    <el-button type="primary" plain :loading="followLoading" @click="handleToggleFollow">
                      {{ isFollowing ? '已关注' : '关注' }}
                    </el-button>
                  </template>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
  
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>
              <div class="posts-header">
                <div class="posts-header-left">
                  <span class="posts-icon">📚</span>
                  <span class="posts-title">内容</span>
                </div>
                <div class="posts-header-right" v-if="isOwner">
                  <el-dropdown trigger="click" @command="handleTabChange">
                    <span class="el-dropdown-link">
                      <span class="active-tab-text">{{ activeTabName }}</span>
                      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </span>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="posts">我的帖子</el-dropdown-item>
                        <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                        <el-dropdown-item command="liked">我赞过的</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </template>
            
            <div class="post-list">
              <PostListSkeleton v-if="loading && ((mainTab === 'posts' && postList.length === 0) || (mainTab === 'favorites' && favoriteList.length === 0))" />
              <div v-else class="list-body" v-infinite-scroll="loadMoreProfile" :infinite-scroll-disabled="infiniteDisabled" :infinite-scroll-distance="160">
              <template v-if="mainTab === 'posts'">
              <el-empty v-if="postList.length === 0" description="暂无发布记录" />
              
              <div v-for="post in postList" :key="post.id" class="post-item">
                <div class="post-main" @click="goToPostDetail(post.id)">
                  <h3 class="title">
                    <template v-if="isOwner">
                      <el-tooltip v-if="post.status === 2 && post.rejectReason" :content="post.rejectReason" placement="top">
                        <el-tag :type="getStatusType(post.status)" size="small" style="margin-right: 8px">
                          {{ getStatusName(post.status) }}
                        </el-tag>
                      </el-tooltip>
                      <el-tag v-else :type="getStatusType(post.status)" size="small" style="margin-right: 8px">
                        {{ getStatusName(post.status) }}
                      </el-tag>
                    </template>
                    {{ post.title }}
                  </h3>
                  <p class="summary">{{ getPostSummary(post.content) }}</p>
                  <div class="meta">
                     <span>{{ formatTime(post.createTime) }}</span>
                     <span class="divider">|</span>
                     <span>{{ post.viewCount }} 阅读</span>
                     <span class="divider">|</span>
                     <span>{{ post.replyCount }} 评论</span>
                  </div>
                </div>
                <div v-if="isOwner" class="post-actions">
                  <el-button link type="primary" size="small" :icon="Edit" @click="handleEditPost(post.id)">编辑</el-button>
                  <el-button link type="danger" size="small" :icon="Delete" @click="handleDeletePost(post.id)">删除</el-button>
                </div>
              </div>
              </template>
  
              <template v-else-if="mainTab === 'favorites'">
              <el-empty v-if="favoriteList.length === 0" description="暂无收藏记录" />
              
              <div v-for="post in favoriteList" :key="post.id" class="post-item">
                <div class="post-main" @click="goToPostDetail(post.id)">
                  <h3 class="title">{{ post.title }}</h3>
                  <p class="summary">{{ getPostSummary(post.content) }}</p>
                  <div class="meta">
                    <el-tag size="small" type="info" style="margin-right: 8px">{{ post.boardName }}</el-tag>
                    <span>{{ post.authorName }}</span>
                    <span class="divider">|</span>
                    <span>{{ post.viewCount }} 阅读</span>
                  </div>
                </div>
              </div>
              </template>

              <template v-else-if="mainTab === 'liked'">
              <el-empty v-if="likedList.length === 0" description="暂无点赞记录" />
              
              <div v-for="post in likedList" :key="post.id" class="post-item">
                <div class="post-main" @click="goToPostDetail(post.id)">
                  <h3 class="title">{{ post.title }}</h3>
                  <p class="summary">{{ getPostSummary(post.content) }}</p>
                  <div class="meta">
                    <el-tag size="small" type="info" style="margin-right: 8px">{{ post.boardName }}</el-tag>
                    <span>{{ post.authorName }}</span>
                    <span class="divider">|</span>
                    <span>{{ post.viewCount }} 阅读</span>
                    <span class="divider">|</span>
                    <span>{{ post.replyCount }} 评论</span>
                  </div>
                </div>
              </div>
              </template>

              <template v-else-if="mainTab === 'followees' || mainTab === 'fans'">
              <el-empty v-if="userList.length === 0" :description="mainTab === 'fans' ? '暂无粉丝' : '暂无关注'" />
              
              <div class="user-list-grid">
                <div v-for="user in userList" :key="user.id" class="user-card" @click="goToUserDetail(user.id)">
                  <el-avatar :size="50" :src="user.avatar || defaultAvatar" />
                  <div class="user-info">
                    <div class="user-nickname">{{ user.nickname || user.username }}</div>
                    <div class="user-role">{{ getRoleName(user.role) }}</div>
                  </div>
                  <el-button size="small" type="primary" plain @click.stop="goToUserDetail(user.id)">查看主页</el-button>
                </div>
              </div>
              </template>
              
              <div class="load-more" v-if="currentListLength > 0">
                <span v-if="loadingMore">加载中...</span>
                <span v-else-if="!hasMore">没有更多了</span>
                <el-button v-else-if="loadMoreError" link type="danger" @click="retryLoadMore">加载失败，点击重试</el-button>
              </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-backtop :right="24" :bottom="40" />
  
      <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="500px">
        <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="editForm.nickname" />
          </el-form-item>
          <el-form-item label="头像">
            <div class="avatar-upload">
              <el-avatar :size="60" :src="editForm.avatar || defaultAvatar" class="preview-avatar" />
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <el-button size="small" type="primary" style="margin-left: 10px">点击上传</el-button>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="editForm.email" placeholder="请输入邮箱地址" />
          </el-form-item>
          <el-form-item label="简介" prop="bio">
            <el-input
              v-model="editForm.bio"
              type="textarea"
              :rows="4"
              maxlength="255"
              show-word-limit
              placeholder="介绍一下自己吧"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleUpdateProfile" :loading="updating">保存</el-button>
          </span>
        </template>
      </el-dialog>
  
      <el-dialog v-model="passwordDialogVisible" title="修改密码" width="450px">
        <el-form 
          ref="passwordFormRef" 
          :model="passwordForm" 
          :rules="passwordRules" 
          label-width="100px"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="passwordDialogVisible = false">取消</el-button>
            <el-button type="danger" @click="handleSubmitPassword" :loading="passwordSubmitting">确认修改</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, computed, reactive, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { useUserStore } from '@/store/userStore'
  import { getUserInfo, updateUserInfo, updatePassword, getLikedPosts, type UserVO } from '@/api/user'
  import { getPostList, deletePost, type PostVO } from '@/api/post'
  import { getFavoriteList } from '@/api/favorite'
  import { uploadImage } from '@/api/upload'
  import { getFollowStatus, toggleFollow, getFansList, getFolloweeList } from '@/api/follow'
  import { Edit, Delete, Lock, ArrowDown, ChatDotRound } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
  import PostListSkeleton from '@/components/PostListSkeleton.vue'
  import 'element-plus/theme-chalk/display.css'
  
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  
  const loading = ref(false)
  const userId = ref<number>(0)
  const userInfo = ref<Partial<UserVO>>({})
  const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  type ProfileTab = 'posts' | 'favorites' | 'liked' | 'followees' | 'fans'
  
  const postList = ref<PostVO[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const mainTab = ref<ProfileTab>('posts')
  const sanitizeProfileTab = (tab: string | null | undefined): ProfileTab => {
    const normalizedTab = (tab || 'posts') as ProfileTab
    const allowedTabs: ProfileTab[] = isOwner.value
      ? ['posts', 'favorites', 'liked', 'followees', 'fans']
      : ['posts', 'followees', 'fans']
    return allowedTabs.includes(normalizedTab) ? normalizedTab : 'posts'
  }
  
  const currentTab = computed(() => {
    return sanitizeProfileTab(route.query.tab as string | undefined)
  })

  const favoriteList = ref<PostVO[]>([])
  const favoriteTotal = ref(0)
  const favoritePageNum = ref(1)
  
  const likedList = ref<PostVO[]>([])
  const likedTotal = ref(0)
  const likedPageNum = ref(1)
  const likedLoading = ref(false)

  const userList = ref<UserVO[]>([])
  const userTotal = ref(0)
  const userPageNum = ref(1)
  const userLoading = ref(false)

  const activeTabName = computed(() => {
    if (mainTab.value === 'posts') return '我的帖子'
    if (mainTab.value === 'favorites') return '我的收藏'
    if (mainTab.value === 'liked') return '我赞过的'
    if (mainTab.value === 'followees') return '关注的人'
    if (mainTab.value === 'fans') return '我的粉丝'
    return '内容'
  })

  const favoritePageSize = ref(10)
  const loadingMore = ref(false)
  const loadMoreError = ref(false)
  const isFollowing = ref(false)
  const followLoading = ref(false)
  
  // 编辑资料相关
  const editDialogVisible = ref(false)
  const updating = ref(false)
  const editFormRef = ref<FormInstance>()
  const editForm = reactive({
    nickname: '',
    avatar: '',
    email: '',
    bio: ''
  })
  
  const editRules = reactive<FormRules>({
    nickname: [
      { required: true, message: '请输入昵称', trigger: 'blur' },
      { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
    ],
    email: [
      { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] },
      { 
        pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, 
        message: '邮箱格式不正确，必须包含合法的后缀（如 .com, .cn 等）', 
        trigger: 'blur' 
      }
    ],
    bio: [
      { max: 255, message: '个人简介不能超过255个字符', trigger: 'blur' }
    ]
  })
  
  // ✅ 修改密码相关状态
  const passwordDialogVisible = ref(false)
  const passwordSubmitting = ref(false)
  const passwordFormRef = ref<FormInstance>()
  const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
  
  // 验证两次密码是否一致
  const validatePass2 = (rule: any, value: any, callback: any) => {
    if (value === '') {
      callback(new Error('请再次输入密码'))
    } else if (value !== passwordForm.newPassword) {
      callback(new Error('两次输入密码不一致!'))
    } else {
      callback()
    }
  }
  
  const passwordRules = reactive<FormRules>({
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, validator: validatePass2, trigger: 'blur' }
    ]
  })
  
  const isOwner = computed(() => {
    return userStore.userInfo?.id === userId.value
  })

  const getStatusName = (status: number | undefined) => {
    if (status === 0) return '待审核'
    if (status === 1) return '已发布'
    if (status === 2) return '已拒绝'
    return '未知'
  }

  const getStatusNameByCode = (status: number | undefined) => {
    return getStatusName(status)
  }

  const getStatusType = (status: number | undefined) => {
    if (status === 0) return 'info'
    if (status === 1) return 'success'
    if (status === 2) return 'danger'
    return ''
  }

  const getRoleName = (role: string | undefined) => {
    if (role === 'ADMIN') return '管理员'
    if (role === 'MODERATOR') return '版主'
    return '普通用户'
  }

  const beforeAvatarUpload = (file: File) => {
    const isImage = file.type.startsWith('image/')
    const isLt5M = file.size / 1024 / 1024 < 5
    if (!isImage) {
      ElMessage.error('只能上传图片文件')
      return false
    }
    if (!isLt5M) {
      ElMessage.error('图片大小不能超过 5MB')
      return false
    }
    return true
  }

  const handleAvatarUpload = async (options: any) => {
    const { file } = options
    try {
      const res: any = await uploadImage(file)
      if (res.code === 0 || res.code === 200) {
        editForm.avatar = res.data
        ElMessage.success('头像上传成功')
      } else {
        ElMessage.error('头像上传失败')
      }
    } catch (error) {
      console.error(error)
      ElMessage.error('头像上传失败')
    }
  }
  
  const initData = async () => {
    userId.value = Number(route.params.id)
    if (!userId.value) return
    
    loading.value = true
    try {
      const userRes: any = await getUserInfo(userId.value)
      if (userRes.code === 0 || userRes.code === 200) {
        userInfo.value = userRes.data
        // ✅ 动态修改浏览器标题
        document.title = `${userInfo.value.nickname || userInfo.value.username} 的主页 - 知识星球`
        editForm.nickname = userRes.data.nickname || ''
        editForm.avatar = userRes.data.avatar || ''
        editForm.email = userRes.data.email || ''
        editForm.bio = userRes.data.bio || ''
      }
      if (!isOwner.value) {
        const followRes: any = await getFollowStatus(userId.value)
        if (followRes.code === 0 || followRes.code === 200) {
          isFollowing.value = !!followRes.data
        }
      }
      
      // 根据当前 URL 初始化数据列表
      await syncTabData()
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  const syncTabData = async () => {
    const tab = currentTab.value
    const routeTab = typeof route.query.tab === 'string' ? route.query.tab : undefined
    const expectedRouteTab = tab === 'posts' ? undefined : tab
    if (routeTab !== expectedRouteTab) {
      await router.replace({
        query: { ...route.query, tab: expectedRouteTab }
      })
      return
    }
    mainTab.value = tab
    
    if (tab === 'posts') {
      pageNum.value = 1
      postList.value = []
      clearScroll()
      await fetchPosts(false)
    } else if (tab === 'favorites') {
      favoritePageNum.value = 1
      favoriteList.value = []
      clearScroll()
      await fetchFavorites(false)
    } else if (tab === 'liked') {
      likedPageNum.value = 1
      likedList.value = []
      clearScroll()
      await fetchLikedPosts(false)
    } else if (tab === 'followees' || tab === 'fans') {
      userPageNum.value = 1
      userList.value = []
      clearScroll()
      await fetchUsers(false)
    }
  }

  const handleToggleFollow = async () => {
    if (!userStore.token) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    followLoading.value = true
    try {
      const res: any = await toggleFollow(userId.value)
      if (res.code === 0 || res.code === 200) {
        isFollowing.value = !!res.data
        if (userInfo.value) {
          const current = Number(userInfo.value.fansCount || 0)
          userInfo.value.fansCount = isFollowing.value ? current + 1 : Math.max(current - 1, 0)
        }
      }
    } finally {
      followLoading.value = false
    }
  }

  const handleGoToChat = () => {
    if (!userStore.token) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    router.push({
      path: '/message',
      query: { 
        tab: 'CHAT',
        chatUserId: userId.value // 改为 chatUserId 以匹配 ChatWindow 的逻辑
      }
    })
  }
  
  const fetchPosts = async (append: boolean) => {
    const postRes: any = await getPostList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      userId: userId.value,
      status: isOwner.value ? -1 : 1
    })
    if (postRes.code === 0 || postRes.code === 200) {
      postList.value = append ? postList.value.concat(postRes.data.records) : postRes.data.records
      total.value = postRes.data.total
    }
  }
 
  const fetchFavorites = async (append: boolean) => {
    const res: any = await getFavoriteList({
      pageNum: favoritePageNum.value,
      pageSize: favoritePageSize.value
    })
    if (res.code === 0 || res.code === 200) {
      favoriteList.value = append ? favoriteList.value.concat(res.data.records) : res.data.records
      favoriteTotal.value = res.data.total
    }
  }

  const fetchLikedPosts = async (append: boolean) => {
    likedLoading.value = true
    try {
      const res: any = await getLikedPosts({
        pageNum: likedPageNum.value,
        pageSize: favoritePageSize.value,
        userId: userId.value
      })
      if (res.code === 0 || res.code === 200) {
        likedList.value = append ? likedList.value.concat(res.data.records) : res.data.records
        likedTotal.value = res.data.total
      }
    } finally {
      likedLoading.value = false
    }
  }

  const fetchUsers = async (append: boolean) => {
    userLoading.value = true
    try {
      const api = mainTab.value === 'fans' ? getFansList : getFolloweeList
      const res: any = await api(userId.value, {
        pageNum: userPageNum.value,
        pageSize: favoritePageSize.value
      })
      if (res.code === 0 || res.code === 200) {
        userList.value = append ? userList.value.concat(res.data.records) : res.data.records
        userTotal.value = res.data.total
      }
    } finally {
      userLoading.value = false
    }
  }
  
  const handleTabChange = (val: string | number | boolean | undefined) => {
    const command = sanitizeProfileTab(String(val || 'posts'))
    
    // 只负责更新 URL
    router.replace({
      query: { ...route.query, tab: command === 'posts' ? undefined : command }
    })
  }
 
  const handleMainTabChange = () => {
    syncTabData()
  }

  const getPostSummary = (content: string | undefined) => {
    if (!content) return ''
    const plainText = content
      .replace(/#+\s/g, '')
      .replace(/(\*\*|__)(.*?)\1/g, '$2')
      .replace(/(\*|_)(.*?)\1/g, '$2')
      .replace(/`{1,3}(.*?)`{1,3}/g, '$1')
      .replace(/\[(.*?)\]\(.*?\)/g, '$1')
      .replace(/>\s+/g, '')
      .replace(/<[^>]*>?/gm, '')
      .replace(/\n+/g, ' ')
      .trim()
    return plainText.length > 100 ? `${plainText.substring(0, 100)}...` : plainText
  }
  
  watch(() => route.params.id, (newId) => {
    if (newId) initData()
  })

  watch(() => route.query.tab, (tab, oldTab) => {
    if (tab === oldTab) return
    syncTabData()
  })
  
  const handleUpdateProfile = async () => {
    if (!editFormRef.value) return
    
    await editFormRef.value.validate(async (valid) => {
      if (valid) {
        updating.value = true
        try {
          const res: any = await updateUserInfo(editForm)
          if (res.code === 0 || res.code === 200) {
            ElMessage.success('保存成功')
            editDialogVisible.value = false
            userInfo.value.nickname = editForm.nickname
            userInfo.value.avatar = editForm.avatar
            userInfo.value.email = editForm.email
            userInfo.value.bio = editForm.bio
            if (isOwner.value) {
              userStore.userInfo.nickname = editForm.nickname
              userStore.userInfo.avatar = editForm.avatar
              localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
            }
          }
        } finally {
          updating.value = false
        }
      }
    })
  }
  
  // ✅ 修改密码提交逻辑
  const handleSubmitPassword = async () => {
    if (!passwordFormRef.value) return
    
    await passwordFormRef.value.validate(async (valid) => {
      if (valid) {
        passwordSubmitting.value = true
        try {
          const res: any = await updatePassword(passwordForm)
          
          if (res.code === 0 || res.code === 200) {
            ElMessage.success('密码修改成功，请重新登录')
            passwordDialogVisible.value = false
            // 强制登出
            userStore.logout()
          }
        } catch (error) {
          console.error(error)
        } finally {
          passwordSubmitting.value = false
        }
      }
    })
  }
  
  const handleDeletePost = (id: number) => {
    ElMessageBox.confirm('确定要删除这篇帖子吗？', '提示', { type: 'warning' })
      .then(async () => {
        const res: any = await deletePost(id)
        if (res.code === 0 || res.code === 200) {
          ElMessage.success('删除成功')
          pageNum.value = 1
          postList.value = []
          clearScroll()
          fetchPosts(false)
        }
      })
  }
  
  const handleEditPost = (id: number) => {
    router.push(`/post/edit/${id}`)
  }
  
  const currentListLength = computed(() => {
    if (mainTab.value === 'posts') return postList.value.length
    if (mainTab.value === 'favorites') return favoriteList.value.length
    if (mainTab.value === 'liked') return likedList.value.length
    if (mainTab.value === 'followees' || mainTab.value === 'fans') return userList.value.length
    return 0
  })
  const hasMore = computed(() => {
    if (mainTab.value === 'posts') return postList.value.length < total.value
    if (mainTab.value === 'favorites') return favoriteList.value.length < favoriteTotal.value
    if (mainTab.value === 'liked') return likedList.value.length < likedTotal.value
    if (mainTab.value === 'followees' || mainTab.value === 'fans') return userList.value.length < userTotal.value
    return false
  })
  const infiniteDisabled = computed(() => loading.value || loadingMore.value || (mainTab.value === 'liked' && likedLoading.value) || (userLoading.value) || !hasMore.value)

  const loadMoreProfile = async () => {
    if (infiniteDisabled.value) return
    loadingMore.value = true
    loadMoreError.value = false
    try {
      if (mainTab.value === 'posts') {
        pageNum.value += 1
        await fetchPosts(true)
      } else if (mainTab.value === 'favorites') {
        favoritePageNum.value += 1
        await fetchFavorites(true)
      } else if (mainTab.value === 'liked') {
        likedPageNum.value += 1
        await fetchLikedPosts(true)
      } else if (mainTab.value === 'followees' || mainTab.value === 'fans') {
        userPageNum.value += 1
        await fetchUsers(true)
      }
    } catch (e) {
      if (mainTab.value === 'posts') pageNum.value -= 1
      else if (mainTab.value === 'favorites') favoritePageNum.value -= 1
      else if (mainTab.value === 'liked') likedPageNum.value -= 1
      else if (mainTab.value === 'followees' || mainTab.value === 'fans') userPageNum.value -= 1
      loadMoreError.value = true
    } finally {
      loadingMore.value = false
    }
  }

  const goToUserDetail = (id: number) => {
    saveScroll()
    router.push(`/user/${id}`)
  }
  
  const retryLoadMore = () => {
    loadMoreProfile()
  }

  const getScrollKey = () => `scroll_user_profile_${route.fullPath}`
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

  const goToPostDetail = (id: number) => {
    saveScroll()
    router.push(`/post/${id}`)
  }
  
  const formatTime = (time: string | undefined) => {
    if (!time) return ''
    return time.replace('T', ' ').substring(0, 10)
  }
  
  onMounted(() => {
    initData()
    restoreScroll()
  })
  </script>
  
  <style scoped lang="scss">
  .user-profile-container {
    max-width: 1200px;
    margin: 20px auto;
  }
  
  .info-card {
    :deep(.el-card__body) {
      padding: 0;
    }

    .profile-banner {
      padding: 18px 18px 20px;
      background: linear-gradient(135deg, #eef6ff 0%, #f7f9ff 50%, #fff 100%);
    }

    .profile-header {
      display: flex;
      align-items: center;
      gap: 18px;
    }

    .avatar-section {
      flex-shrink: 0;
      padding: 6px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.8);
      box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
    }

    .info-section {
      flex: 1;
      min-width: 0;
    }

    .username {
      font-size: 22px;
      margin: 0 0 8px 0;
      font-weight: 700;
      color: #0f172a;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .meta-row {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #64748b;
      font-size: 13px;
    }

    .meta-split {
      opacity: 0.7;
    }

    .cursor-pointer {
      cursor: pointer;
      transition: color 0.2s;
      &:hover {
        color: #409eff;
      }
    }

    .bio {
      margin: 0;
    }

    .intro-text {
      margin-top: 10px;
      color: #475569;
      font-size: 15px;
      font-weight: 500;
      line-height: 1.7;
      word-break: break-word;
    }

    .action-section {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }

  .posts-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .posts-header-left {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  .posts-icon {
    font-size: 18px;
  }

  .posts-title {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
  }

  .posts-header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-shrink: 0;
  }

  .el-dropdown-link {
    cursor: pointer;
    display: flex;
    align-items: center;
    padding: 6px 12px;
    background: #f1f5f9;
    border-radius: 8px;
    color: #475569;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s;
    
    &:hover {
      background: #e2e8f0;
      color: #1e293b;
    }

    .active-tab-text {
      margin-right: 4px;
    }
  }

  .status-select-container {
    width: 200px;
    height: 32px;
    display: flex;
    align-items: center;
  }

  .status-select {
    width: 100%;
  }

  .avatar-upload {
    display: flex;
    align-items: center;

    .preview-avatar {
      border: 1px solid #ddd;
    }
  }
  
  .post-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 14px;
    border: 1px solid #eef2f7;
    border-radius: 10px;
    margin-bottom: 12px;
    background: #fff;
    transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
    
    &:hover {
      border-color: #dbeafe;
      box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
      transform: translateY(-1px);
    }
    
    .post-main {
      flex: 1;
      cursor: pointer;
      &:hover .title { color: #409eff; }
      
      .title { font-size: 16px; font-weight: 700; margin-bottom: 6px; color: #0f172a; }
      .summary { font-size: 13px; color: #666; margin-bottom: 8px; }
      .meta { font-size: 12px; color: #999; .divider { margin: 0 8px; color: #eee; } }
    }
    
    .post-actions {
      margin-left: 20px;
      display: flex;
      gap: 8px;
    }
  }
  
  .user-list-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
    padding: 8px 0;
  }

  .user-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #fff;
    border: 1px solid #eef2f7;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      transform: translateY(-2px);
    }

    .user-info {
      flex: 1;
      min-width: 0;

      .user-nickname {
        font-size: 15px;
        font-weight: 600;
        color: #1e293b;
        margin-bottom: 4px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .user-role {
        font-size: 12px;
        color: #64748b;
      }
    }
  }

  .load-more {
    padding: 16px 0 6px;
    text-align: center;
    font-size: 12px;
    color: #94a3b8;
  }

  @media (max-width: 768px) {
    .posts-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
    }
    .posts-header-right {
      width: 100%;
      justify-content: space-between;
    }
    .status-select {
      width: 160px;
    }
  }
  </style>

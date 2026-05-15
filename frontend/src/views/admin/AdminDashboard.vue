<template>
    <div class="admin-container">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span class="title">🛡️ 管理员控制台</span>
          </div>
        </template>
  
        <el-tabs v-model="activeTab" class="admin-tabs" @tab-change="handleTabChange">
          
          <el-tab-pane label="板块管理" name="board">
            <div class="tab-action-bar">
              <el-button type="primary" :icon="Plus" @click="handleOpenAddBoard">新增板块</el-button>
            </div>
            
            <el-table :data="boardList" style="width: 100%" v-loading="boardLoading" border stripe>
              <el-table-column prop="name" label="板块名称" width="150" font-weight="bold" />
              <el-table-column prop="description" label="描述" />
              <el-table-column prop="moderatorName" label="当前板主" width="120">
                <template #default="scope">
                  <el-tag :type="scope.row.moderatorId ? 'warning' : 'info'" size="small">
                    {{ scope.row.moderatorName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right" align="center">
                <template #default="scope">
                  <el-button type="primary" size="small" plain @click="handleEditBoard(scope.row)">编辑</el-button>
                  <el-button type="danger" size="small" plain @click="handleDeleteBoard(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
  
          <el-tab-pane label="帖子管理" name="post">
             <div class="tab-action-bar">
                <div class="filter-row">
                    <el-input 
                        v-model="postKeyword" 
                        placeholder="搜索帖子标题" 
                        style="width: 300px; margin-right: 15px" 
                        clearable
                        @clear="handlePostSearch"
                        @keyup.enter="handlePostSearch"
                    >
                        <template #append>
                        <el-button :icon="Search" @click="handlePostSearch" />
                        </template>
                    </el-input>
                    <el-select v-model="postStatusFilter" style="width: 160px; margin-right: 15px" @change="handlePostSearch">
                      <el-option label="全部状态" :value="-1" />
                      <el-option label="待审核" :value="0" />
                      <el-option label="已发布" :value="1" />
                      <el-option label="已拒绝" :value="2" />
                    </el-select>
                    <el-alert title="提示：置顶操作会立即生效，删除操作不可恢复。" type="info" show-icon :closable="false" />
                </div>
             </div>
  
             <el-table :data="postList" style="width: 100%" v-loading="postLoading" border>
               <el-table-column label="标题" min-width="300">
                 <template #default="scope">
                    <div style="display: flex; align-items: center; gap: 8px">
                      <el-tag v-if="scope.row.isTop" type="warning" size="small" effect="dark">置顶</el-tag>
                      <el-link type="primary" :href="`/post/${scope.row.id}`" target="_blank">
                         {{ scope.row.title }}
                      </el-link>
                    </div>
                 </template>
               </el-table-column>
               <el-table-column prop="authorName" label="作者" width="120" />
               <el-table-column prop="boardName" label="所属板块" width="120" />
               <el-table-column prop="status" label="状态" width="100" align="center">
                 <template #default="scope">
                   <el-tag size="small" :type="scope.row.status === 1 ? 'success' : (scope.row.status === 0 ? 'info' : 'danger')">
                     {{ scope.row.status === 1 ? '已发布' : (scope.row.status === 0 ? '待审核' : '已拒绝') }}
                   </el-tag>
                 </template>
               </el-table-column>
               <el-table-column prop="createTime" label="发布时间" width="180">
                 <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
               </el-table-column>
               <el-table-column label="管理操作" width="200" fixed="right">
                 <template #default="scope">
                   <template v-if="scope.row.status === 0">
                     <el-button size="small" type="success" plain @click="handleAuditPost(scope.row, 1)" style="margin-right: 10px">通过</el-button>
                     <el-button size="small" type="danger" plain @click="handleAuditPost(scope.row, 2)">拒绝</el-button>
                   </template>
                   <el-button 
                     v-if="scope.row.status === 1"
                     size="small" 
                     type="warning" 
                     plain
                     :icon="Top"
                     @click="handleTopPost(scope.row)"
                   >
                     置顶/取消
                   </el-button>
                   <el-button 
                     v-if="scope.row.status !== 0"
                     size="small" 
                     type="danger" 
                     :icon="Delete"
                     @click="handleDeletePost(scope.row)"
                   >
                     删除
                   </el-button>
                 </template>
               </el-table-column>
             </el-table>
  
             <div class="pagination-box">
               <el-pagination 
                 background 
                 layout="prev, pager, next" 
                 :total="postTotal" 
                 :page-size="postPageSize"
                 @current-change="handlePostPageChange"
               />
             </div>
          </el-tab-pane>
  
          <el-tab-pane label="用户管理" name="user">
             <div class="tab-action-bar">
                <div class="filter-row">
                    <el-input 
                        v-model="userKeyword" 
                        placeholder="搜索用户名/昵称" 
                        style="width: 300px; margin-right: 15px" 
                        clearable
                        @clear="handleUserSearch"
                        @keyup.enter="handleUserSearch"
                    >
                        <template #append>
                          <el-button :icon="Search" @click="handleUserSearch" />
                        </template>
                    </el-input>
                    <el-select v-model="userRoleFilter" style="width: 160px" @change="handleUserSearch">
                      <el-option label="全部角色" value="" />
                      <el-option label="管理员" value="ADMIN" />
                      <el-option label="版主" value="MODERATOR" />
                      <el-option label="普通用户" value="USER" />
                    </el-select>
                </div>
             </div>
  
             <el-table :data="userList" style="width: 100%" v-loading="userLoading" border stripe>
               <el-table-column label="头像" width="70" align="center">
                 <template #default="scope">
                    <el-avatar :size="30" :src="scope.row.avatar || defaultAvatar" />
                 </template>
               </el-table-column>
               <el-table-column prop="username" label="用户名" />
               <el-table-column prop="nickname" label="昵称" />
               <el-table-column prop="role" label="角色" width="100">
                  <template #default="scope">
                     <el-tag size="small" :type="scope.row.role === 'ADMIN' ? 'danger' : (scope.row.role === 'MODERATOR' ? 'warning' : 'info')">
                       {{ scope.row.role === 'ADMIN' ? '管理员' : (scope.row.role === 'MODERATOR' ? '版主' : '用户') }}
                     </el-tag>
                  </template>
               </el-table-column>
               <el-table-column prop="status" label="状态" width="100" align="center">
                  <template #default="scope">
                     <el-tag size="small" :type="scope.row.status === 1 ? 'danger' : 'success'">
                       {{ scope.row.status === 1 ? '已封禁' : '正常' }}
                     </el-tag>
                  </template>
               </el-table-column>
               <el-table-column label="操作" width="150" align="center">
                 <template #default="scope">
                   <el-button 
                     v-if="scope.row.role !== 'ADMIN'"
                     :type="scope.row.status === 1 ? 'success' : 'danger'" 
                     size="small" 
                     @click="handleToggleStatus(scope.row)"
                   >
                     {{ scope.row.status === 1 ? '解封' : '封禁' }}
                   </el-button>
                 </template>
               </el-table-column>
             </el-table>
  
             <div class="pagination-box">
               <el-pagination 
                 background 
                 layout="prev, pager, next" 
                 :total="userTotal" 
                 :page-size="userPageSize"
                 @current-change="handleUserPageChange"
               />
             </div>
          </el-tab-pane>

          <el-tab-pane label="评论管理" name="comment">
            <div class="tab-action-bar">
              <div class="filter-row">
                <el-select v-model="commentSearchField" style="width: 160px; margin-right: 12px" @change="handleCommentSearch">
                  <el-option label="评论内容" value="content" />
                  <el-option label="帖子标题" value="postTitle" />
                  <el-option label="作者" value="author" />
                </el-select>
                <el-input
                  v-model="commentKeyword"
                  :placeholder="getCommentPlaceholder()"
                  style="width: 360px; margin-right: 12px"
                  clearable
                  @clear="handleCommentSearch"
                  @keyup.enter="handleCommentSearch"
                >
                  <template #append>
                    <el-button :icon="Search" @click="handleCommentSearch" />
                  </template>
                </el-input>
              </div>
            </div>

            <el-table :data="commentList" style="width: 100%" v-loading="commentLoading" border stripe>
              <el-table-column label="评论内容" min-width="400">
                <template #default="scope">
                  <div v-if="scope.row.parentContent" class="reply-to-container">
                    <div class="reply-quote">
                      <span class="reply-label">回复:</span>
                      <span class="parent-content-text">{{ scope.row.parentContent }}</span>
                    </div>
                  </div>
                  <div class="comment-main-content">
                    {{ stripReplyPrefix(scope.row.content) }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="作者" width="140">
                <template #default="scope">
                  <div style="display: flex; align-items: center; gap: 8px">
                    <el-avatar :size="24" :src="scope.row.authorAvatar || defaultAvatar" />
                    <span>{{ scope.row.authorName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="所属帖子" min-width="260">
                <template #default="scope">
                  <el-link type="primary" :href="`/post/${scope.row.postId}`" target="_blank">
                    {{ scope.row.postTitle || '查看详情' }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="时间" width="180">
                <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right" align="center">
                <template #default="scope">
                  <el-button link type="danger" @click="handleDeleteComment(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-box">
              <el-pagination
                background
                layout="prev, pager, next"
                :total="commentTotal"
                :page-size="commentPageSize"
                @current-change="handleCommentPageChange"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="标签管理" name="tag">
             <div class="tab-action-bar">
                <div class="filter-row">
                  <el-input 
                      v-model="tagKeyword" 
                      placeholder="搜索标签" 
                      style="width: 300px; margin-right: 15px" 
                      clearable
                      @clear="handleTagSearch"
                      @keyup.enter="handleTagSearch"
                  >
                      <template #append>
                        <el-button :icon="Search" @click="handleTagSearch" />
                      </template>
                  </el-input>
                  <el-button type="primary" @click="openTagDialog()">新增标签</el-button>
                </div>
             </div>

             <el-table :data="tagList" style="width: 100%" v-loading="tagLoading" border stripe>
               <el-table-column prop="name" label="标签名" />
               <el-table-column prop="createTime" label="创建时间" width="180">
                 <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
               </el-table-column>
               <el-table-column label="操作" width="160" fixed="right" align="center">
                 <template #default="scope">
                   <el-button link type="primary" size="small" @click="openTagDialog(scope.row)">编辑</el-button>
                   <el-button link type="danger" size="small" @click="handleDeleteTag(scope.row)">删除</el-button>
                 </template>
               </el-table-column>
             </el-table>

             <div class="pagination-box">
               <el-pagination 
                 background 
                 layout="prev, pager, next" 
                 :total="tagTotal" 
                 :page-size="tagPageSize"
                 @current-change="handleTagPageChange"
               />
             </div>

             <el-dialog v-model="tagDialogVisible" :title="editingTagId ? '编辑标签' : '新增标签'" width="420px">
               <el-form :model="tagForm" label-width="70px">
                 <el-form-item label="标签名">
                   <el-input v-model="tagForm.name" maxlength="32" show-word-limit />
                 </el-form-item>
               </el-form>
               <template #footer>
                 <span class="dialog-footer">
                   <el-button @click="tagDialogVisible = false">取消</el-button>
                   <el-button type="primary" @click="submitTag" :loading="tagSubmitting">确定</el-button>
                 </span>
               </template>
             </el-dialog>
          </el-tab-pane>
  
        </el-tabs>
      </el-card>
  
      <el-dialog v-model="dialogVisible" :title="isEditBoard ? '编辑板块' : '新增板块'" width="500px">
        <el-form :model="boardForm" label-width="80px">
          <el-form-item label="板块名称">
            <el-input v-model="boardForm.name" placeholder="例如：学习交流" />
          </el-form-item>
          <el-form-item label="板块描述">
            <el-input v-model="boardForm.description" type="textarea" placeholder="板块的简单介绍" />
          </el-form-item>
          <el-form-item label="板主">
            <el-select
              v-model="boardForm.moderatorId"
              placeholder="请选择版主"
              filterable
              remote
              clearable
              :remote-method="searchUsers"
              :loading="userSearchLoading"
              style="width: 100%"
              @visible-change="handleModeratorDropdownVisible"
            >
              <el-option
                v-if="currentModeratorOption"
                :key="`current-${currentModeratorOption.id}`"
                :label="(currentModeratorOption.nickname || currentModeratorOption.username) + '（当前版主）'"
                :value="currentModeratorOption.id"
                disabled
              />
              <el-option
                v-for="item in userOptions"
                :key="item.id"
                :label="item.nickname || item.username"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmitBoard" :loading="submitting">
              {{ isEditBoard ? '保存修改' : '确定新增' }}
            </el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { Plus, Delete, Top, Search } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { getBoardList } from '@/api/board'
  import { getPostList, auditPost } from '@/api/post'
  import { getTagPage, createTag, updateTag, deleteTag, type Tag } from '@/api/tag'
  import { getUserList, type UserVO } from '@/api/user' // 引入用户查询接口
  import { addBoard, updateBoard, deleteBoard, deletePostAdmin, toggleTopPost, toggleUserStatus, getCommentManagePage, deleteCommentManage, type CommentManageVO } from '@/api/admin'
  
  const activeTab = ref('board')
  const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  
  // --- 板块管理 ---
  const boardList = ref<any[]>([])
  const boardLoading = ref(false)
  const dialogVisible = ref(false)
  const submitting = ref(false)
  const isEditBoard = ref(false)
  const editingBoardId = ref<number | null>(null)
  const boardForm = reactive({ name: '', description: '', moderatorId: null as number | null })
  const userSearchLoading = ref(false)
  const userOptions = ref<UserVO[]>([])
  const currentModeratorOption = ref<UserVO | null>(null)
  
  const postKeyword = ref('')
  const postStatusFilter = ref(-1)
  
  // --- 帖子管理 ---
  const postList = ref([])
  const postLoading = ref(false)
  const postTotal = ref(0)
  const postPageNum = ref(1)
  const postPageSize = ref(10)
  
  // --- 评论管理 ---
  const commentList = ref<CommentManageVO[]>([])
  const commentLoading = ref(false)
  const commentTotal = ref(0)
  const commentPageNum = ref(1)
  const commentPageSize = ref(10)
  const commentKeyword = ref('')
  const commentSearchField = ref<'content' | 'postTitle' | 'author'>('content')

  const getCommentPlaceholder = () => {
    if (commentSearchField.value === 'postTitle') return '输入帖子标题关键词'
    if (commentSearchField.value === 'author') return '输入作者用户名/昵称'
    return '输入评论内容关键词'
  }

  // --- 标签管理 ---
  const tagList = ref<Tag[]>([])
  const tagLoading = ref(false)
  const tagTotal = ref(0)
  const tagPageNum = ref(1)
  const tagPageSize = ref(20)
  const tagKeyword = ref('')
  const tagDialogVisible = ref(false)
  const tagSubmitting = ref(false)
  const editingTagId = ref<number | null>(null)
  const tagForm = reactive({ name: '' })
  
  // --- 用户管理相关 ---
  const userList = ref<UserVO[]>([])
  const userLoading = ref(false)
  const userKeyword = ref('')
  const userRoleFilter = ref('')
  const userTotal = ref(0)
  const userPageNum = ref(1)
  const userPageSize = ref(10)

  // --- 方法：板块 ---
  const fetchBoards = async () => {
    boardLoading.value = true
    try {
      const res: any = await getBoardList()
      if (res.code === 0 || res.code === 200) boardList.value = res.data
    } finally { boardLoading.value = false }
  }
  
  const handleOpenAddBoard = () => {
    isEditBoard.value = false
    editingBoardId.value = null
    boardForm.name = ''
    boardForm.description = ''
    boardForm.moderatorId = null
    userOptions.value = []
    currentModeratorOption.value = null
    dialogVisible.value = true
  }

  const handleEditBoard = (row: any) => {
    isEditBoard.value = true
    editingBoardId.value = row.id
    boardForm.name = row.name
    boardForm.description = row.description
    boardForm.moderatorId = row.moderatorId || null
    // 记录当前版主（仅用于下拉里展示，且不可选择）
    if (row.moderatorId) {
      currentModeratorOption.value = {
        id: row.moderatorId,
        username: '',
        nickname: row.moderatorName,
        avatar: '',
        email: '',
        role: 'MODERATOR',
        status: 0,
        managedBoardId: row.id,
        createTime: ''
      } as UserVO
    } else {
      currentModeratorOption.value = null
    }
    dialogVisible.value = true
  }

  const handleModeratorDropdownVisible = (visible: boolean) => {
    if (!visible) return
    if (userOptions.value.length === 0) {
      searchUsers('')
    }
  }

  const searchUsers = async (query: string) => {
    userSearchLoading.value = true
    try {
      const keyword = query && query.trim() ? query.trim() : undefined
      const res: any = await getUserList({
        pageNum: 1,
        pageSize: 50,
        keyword,
        role: 'USER'
      })
      if (res.code === 0 || res.code === 200) {
        userOptions.value = res.data.records || []
      }
    } finally {
      userSearchLoading.value = false
    }
  }

  const handleSubmitBoard = async () => {
    if(!boardForm.name) return ElMessage.warning('请输入板块名称')
    submitting.value = true
    try {
      const data = { ...boardForm }
      const res: any = isEditBoard.value 
        ? await updateBoard(editingBoardId.value!, data)
        : await addBoard(data)
      
      if (res.code === 0 || res.code === 200) {
        ElMessage.success(isEditBoard.value ? '修改成功' : '添加成功')
        dialogVisible.value = false
        fetchBoards()
      }
    } finally { submitting.value = false }
  }
  
  const handleDeleteBoard = (row: any) => {
    ElMessageBox.confirm(`确定要删除板块【${row.name}】吗？删除后不可恢复！`, '警告', {
      type: 'warning', confirmButtonText: '确定删除', confirmButtonClass: 'el-button--danger'
    }).then(async () => {
      const res: any = await deleteBoard(row.id)
      if (res.code === 0 || res.code === 200) { ElMessage.success('删除成功'); fetchBoards() }
    })
  }
  
  // --- 方法：帖子 ---
  const fetchPosts = async () => {
    postLoading.value = true
    try {
      const res: any = await getPostList({ 
        pageNum: postPageNum.value, 
        pageSize: postPageSize.value, 
        keyword: postKeyword.value,
        status: postStatusFilter.value
      })
      if (res.code === 0 || res.code === 200) {
        postList.value = res.data.records
        postTotal.value = res.data.total
      }
    } finally { postLoading.value = false }
  }
  const handlePostSearch = () => {
    postPageNum.value = 1 // 搜索时重置为第一页
    fetchPosts()
  }
  const handlePostPageChange = (page: number) => { postPageNum.value = page; fetchPosts() }
  const handleTopPost = async (row: any) => {
    try {
      const res: any = await toggleTopPost(row.id)
      if (res.code === 0 || res.code === 200) { ElMessage.success('操作成功'); fetchPosts() }
    } catch(e) {}
  }
  const handleDeletePost = (row: any) => {
    ElMessageBox.confirm('确定要强制删除该帖子吗？', '管理员操作', { type: 'warning' })
      .then(async () => {
        const res: any = await deletePostAdmin(row.id)
        if (res.code === 0 || res.code === 200) { ElMessage.success('帖子已删除'); fetchPosts() }
      })
  }
 
  const handleAuditPost = async (row: any, status: number) => {
    try {
      if (status === 2) {
        const { value } = await ElMessageBox.prompt('请输入拒绝理由', '拒绝帖子', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '例如：内容不符合板块主题 / 含广告 / 含违规信息',
          inputValidator: (v: string) => !!v && v.trim().length > 0,
          inputErrorMessage: '拒绝理由不能为空'
        })
        const res: any = await auditPost(row.id, 2, value)
        if (res.code === 0 || res.code === 200) {
          ElMessage.success('已拒绝')
          fetchPosts()
        }
        return
      }
 
      const res: any = await auditPost(row.id, 1)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('已通过审核')
        fetchPosts()
      }
    } catch (e) {
      console.error(e)
    }
  }

  const fetchComments = async () => {
    commentLoading.value = true
    try {
      const res: any = await getCommentManagePage({
        pageNum: commentPageNum.value,
        pageSize: commentPageSize.value,
        keyword: commentKeyword.value,
        searchField: commentSearchField.value
      })
      if (res.code === 0 || res.code === 200) {
        commentList.value = res.data.records
        commentTotal.value = res.data.total
      }
    } finally {
      commentLoading.value = false
    }
  }

  const handleCommentSearch = () => {
    commentPageNum.value = 1
    fetchComments()
  }

  const handleCommentPageChange = (page: number) => {
    commentPageNum.value = page
    fetchComments()
  }

  const handleDeleteComment = (row: CommentManageVO) => {
    ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' })
      .then(async () => {
        const res: any = await deleteCommentManage(row.id)
        if (res.code === 0 || res.code === 200) {
          ElMessage.success('删除成功')
          fetchComments()
        }
      })
  }

  // --- 方法：标签管理 ---
  const fetchTags = async () => {
    tagLoading.value = true
    try {
      const res: any = await getTagPage({
        pageNum: tagPageNum.value,
        pageSize: tagPageSize.value,
        keyword: tagKeyword.value
      })
      if (res.code === 0 || res.code === 200) {
        tagList.value = res.data.records
        tagTotal.value = res.data.total
      }
    } finally {
      tagLoading.value = false
    }
  }

  const handleTagSearch = () => {
    tagPageNum.value = 1
    fetchTags()
  }

  const handleTagPageChange = (page: number) => {
    tagPageNum.value = page
    fetchTags()
  }

  const openTagDialog = (row?: Tag) => {
    if (row) {
      editingTagId.value = row.id
      tagForm.name = row.name
    } else {
      editingTagId.value = null
      tagForm.name = ''
    }
    tagDialogVisible.value = true
  }

  const submitTag = async () => {
    if (!tagForm.name || !tagForm.name.trim()) return ElMessage.warning('请输入标签名')
    tagSubmitting.value = true
    try {
      const name = tagForm.name.trim()
      const res: any = editingTagId.value ? await updateTag(editingTagId.value, name) : await createTag(name)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success(editingTagId.value ? '修改成功' : '创建成功')
        tagDialogVisible.value = false
        fetchTags()
      }
    } finally {
      tagSubmitting.value = false
    }
  }

  const handleDeleteTag = (row: Tag) => {
    ElMessageBox.confirm(`确定要删除标签【${row.name}】吗？`, '提示', { type: 'warning' })
      .then(async () => {
        const res: any = await deleteTag(row.id)
        if (res.code === 0 || res.code === 200) {
          ElMessage.success('删除成功')
          fetchTags()
        }
      })
  }
  
  // --- 方法：任命板主 ---
  const handleOpenAppoint = (board: any) => {
    currentBoard.value = board
    userSelectVisible.value = true
    // 重置候选人列表
    candidateKeyword.value = ''
    candidatePageNum.value = 1
    fetchCandidates()
    
    // 查找当前板块的板主
    currentModerator.value = null
    getUserList({ pageNum: 1, pageSize: 1, managedBoardId: board.id }).then((res: any) => {
      if (res.code === 0 || res.code === 200) {
        if (res.data.records && res.data.records.length > 0) {
          currentModerator.value = res.data.records[0]
        }
      }
    })
  }
  
  const fetchCandidates = async () => {
    candidateLoading.value = true
    try {
      const res: any = await getUserList({
        pageNum: candidatePageNum.value,
        pageSize: candidatePageSize.value,
        keyword: candidateKeyword.value,
        role: 'USER' // 强制要求后端只返回普通用户
      })
      if (res.code === 0 || res.code === 200) {
        candidateList.value = res.data.records || []
        candidateTotal.value = res.data.total
      }
    } finally {
      candidateLoading.value = false
    }
  }
  
  const handleCandidatePageChange = (page: number) => {
    candidatePageNum.value = page
    fetchCandidates()
  }
  
  // 这里的 fetchUserList 需要确保 role 筛选能生效
  const fetchUserList = async () => {
    userLoading.value = true
    try {
      const res: any = await getUserList({
        pageNum: userPageNum.value,
        pageSize: userPageSize.value,
        keyword: userKeyword.value,
        role: userRoleFilter.value
      })
      if (res.code === 0 || res.code === 200) {
        userList.value = res.data.records || []
        userTotal.value = res.data.total
      }
    } finally {
      userLoading.value = false
    }
  }

  const handleUserSearch = () => {
    userPageNum.value = 1
    fetchUserList()
  }
  
  const handleUserPageChange = (page: number) => {
    userPageNum.value = page
    fetchUserList()
  }

  const handleToggleStatus = (row: any) => {
    const action = row.status === 1 ? '解封' : '封禁'
    ElMessageBox.confirm(`确定要${action}用户【${row.nickname || row.username}】吗？`, '提示', {
      type: 'warning'
    }).then(async () => {
      const res: any = await toggleUserStatus(row.id)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success(`${action}成功`)
        fetchUserList()
      }
    })
  }
  
  const formatTime = (time: string) => {
    if (!time) return ''
    return time.replace('T', ' ').substring(0, 16)
  }

  // ✅ 新增：去除评论内容中可能存在的旧版“回复 @xxx : ”前缀
  const stripReplyPrefix = (content: string) => {
    if (!content) return ''
    return content.replace(/^回复\s*@.*?\s*:\s*/, '')
  }

  const handleTabChange = (name: string) => {
    if (name === 'board') {
      fetchBoards()
    } else if (name === 'post') {
      fetchPosts()
    } else if (name === 'user') {
      userKeyword.value = ''
      userRoleFilter.value = '' // 切换回用户管理时重置角色筛选
      userPageNum.value = 1
      fetchUserList()
    } else if (name === 'comment') {
      fetchComments()
    } else if (name === 'tag') {
      fetchTags()
    }
  }
  
  onMounted(() => {
    fetchBoards()
    fetchPosts()
    fetchUserList()
    fetchComments()
    fetchTags()
  })
  </script>
  
  <style scoped lang="scss">
  .admin-container { max-width: 1200px; margin: 20px auto; }
  .card-header { font-size: 18px; font-weight: bold; }
  .tab-action-bar { margin-bottom: 20px; display: flex; justify-content: flex-start; }
  .form-tip { font-size: 12px; color: #999; margin-left: 10px; display: inline-block; }
  .pagination-box { margin-top: 20px; display: flex; justify-content: center; }
  .filter-row {
    display: flex;
    align-items: center;
  }
  
  .reply-to-container {
    margin-bottom: 8px;
  }

  .reply-quote {
    font-size: 13px;
    color: #888;
    background-color: #f8f9fa;
    border-left: 4px solid #dcdfe6;
    padding: 6px 12px;
    border-radius: 0 4px 4px 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
    
    .reply-label {
      font-weight: bold;
      font-size: 11px;
      text-transform: uppercase;
      color: #a8abb2;
    }
    
    .parent-content-text {
      white-space: normal;
      word-break: break-all;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .comment-main-content {
    font-size: 14px;
    color: #303133;
    white-space: normal;
    word-break: break-all;
    line-height: 1.6;
  }
  </style>

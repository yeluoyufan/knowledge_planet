<template>
    <div class="moderator-container">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span class="title">⚖️ 板主工作台</span>
            <el-tag type="success" v-if="boardId">板块管理模式已开启</el-tag>
          </div>
        </template>
  
        <div v-if="!boardId" class="empty-state">
          <el-empty description="您当前没有管理的板块" />
        </div>
  
        <div v-else>
           <div class="action-bar">
            <div class="search-box">
                <template v-if="activeTab !== 'comment'">
                  <el-input
                      v-model="keyword"
                      placeholder="搜索本板块帖子"
                      style="width: 300px"
                      clearable
                      @clear="handleSearch"
                      @keyup.enter="handleSearch"
                  >
                      <template #append>
                      <el-button :icon="Search" @click="handleSearch" />
                      </template>
                  </el-input>
                </template>
                <template v-else>
                  <el-input
                      v-model="commentKeyword"
                      placeholder="搜索评论内容"
                      style="width: 300px"
                      clearable
                      @clear="handleCommentSearch"
                      @keyup.enter="handleCommentSearch"
                  >
                      <template #append>
                      <el-button :icon="Search" @click="handleCommentSearch" />
                      </template>
                  </el-input>
                </template>
                </div>
             <el-alert title="您可以删除本板块内的违规帖子和评论，请谨慎操作。" type="warning" show-icon :closable="false" />
           </div>

           <el-tabs v-model="activeTab" @tab-change="handleTabChange">
             <el-tab-pane label="待审核" name="pending">
               <el-table :data="postList" style="width: 100%; margin-top: 20px" v-loading="loading" border stripe>
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
                 <el-table-column prop="authorName" label="发布者" width="120" />
                 <el-table-column prop="createTime" label="发布时间" width="180">
                   <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
                 </el-table-column>
                 <el-table-column label="操作" width="200" fixed="right" align="center">
                   <template #default="scope">
                     <el-button
                       size="small"
                       type="success"
                       plain
                       :icon="Check"
                       @click="handleAudit(scope.row, 1)"
                       style="margin-right: 10px"
                     >
                       通过
                     </el-button>
                    <el-button type="danger" size="small" :icon="Close" plain @click="handleAudit(scope.row, 2)">拒绝</el-button>
                   </template>
                 </el-table-column>
               </el-table>
             </el-tab-pane>

             <el-tab-pane label="已发布" name="published">
               <el-table :data="postList" style="width: 100%; margin-top: 20px" v-loading="loading" border stripe>
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
                 <el-table-column prop="authorName" label="发布者" width="120" />
                 <el-table-column prop="createTime" label="发布时间" width="180">
                   <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
                 </el-table-column>
                 <el-table-column label="操作" width="200" fixed="right" align="center">
                   <template #default="scope">
                     <el-button
                       size="small"
                       type="warning"
                       plain
                       :icon="Top"
                       @click="handleToggleTop(scope.row)"
                       style="margin-right: 10px"
                     >
                       置顶/取消
                     </el-button>
                     <el-popconfirm title="确定要删除这个帖子吗？" @confirm="handleDelete(scope.row)">
                       <template #reference>
                         <el-button type="danger" size="small" :icon="Delete" plain>删除</el-button>
                       </template>
                     </el-popconfirm>
                   </template>
                 </el-table-column>
               </el-table>
             </el-tab-pane>

             <el-tab-pane label="已拒绝" name="rejected">
               <el-table :data="postList" style="width: 100%; margin-top: 20px" v-loading="loading" border stripe>
                 <el-table-column label="标题" min-width="300">
                   <template #default="scope">
                     <el-link type="primary" :href="`/post/${scope.row.id}`" target="_blank">
                        {{ scope.row.title }}
                     </el-link>
                   </template>
                 </el-table-column>
                 <el-table-column prop="authorName" label="发布者" width="120" />
                 <el-table-column prop="createTime" label="发布时间" width="180">
                   <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
                 </el-table-column>
                 <el-table-column label="操作" width="200" fixed="right" align="center">
                   <template #default="scope">
                     <el-popconfirm title="确定要删除这个帖子吗？" @confirm="handleDelete(scope.row)">
                       <template #reference>
                         <el-button type="danger" size="small" :icon="Delete" plain>删除</el-button>
                       </template>
                     </el-popconfirm>
                   </template>
                 </el-table-column>
               </el-table>
             </el-tab-pane>

             <el-tab-pane label="评论管理" name="comment">
               <el-table :data="commentList" style="width: 100%; margin-top: 20px" v-loading="commentLoading" border stripe>
                 <el-table-column label="评论内容" min-width="320">
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
                 <el-table-column prop="authorName" label="发布者" width="140" />
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
                     <el-popconfirm title="确定要删除这条评论吗？" @confirm="handleDeleteComment(scope.row)">
                       <template #reference>
                         <el-button type="danger" size="small" plain>删除</el-button>
                       </template>
                     </el-popconfirm>
                   </template>
                 </el-table-column>
               </el-table>
             </el-tab-pane>
           </el-tabs>
  
           <div class="pagination-box">
             <el-pagination 
               background 
               layout="prev, pager, next" 
               :total="activeTab === 'comment' ? commentTotal : total" 
               :page-size="pageSize"
               @current-change="handlePageChange"
             />
           </div>
        </div>
      </el-card>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, computed } from 'vue'
  import { Delete, Search, Top, Check, Close } from '@element-plus/icons-vue'
  import { useUserStore } from '@/store/userStore'
  import { getPostList, deletePost, auditPost } from '@/api/post'
  import { toggleTopPost, getCommentManagePage, deleteCommentManage, type CommentManageVO } from '@/api/admin'
  import { ElMessage, ElMessageBox } from 'element-plus'
  
  const userStore = useUserStore()
  const loading = ref(false)
  const postList = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')
  const activeTab = ref('pending')
  const currentStatus = ref(0)
  const commentKeyword = ref('')
  const commentList = ref<CommentManageVO[]>([])
  const commentLoading = ref(false)
  const commentTotal = ref(0)
  const commentPageNum = ref(1)
  
  // 获取当前用户管理的板块ID
  const boardId = computed(() => userStore.userInfo?.boardId)
  
  const fetchMyBoardPosts = async () => {
    if (!boardId.value) return
  
    loading.value = true
    try {
      const res: any = await getPostList({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        boardId: boardId.value,
        keyword: keyword.value,
        status: currentStatus.value
      })
      if (res.code === 0 || res.code === 200) {
        postList.value = res.data.records
        total.value = res.data.total
      }
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    pageNum.value = 1
    fetchMyBoardPosts()
  }
 
  const fetchComments = async () => {
    commentLoading.value = true
    try {
      const res: any = await getCommentManagePage({
        pageNum: commentPageNum.value,
        pageSize: pageSize.value,
        keyword: commentKeyword.value
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

  const handleTabChange = (tabName: string) => {
    if (tabName === 'comment') {
      activeTab.value = tabName
      commentPageNum.value = 1
      fetchComments()
      return
    }
    if (tabName === 'pending') {
      currentStatus.value = 0
    } else if (tabName === 'published') {
      currentStatus.value = 1
    } else {
      currentStatus.value = 2
    }
    pageNum.value = 1
    fetchMyBoardPosts()
  }

  const handleAudit = async (row: any, status: number) => {
    try {
      if (status === 2) {
        const { value } = await ElMessageBox.prompt('请输入拒绝理由', '拒绝帖子', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '例如：内容不符合板块主题 / 含广告 / 含违规信息',
          inputValidator: (v: string) => !!v && v.trim().length > 0,
          inputErrorMessage: '拒绝理由不能为空'
        })
        const res: any = await auditPost(row.id, status, value)
        if (res.code === 0 || res.code === 200) {
          ElMessage.success('已拒绝')
          fetchMyBoardPosts()
        }
        return
      }

      const res: any = await auditPost(row.id, status)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('已通过审核')
        fetchMyBoardPosts()
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleToggleTop = async (row: any) => {
    try {
      const res: any = await toggleTopPost(row.id)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('置顶状态已更新')
        fetchMyBoardPosts()
      }
    } catch (e) {
      console.error(e)
    }
  }
  
  const handlePageChange = (page: number) => {
    if (activeTab.value === 'comment') {
      commentPageNum.value = page
      fetchComments()
    } else {
      pageNum.value = page
      fetchMyBoardPosts()
    }
  }
 
  const handleDeleteComment = async (row: CommentManageVO) => {
    try {
      const res: any = await deleteCommentManage(row.id)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('删除成功')
        fetchComments()
      }
    } catch (e) {
      console.error(e)
    }
  }
  
  const handleDelete = async (row: any) => {
    try {
      const res: any = await deletePost(row.id)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success('帖子已删除')
        fetchMyBoardPosts()
      }
    } catch (e) {
      console.error(e)
    }
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
  
  onMounted(() => {
    fetchMyBoardPosts()
  })
  </script>
  
  <style scoped lang="scss">
  .moderator-container {
    max-width: 1200px;
    margin: 20px auto;
  }
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .title { font-size: 18px; font-weight: bold; }
  }
  .pagination-box {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
    .action-bar {
        display: flex;
        gap: 20px;
        align-items: center;
        margin-bottom: 20px;
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

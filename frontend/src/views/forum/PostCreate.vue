<template>
    <div class="create-post-container">
      <el-card class="box-card">
        <template #header>
          <div class="card-header">
            <span class="header-title">{{ isEditMode ? '📝 编辑帖子' : '📝 发布新帖子' }}</span>
            <el-button text @click="$router.back()">返回</el-button>
          </div>
        </template>
        
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" v-loading="loading">
           <el-row :gutter="20">
            <el-col :span="6" :xs="24">
              <el-form-item label="选择板块" prop="boardId">
                <el-select v-model="form.boardId" placeholder="请选择发帖板块" style="width: 100%" :disabled="isEditMode">
                  <el-option v-for="board in boardList" :key="board.id" :label="board.name" :value="board.id" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :span="18" :xs="24">
              <el-form-item label="帖子标题" prop="title">
                <el-input v-model="form.title" placeholder="请输入标题" maxlength="50" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="标签" prop="tags">
                <el-select
                  v-model="combinedTags"
                  multiple
                  filterable
                  allow-create
                  :reserve-keyword="false"
                  default-first-option
                  placeholder="请选择或输入自定义标签"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in tagList"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
  
          <el-form-item label="内容正文" prop="content">
            <div class="editor-mode">
              <div class="editor-actions">
                <el-button size="small" plain @click="triggerLocalImage">插入本地图片</el-button>
                <span v-if="autoSaveTime" class="autosave-text">已自动保存 {{ autoSaveTime }}</span>
              </div>
            </div>
            <input ref="localImageInputRef" type="file" accept="image/*" multiple style="display: none" @change="handleLocalImageChange" />

            <v-md-editor
              v-model="markdownContent"
              height="500px"
              placeholder="请输入正文..."
              @upload-image="handleUploadImage"
              left-toolbar="undo redo clear | h bold italic strikethrough | quote ul ol | link image code"
            />
          </el-form-item>
  
          <div class="form-footer">
            <el-button @click="$router.back()">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">
              {{ isEditMode ? '保存修改并重新审核' : '立即发布' }}
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, reactive, onMounted, computed, watch } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
  import { createPost, updatePost, getPostDetail } from '@/api/post'
  import { getBoardList, type Board } from '@/api/board'
  import { getAllTags, type Tag } from '@/api/tag'
  import { uploadImage } from '@/api/upload'
  import { useUserStore } from '@/store/userStore'
  
  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const formRef = ref<FormInstance>()
  const submitting = ref(false)
  const loading = ref(false)
  const boardList = ref<Board[]>([])
  const tagList = ref<Tag[]>([])
  const markdownContent = ref('')
  const localImageInputRef = ref<HTMLInputElement>()
  let draftAutoTimer: any = null
  const autoSaveTime = ref('')
  const editPostStatus = ref<number | null>(null)
  
  // 图片上传处理
  const handleUploadImage = async (event: any, insertImage: any, files: File[]) => {
    try {
      for (const file of files) {
        const res: any = await uploadImage(file)
        if (res.code === 0 || res.code === 200) {
          insertImage({
            url: res.data,
            desc: file.name
          })
        } else {
          ElMessage.error('图片上传失败')
        }
      }
    } catch (error) {
      console.error(error)
      ElMessage.error('图片上传失败')
    }
  }
 
  const triggerLocalImage = () => {
    localImageInputRef.value?.click()
  }
 
  const handleLocalImageChange = async (e: Event) => {
    const input = e.target as HTMLInputElement
    const files = input.files ? Array.from(input.files) : []
    if (files.length === 0) return
 
    try {
      for (const file of files) {
        const res: any = await uploadImage(file)
        if (res.code === 0 || res.code === 200) {
          markdownContent.value = `${markdownContent.value}\n\n![${file.name}](${res.data})\n`
        } else {
          ElMessage.error('图片上传失败')
        }
      }
    } catch (err) {
      console.error(err)
      ElMessage.error('图片上传失败')
    } finally {
      input.value = ''
    }
  }
  
  // ✅ 判断是否为编辑模式
  const isEditMode = computed(() => !!route.params.id)
  
  const form = reactive({
    id: undefined as number | undefined,
    title: '',
    boardId: undefined as number | undefined,
    content: '',
    tagIds: [] as number[],
    customTags: [] as string[]
  })
  
  // 用于合并已有标签和自定义标签
  const combinedTags = ref<(number | string)[]>([])
  
  const rules = reactive<FormRules>({
    title: [{ required: true, message: '请输入标题', trigger: 'blur' }, { min: 5, max: 50, message: '长度5-50', trigger: 'blur' }],
    boardId: [{ required: true, message: '请选择板块', trigger: 'change' }],
    content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
  })
  
  onMounted(async () => {
    // 1. 加载板块
    const boardRes: any = await getBoardList()
    if (boardRes.code === 0 || boardRes.code === 200) {
      boardList.value = boardRes.data
    }

    // 2. 加载标签
    const tagRes: any = await getAllTags()
    if (tagRes.code === 0 || tagRes.code === 200) {
      tagList.value = tagRes.data
    }
  
    // 3. 如果是编辑模式，回显数据
    if (isEditMode.value) {
      loading.value = true
      try {
        const postId = Number(route.params.id)
        const res: any = await getPostDetail(postId, false)
        if (res.code === 0 || res.code === 200) {
          form.id = res.data.id
          form.title = res.data.title
          form.boardId = res.data.boardId
          editPostStatus.value = res.data.status
          const content = res.data.content || ''
          markdownContent.value = content
          // 如果后端返回了 tags，提取 ID
          if (res.data.tags) {
            combinedTags.value = res.data.tags.map((t: Tag) => t.id)
          }
        }
      } finally {
        loading.value = false
      }
    }

    loadAutoDraftIfNeeded()
  })

  const getDraftKey = () => {
    const uid = userStore.userInfo?.id ?? userStore.userInfo?.username
    return `draft_post_create_${uid}`
  }

  const loadAutoDraftIfNeeded = () => {
    if (isEditMode.value) return
    if (form.title || form.boardId || combinedTags.value.length > 0 || markdownContent.value) return
    const raw = localStorage.getItem(getDraftKey())
    if (!raw) return
    try {
      const d: any = JSON.parse(raw)
      form.title = d.title || ''
      form.boardId = d.boardId
      combinedTags.value = Array.isArray(d.combinedTags) ? d.combinedTags : []
      markdownContent.value = d.markdownContent || ''
      autoSaveTime.value = d.updateTime ? (String(d.updateTime).split(' ').slice(-1)[0] || d.updateTime) : ''
    } catch (e) {}
  }

  const saveAutoDraft = () => {
    const updateTime = new Date().toLocaleString()
    const payload = {
      title: form.title,
      boardId: form.boardId,
      combinedTags: combinedTags.value,
      markdownContent: markdownContent.value,
      updateTime
    }
    localStorage.setItem(getDraftKey(), JSON.stringify(payload))
    autoSaveTime.value = updateTime.split(' ').slice(-1)[0] || updateTime
  }

  const clearAutoDraft = () => {
    localStorage.removeItem(getDraftKey())
    autoSaveTime.value = ''
  }

  watch([markdownContent], () => {
    form.content = markdownContent.value
  }, { deep: true })

  const autoSaveDraft = () => {
    if (isEditMode.value) return
    if (!form.title && !form.boardId && combinedTags.value.length === 0 && !markdownContent.value) return
    if (draftAutoTimer) clearTimeout(draftAutoTimer)
    draftAutoTimer = setTimeout(() => {
      saveAutoDraft()
    }, 800)
  }

  watch([() => form.title, () => form.boardId, combinedTags, markdownContent], () => {
    autoSaveDraft()
  }, { deep: true })

  const handleSubmit = async () => {
    if (!formRef.value) return
    form.content = markdownContent.value
    await formRef.value.validate(async (valid) => {
      if (valid) {
        submitting.value = true
        
        // 分离已有标签 ID 和自定义标签字符串
        const tagIds = combinedTags.value.filter(tag => typeof tag === 'number') as number[]
        const customTags = combinedTags.value.filter(tag => typeof tag === 'string') as string[]
        const content = markdownContent.value

        try {
          if (isEditMode.value) {
            // ✅ 编辑逻辑
            await updatePost({
              id: form.id!,
              title: form.title,
              content: content,
              tagIds: tagIds,
              customTags: customTags
            })
            ElMessage.success('修改成功，已重新提交审核')
            router.push(`/user/${localStorage.getItem('userInfo') ? (JSON.parse(localStorage.getItem('userInfo')!).id ?? JSON.parse(localStorage.getItem('userInfo')!).username) : ''}`)
          } else {
            // ✅ 新增逻辑
            await createPost({
              title: form.title,
              content: content,
              boardId: form.boardId!,
              tagIds: tagIds,
              customTags: customTags
            })
            clearAutoDraft()
            ElMessage.success('发布成功')
            router.push('/')
          }
        } catch (error) {
          console.error(error)
        } finally {
          submitting.value = false
        }
      }
    })
  }
  </script>
  
  <style scoped lang="scss">
  .create-post-container { max-width: 1000px; margin: 20px auto; }
  .card-header { display: flex; justify-content: space-between; align-items: center; .header-title { font-size: 18px; font-weight: 600; } }
  .form-footer { display: flex; justify-content: flex-end; margin-top: 20px; gap: 12px; }

  .editor-mode {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .editor-actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }
  
  .autosave-text {
    font-size: 12px;
    color: #94a3b8;
    white-space: nowrap;
  }
  </style>

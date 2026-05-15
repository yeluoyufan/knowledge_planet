<template>
  <div class="auth-wrapper">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="register-container">
      <!-- 左侧品牌展示区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-icon">
            <el-icon><Star /></el-icon>
          </div>
          <h1 class="brand-title">加入知识星球</h1>
          <p class="brand-slogan">开启您的深度学习与交流之旅</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><EditPen /></el-icon>
              <span>自由表达观点</span>
            </div>
            <div class="feature-item">
              <el-icon><CircleCheck /></el-icon>
              <span>结识志同道合的朋友</span>
            </div>
            <div class="feature-item">
              <el-icon><TrendCharts /></el-icon>
              <span>共同见证成长</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧注册表单区 -->
      <div class="form-section">
        <div class="form-content">
          <div class="form-header">
            <h2>创建账号</h2>
            <p>只需几步，即可加入我们的社区</p>
          </div>

          <el-form 
            ref="registerFormRef"
            :model="registerForm"
            :rules="rules"
            label-width="0"
            size="large"
            class="custom-form"
          >
            <el-form-item prop="username">
              <el-input 
                v-model="registerForm.username" 
                placeholder="用户名 (用于登录)"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="nickname">
              <el-input 
                v-model="registerForm.nickname" 
                placeholder="昵称 (展示名称)"
                :prefix-icon="Edit"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input 
                v-model="registerForm.password" 
                type="password" 
                placeholder="设置密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
    
            <el-form-item prop="confirmPassword">
              <el-input 
                v-model="registerForm.confirmPassword" 
                type="password" 
                placeholder="确认密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
    
            <el-form-item>
              <el-button 
                type="primary" 
                class="submit-btn" 
                :loading="loading"
                @click="handleSubmit"
              >
                立即注册
              </el-button>
            </el-form-item>
            
            <div class="auth-footer">
              <span>已有账号？</span>
              <el-link type="primary" @click="$router.push('/login')" :underline="false">返回登录</el-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { User, Lock, Star, EditPen, CircleCheck, TrendCharts, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

// 验证确认密码是否一致
const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const payload = {
          username: registerForm.username,
          password: registerForm.password,
          nickname: registerForm.nickname
        }
        
        await register(payload)
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.auth-wrapper {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 0;
  
  .circle {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.4;
  }
  
  .circle-1 {
    width: 400px;
    height: 400px;
    background: #409eff;
    top: -100px;
    left: -100px;
  }
  
  .circle-2 {
    width: 300px;
    height: 300px;
    background: #67c23a;
    bottom: -50px;
    right: -50px;
  }
  
  .circle-3 {
    width: 250px;
    height: 250px;
    background: #e6a23c;
    top: 20%;
    right: 15%;
  }
}

.register-container {
  width: 1000px;
  height: 650px;
  display: flex;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);
  z-index: 1;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 左侧品牌区 */
.brand-section {
  flex: 1;
  background: linear-gradient(225deg, #409eff 0%, #3a8ee6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  padding: 40px;
  position: relative;
  
  .brand-content {
    position: relative;
    z-index: 1;
    text-align: center;
    
    .logo-icon {
      font-size: 64px;
      margin-bottom: 24px;
      display: inline-block;
      padding: 16px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 20px;
      line-height: 1;
    }
    
    .brand-title {
      font-size: 36px;
      font-weight: 700;
      margin-bottom: 12px;
      letter-spacing: 2px;
    }
    
    .brand-slogan {
      font-size: 18px;
      opacity: 0.9;
      margin-bottom: 48px;
    }
    
    .brand-features {
      text-align: left;
      display: inline-block;
      
      .feature-item {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 20px;
        font-size: 16px;
        
        .el-icon {
          font-size: 20px;
          padding: 8px;
          background: rgba(255, 255, 255, 0.15);
          border-radius: 8px;
        }
      }
    }
  }
}

/* 右侧表单区 */
.form-section {
  width: 450px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
  
  .form-content {
    width: 100%;
    max-width: 340px;
    
    .form-header {
      margin-bottom: 32px;
      
      h2 {
        font-size: 28px;
        font-weight: 700;
        color: #1f2f3d;
        margin-bottom: 8px;
      }
      
      p {
        color: #909399;
        font-size: 14px;
      }
    }
    
    .custom-form {
      :deep(.el-input__wrapper) {
        background-color: #f5f7fa;
        box-shadow: none !important;
        border: 1px solid transparent;
        transition: all 0.3s;
        border-radius: 12px;
        padding: 4px 15px;
        
        &:hover {
          border-color: #dcdfe6;
        }
        
        &.is-focus {
          background-color: white;
          border-color: #409eff;
        }
      }
      
      .submit-btn {
        width: 100%;
        height: 50px;
        font-size: 16px;
        font-weight: 600;
        border-radius: 12px;
        box-shadow: 0 10px 15px -3px rgba(64, 158, 255, 0.3);
        margin-top: 10px;
      }
      
      .auth-footer {
        display: flex;
        align-items: center;
        margin-top: 32px;
        font-size: 14px;
        color: #606266;
        
        .el-link {
          font-weight: 600;
          margin-left: 4px;
        }
      }
    }
  }
}

@media (max-width: 1024px) {
  .register-container {
    width: 90%;
    height: auto;
    min-height: 500px;
  }
  .brand-section {
    display: none;
  }
  .form-section {
    width: 100%;
  }
}
</style>

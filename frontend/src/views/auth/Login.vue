<template>
  <div class="auth-wrapper">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌展示区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-icon">
            <el-icon><Star /></el-icon>
          </div>
          <h1 class="brand-title">知识星球</h1>
          <p class="brand-slogan">在这里，连接每一份热爱与思考</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><ChatLineRound /></el-icon>
              <span>高质量技术讨论</span>
            </div>
            <div class="feature-item">
              <el-icon><Document /></el-icon>
              <span>深度技术沉淀</span>
            </div>
            <div class="feature-item">
              <el-icon><Connection /></el-icon>
              <span>同行交流分享</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="form-section">
        <div class="form-content">
          <div class="form-header">
            <h2>探索新知</h2>
            <p>登录您的账号，开启今日思考</p>
          </div>

          <el-form 
            ref="loginFormRef"
            :model="loginForm"
            :rules="rules"
            label-width="0"
            size="large"
            class="custom-form"
          >
            <el-form-item prop="username">
              <el-input 
                v-model="loginForm.username" 
                placeholder="用户名"
                :prefix-icon="User"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleSubmit"
              />
            </el-form-item>
    
            <el-form-item>
              <el-button 
                type="primary" 
                class="submit-btn" 
                :loading="loading"
                @click="handleSubmit"
              >
                立即登录
              </el-button>
            </el-form-item>
            
            <div class="auth-footer">
              <span>还没有账号？</span>
              <el-link type="primary" @click="$router.push('/register')" :underline="false">立即注册</el-link>
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
import { useUserStore } from '@/store/userStore'
import { User, Lock, Star, ChatLineRound, Document, Connection } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleSubmit = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.handleLogin(loginForm)
        ElMessage.success('登录成功，欢迎回来')
        router.push('/')
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

.login-container {
  width: 1000px;
  height: 600px;
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
  
  &::before {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    background: url('https://raw.githubusercontent.com/visgl/react-map-gl/master/examples/.screenshots/controls.png') center/cover;
    opacity: 0.1;
    mix-blend-mode: overlay;
  }
  
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
      margin-bottom: 40px;
      
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
      
      .form-options {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
        font-size: 14px;
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

/* 响应式适配 */
@media (max-width: 1024px) {
  .login-container {
    width: 90%;
    height: auto;
    min-height: 500px;
  }
  .brand-section {
    display: none; /* 屏幕较小时隐藏左侧品牌区 */
  }
  .form-section {
    width: 100%;
  }
}
</style>


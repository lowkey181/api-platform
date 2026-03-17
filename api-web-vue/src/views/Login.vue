<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>API 平台登录</h2>
        </div>
      </template>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin"></el-input>
        </el-form-item>
        <div class="login-options">
          <el-link type="primary" @click="router.push('/register')">没有账号？去注册</el-link>
        </div>
        <el-form-item label-width="0">
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import { signApi } from '@/api/sign'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loginFormRef = ref<FormInstance>()

const loginForm = reactive({
  username: '',
  password: '',
})

const rules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await userApi.login(loginForm)
        userStore.setToken(res.data)
        const resInfo = await userApi.getInfo()
        
        // 获取用户的 accessKey（用于后续调用接口时生成签名）
        let userInfoData = resInfo.data
        try {
          const signRes = await signApi.generate()
          if (signRes.data) {
            // 只保存 accessKey，timestamp 和 nonce 在每次调用接口时动态生成
            userInfoData = {
              ...userInfoData,
              accessKey: signRes.data.accessKey,
            }
          }
        } catch (signError) {
          console.warn('获取 accessKey 失败，继续使用基本用户信息', signError)
        }
        
        userStore.setUserInfo(userInfoData)
        ElMessage.success('登录成功')
        if (userStore.isAdmin) {
          await router.push('/admin/dashboard')
        } else {
          await router.push('/user/home')
        }
      } catch (error) {
        // Axios interceptor handles error message
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}
.login-card {
  width: 400px;
}
.card-header h2 {
  text-align: center;
  margin: 0;
}
.login-options {
  text-align: right;
  margin-bottom: 20px;
}
</style>

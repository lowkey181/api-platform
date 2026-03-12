<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <h1>欢迎使用 API 开放平台</h1>
          <p>在这里，你可以快速集成各种 API 接口，助力你的应用开发。</p>
          <div class="action-buttons">
            <el-button type="primary" size="large" @click="router.push('/user/api-market')">
              浏览 API 市场
            </el-button>
            <el-button size="large" @click="router.push('/user/my-apps')">
              管理我的应用
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card" @click="router.push('/user/api-market')">
          <el-icon :size="40" color="#409EFF"><ShoppingCart /></el-icon>
          <h3>API 市场</h3>
          <p>浏览和购买各种 API 接口套餐</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card" @click="router.push('/user/my-interfaces')">
          <el-icon :size="40" color="#67C23A"><Connection /></el-icon>
          <h3>我的接口</h3>
          <p>查看已购买的接口和调用次数</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card" @click="router.push('/user/my-apps')">
          <el-icon :size="40" color="#E6A23C"><List /></el-icon>
          <h3>我的应用</h3>
          <p>管理应用密钥和配置信息</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.welcome-card {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}
.welcome-card h1 {
  font-size: 36px;
  margin-bottom: 20px;
}
.welcome-card p {
  font-size: 18px;
  margin-bottom: 30px;
  opacity: 0.9;
}
.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}
.feature-card {
  text-align: center;
  padding: 30px 20px;
  cursor: pointer;
  transition: transform 0.3s;
}
.feature-card:hover {
  transform: translateY(-5px);
}
.feature-card h3 {
  margin: 20px 0 10px;
  font-size: 20px;
  color: #303133;
}
.feature-card p {
  color: #909399;
  font-size: 14px;
}
</style>

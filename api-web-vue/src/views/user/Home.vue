<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <h1>欢迎使用 API 开放平台</h1>
          <p>在这里，你可以快速集成各种 API 接口，助力你的应用开发。</p>
          <el-button type="primary" size="large" @click="router.push('/user/my-apps')">开始集成</el-button>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8" v-for="i in 3" :key="i">
        <el-card shadow="hover">
          <template #header>
            <div class="api-card-header">
              <span>热门接口 {{ i }}</span>
              <el-tag size="small">免费</el-tag>
            </div>
          </template>
          <div class="api-card-content">
            这是一个示例接口，提供数据查询服务。
          </div>
          <div class="api-card-footer">
            <el-button link type="primary">查看详情</el-button>
          </div>
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
  padding: 40px 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}
.welcome-card h1 {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 20px;
}
.welcome-card p {
  font-size: 18px;
  color: #606266;
  margin-bottom: 30px;
}
.api-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.api-card-content {
  color: #909399;
  min-height: 60px;
}
.api-card-footer {
  text-align: right;
  margin-top: 15px;
}
</style>

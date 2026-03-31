<template>
  <el-container class="layout-container">
    <el-header class="user-header">
      <div class="header-content">
        <div class="logo" @click="router.push('/user/home')">
          <span class="logo-text">API 开放平台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          class="user-menu"
        >
          <el-menu-item index="/user/home">主页</el-menu-item>
          <el-menu-item index="/user/api-market">API 市场</el-menu-item>
          <el-menu-item index="/user/my-interfaces">我的接口</el-menu-item>
          <el-menu-item index="/user/my-apps">我的应用</el-menu-item>
          <el-menu-item index="/user/files">文件中心</el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/dashboard">后台管理</el-menu-item>
        </el-menu>
        <div class="user-info">
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              {{ userStore.userInfo.username || '游客' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu v-if="userStore.isLoggedIn">
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
              <el-dropdown-menu v-else>
                <el-dropdown-item command="login">登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    <el-main class="user-main">
      <div class="content-wrapper">
        <router-view />
      </div>
    </el-main>
    <el-footer class="user-footer">
      API 开放平台 ©2026 Created by Developer
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'login') {
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.user-header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  padding: 0;
  height: 60px;
}
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}
.logo {
  cursor: pointer;
  display: flex;
  align-items: center;
}
.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}
.user-menu {
  flex: 1;
  border-bottom: none;
  margin-left: 40px;
}
.user-info {
  display: flex;
  align-items: center;
}
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}
.user-main {
  background-color: #f0f2f5;
  padding: 20px 0;
  flex: 1;
}
.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.user-footer {
  text-align: center;
  color: #909399;
  padding: 20px 0;
  background-color: #f0f2f5;
}
</style>

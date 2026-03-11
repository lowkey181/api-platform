<template>
  <div class="my-apps-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的应用</span>
          <el-button type="primary" @click="dialogVisible = true">申请新应用</el-button>
        </div>
      </template>
      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="appName" label="应用名" />
        <el-table-column prop="accessKey" label="Access Key" />
        <el-table-column prop="secretKey" label="Secret Key" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="copyKey(scope.row.accessKey)">复制 AK</el-button>
            <el-button size="small" type="primary" link @click="copyKey(scope.row.secretKey)">复制 SK</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="申请应用" width="30%">
      <el-form :model="appForm">
        <el-form-item label="应用名称">
          <el-input v-model="appForm.appName" placeholder="请输入应用名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateApp" :loading="loading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { appApi } from '@/api/app'

const tableData = ref<any[]>([
  { appName: '测试应用', accessKey: 'ak_123456', secretKey: 'sk_654321' },
])
const dialogVisible = ref(false)
const loading = ref(false)
const appForm = reactive({
  appName: '',
})

const copyKey = (text: string) => {
  navigator.clipboard.writeText(text)
  ElMessage.success('复制成功')
}

const handleCreateApp = async () => {
  if (!appForm.appName) {
    ElMessage.warning('请输入应用名称')
    return
  }
  loading.value = true
  try {
    const res = await appApi.createApp(appForm.appName)
    ElMessage.success('创建成功')
    tableData.value.push(res.data)
    dialogVisible.value = false
    appForm.appName = ''
  } catch (error) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

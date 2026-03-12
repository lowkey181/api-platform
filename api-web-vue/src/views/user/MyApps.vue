<template>
  <div class="my-apps-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的应用</span>
          <el-button type="primary" @click="dialogVisible = true">申请新应用</el-button>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="appName" label="应用名" />
        <el-table-column prop="accessKey" label="Access Key" show-overflow-tooltip />
        <el-table-column prop="secretKey" label="Secret Key" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
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
          <el-button type="primary" @click="handleCreateApp" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { appApi } from '@/api/app'

const tableData = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const appForm = reactive({
  appName: '',
})

const loadApps = async () => {
  loading.value = true
  try {
    const res = await appApi.listApps()
    tableData.value = res.data || []
  } catch (error) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const copyKey = (text: string) => {
  navigator.clipboard.writeText(text)
  ElMessage.success('复制成功')
}

const handleCreateApp = async () => {
  if (!appForm.appName) {
    ElMessage.warning('请输入应用名称')
    return
  }
  submitLoading.value = true
  try {
    await appApi.createApp(appForm.appName)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    appForm.appName = ''
    loadApps()
  } catch (error) {
    // Error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  loadApps()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

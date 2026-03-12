<template>
  <div class="my-interfaces-container">
    <el-card>
      <template #header>
        <span>我的接口授权</span>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="interfaceId" label="接口ID" width="100" />
        <el-table-column prop="interfaceName" label="接口名称" />
        <el-table-column prop="maxCallCount" label="总调用次数" />
        <el-table-column prop="usedCallCount" label="已使用次数" />
        <el-table-column label="剩余次数">
          <template #default="scope">
            <el-text :type="scope.row.maxCallCount - scope.row.usedCallCount > 100 ? 'success' : 'warning'">
              {{ scope.row.maxCallCount - scope.row.usedCallCount }}
            </el-text>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.expireTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="handleCall(scope.row)"
              :disabled="scope.row.status !== 1 || scope.row.maxCallCount <= scope.row.usedCallCount"
            >
              调用测试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await authApi.selectPage(pageNum.value, pageSize.value)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleCall = async (row: any) => {
  try {
    const res = await authApi.callApi(userStore.userInfo.id, row.interfaceId)
    ElMessage.success('调用成功')
    loadData() // 刷新数据
  } catch (error) {
    // Error handled by interceptor
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-interfaces-container {
  max-width: 1200px;
  margin: 0 auto;
}
</style>

<template>
  <div class="my-interfaces-container">
    <el-card>
      <template #header>
        <span>我的接口授权</span>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="interfaceId" label="接口ID" width="100" />
        <el-table-column prop="name" label="接口名称" />
        <el-table-column prop="description" label="接口描述" />
        <el-table-column prop="requestParams" label="请求参数" />
        <el-table-column prop="responseResult" label="响应体" />
        <el-table-column prop="url" label="接口 url" show-overflow-tooltip>
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ scope.row.url }}</span>
              <el-button 
                type="primary" 
                size="small" 
                @click="handleCopy(scope.row.url)"
                :icon="CopyDocument"
                circle
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="请求方法" />
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

    <!-- 请求参数输入对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="输入请求参数"
      width="500px"
    >
      <el-form>
        <el-form-item label="请求参数">
          <el-input
            v-model="requestParams"
            type="textarea"
            :rows="8"
            placeholder='{"name":"aa"}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCall" :loading="callLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'
import { invokeApi } from '@/api/invoke'
import { signApi } from '@/api/sign'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)
const callLoading = ref(false)
const dialogVisible = ref(false)
const requestParams = ref('')
const currentRow = ref<any>(null)


const handleCopy = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('复制成功')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}
const loadData = async () => {
  loading.value = true
  try {
    const res = await authApi.selectPage(pageNum.value, pageSize.value)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    // 拦截器处理的错误
  } finally {
    loading.value = false
  }
}

const handleCall = async (row: any) => {
  // 检查是否有请求参数
  if (row.requestParams) {
    // 有请求参数，弹出对话框让用户输入
    currentRow.value = row
    requestParams.value = ''
    dialogVisible.value = true
  } else {
    // 没有请求参数，直接调用
    await executeCall(row)
  }
}

const confirmCall = async () => {
  if (!currentRow.value) return
  
  // 验证 JSON 格式
  try {
    if (requestParams.value.trim()) {
      JSON.parse(requestParams.value)
    }
  } catch (error) {
    ElMessage.error('请求参数格式错误，请输入有效的 JSON 格式')
    return
  }
  
  await executeCall(currentRow.value)
}

const executeCall = async (row: any) => {
  try {
    // 1. 每次调用时重新生成 timestamp 和 nonce，并获取新的 sign
    const signRes = await signApi.generate()
    if (!signRes.data) {
      ElMessage.error('获取签名失败')
      return
    }
    
    const { accessKey, timestamp, nonce, sign } = signRes.data
    const Authorization = `Bearer ${userStore.token}`
    
    if (!accessKey || !sign || !timestamp || !nonce) {
      ElMessage.error('签名信息不完整')
      return
    }
    
    // 2. 通过后端代理调用接口（后端通过 RestTemplate 调用网关）
    const response = await invokeApi.invoke(row.interfaceId, accessKey, sign, timestamp, nonce, Authorization, requestParams.value || undefined)
    
    ElMessage.success('调用成功')
    // 显示调用结果
    if (response.data) {
      const resultStr = JSON.stringify(response.data).substring(0, 100)
      ElMessage.info(`返回结果：${resultStr}...`)
    }
    loadData() // 刷新数据
    dialogVisible.value = false
    currentRow.value = null
    requestParams.value = ''
  } catch (error: any) {
    ElMessage.error(`调用失败：${error.message || '未知错误'}`)
  } finally {
    callLoading.value = false
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

<template>
  <div class="api-market-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>API 接口市场</span>
          <el-input
            v-model="searchText"
            placeholder="搜索接口"
            style="width: 300px"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-row :gutter="20" v-loading="loading">
        <el-col :span="8" v-for="item in interfaceList" :key="item.id">
          <el-card shadow="hover" class="api-card">
            <template #header>
              <div class="api-card-header">
                <span class="api-name">{{ item.name }}</span>
                <el-tag :type="item.status === 1 ? 'success' : 'danger'" size="small">
                  {{ item.status === 1 ? '可用' : '不可用' }}
                </el-tag>
              </div>
            </template>
            <div class="api-card-content">
              <p class="api-description">{{ item.description || '暂无描述' }}</p>
              <div class="api-info">
                <el-text type="info" size="small">{{ item.method }}</el-text>
                <el-text type="info" size="small">{{ item.url }}</el-text>
              </div>
            </div>
            <div class="api-card-footer">
              <el-button type="primary" @click="viewProducts(item)" :disabled="item.status !== 1">
                查看套餐
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadInterfaces"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 套餐选择对话框 -->
    <el-dialog v-model="productDialogVisible" :title="`${currentInterface?.name} - 套餐列表`" width="60%">
      <el-table :data="productList" v-loading="productLoading">
        <el-table-column prop="productName" label="套餐名称" />
        <el-table-column prop="callCount" label="调用次数" />
        <el-table-column prop="price" label="价格（元）">
          <template #default="scope">
            ¥{{ scope.row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleBuy(scope.row)">
              购买
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { interfaceApi } from '@/api/interface'
import { productApi } from '@/api/product'
import { orderApi } from '@/api/order'

const searchText = ref('')
const pageNum = ref(1)
const pageSize = ref(9)
const total = ref(0)
const interfaceList = ref<any[]>([])
const loading = ref(false)

const productDialogVisible = ref(false)
const productList = ref<any[]>([])
const productLoading = ref(false)
const currentInterface = ref<any>(null)

const loadInterfaces = async () => {
  loading.value = true
  try {
    const res = await interfaceApi.selectPage(pageNum.value, pageSize.value, 1)
    interfaceList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const viewProducts = async (item: any) => {
  currentInterface.value = item
  productDialogVisible.value = true
  productLoading.value = true
  try {
    const res = await productApi.selectPage(1, 100, item.id, 1)
    productList.value = res.data.records || []
  } catch (error) {
    // Error handled by interceptor
  } finally {
    productLoading.value = false
  }
}

const handleBuy = async (product: any) => {
  try {
    const res = await orderApi.createOrder(product.id)
    // 支付宝返回的是HTML表单，直接渲染到页面
    const div = document.createElement('div')
    div.innerHTML = res.data
    document.body.appendChild(div)
    const form = div.querySelector('form')
    if (form) {
      form.submit()
    }
  } catch (error) {
    ElMessage.error('创建订单失败')
  }
}

onMounted(() => {
  loadInterfaces()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.api-card {
  margin-bottom: 20px;
  height: 280px;
  display: flex;
  flex-direction: column;
}

.api-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.api-name {
  font-weight: bold;
  font-size: 16px;
}

.api-card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.api-description {
  color: #606266;
  margin-bottom: 15px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.api-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.api-card-footer {
  text-align: right;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}
</style>

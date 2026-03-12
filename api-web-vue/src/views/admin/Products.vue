<template>
  <div class="products-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>产品套餐管理</span>
          <el-button type="primary" @click="handleAdd">新增套餐</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="productName" label="套餐名称" />
        <el-table-column prop="interfaceId" label="接口ID" width="100" />
        <el-table-column prop="callCount" label="调用次数" />
        <el-table-column prop="price" label="价格（元）">
          <template #default="scope">
            ¥{{ scope.row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="套餐名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="接口ID" prop="interfaceId">
          <el-input-number v-model="form.interfaceId" :min="1" placeholder="请输入接口ID" />
        </el-form-item>
        <el-form-item label="调用次数" prop="callCount">
          <el-input-number v-model="form.callCount" :min="1" placeholder="请输入调用次数" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" placeholder="请输入价格" />
        </el-form-item>
        <el-form-item label="说明" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入套餐说明" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { productApi } from '@/api/product'

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增套餐')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined,
  productName: '',
  interfaceId: undefined,
  callCount: undefined,
  price: undefined,
  remark: '',
  status: 1,
})

const rules = reactive<FormRules>({
  productName: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  interfaceId: [{ required: true, message: '请输入接口ID', trigger: 'blur' }],
  callCount: [{ required: true, message: '请输入调用次数', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await productApi.selectPage(pageNum.value, pageSize.value)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增套餐'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑套餐'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该套餐吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await productApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    // Cancelled or error
  }
}

const handleStatusChange = async (row: any) => {
  try {
    await productApi.updateStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
  } catch (error) {
    // Revert status on error
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (form.id) {
          await productApi.update(form)
          ElMessage.success('更新成功')
        } else {
          await productApi.insert(form)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        // Error handled by interceptor
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const resetForm = () => {
  form.id = undefined
  form.productName = ''
  form.interfaceId = undefined
  form.callCount = undefined
  form.price = undefined
  form.remark = ''
  form.status = 1
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

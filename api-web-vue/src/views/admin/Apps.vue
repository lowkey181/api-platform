<template>
  <div class="apps-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>应用列表</span>
          <el-button type="primary">新增应用</el-button>
        </div>
      </template>
      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="appName" label="应用名" />
        <el-table-column prop="accessKey" label="Access Key" />
        <el-table-column prop="secretKey" label="Secret Key" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tableData = ref([
  { id: 1, appName: 'Demo App', accessKey: 'ak_123456', secretKey: 'sk_654321', status: 1 },
])

const handleEdit = (row: any) => {
  console.log('Edit', row)
}

const handleDelete = (row: any) => {
  console.log('Delete', row)
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

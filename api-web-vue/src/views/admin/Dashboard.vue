<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>用户总数</template>
          <div class="card-content">1,234</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>今日调用</template>
          <div class="card-content">567</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>应用数量</template>
          <div class="card-content">89</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.card-content {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
}
</style>

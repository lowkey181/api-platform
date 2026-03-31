<template>
  <div class="file-manage-container">
    <el-card class="manage-card">
      <template #header>
        <div class="card-header">
          <span>文件中心</span>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="文件类型">
            <el-select v-model="filterForm.prefix" placeholder="全部类型" clearable filterable @change="loadFiles">
              <el-option label="全部" value="" />
              <el-option label="图片" value="images/" />
              <el-option label="视频" value="videos/" />
              <el-option label="压缩包" value="archives/" />
              <el-option label="其他" value="other/" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @change="loadFiles">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 文件列表 -->
      <el-table
        v-loading="loading"
        :data="fileList"
        border
        style="width: 100%; margin-top: 20px"
      >
        <el-table-column type="index" label="序号" width="60" />
        
        <el-table-column label="预览" width="100" >
          <template #default="{ row }" >
            <div class="file-preview">
              <img
                v-if="isImage(row.fileUrl)"
                :src="row.fileUrl"
                alt="预览"
                style="width: 50px; height: 50px; object-fit: cover; cursor: pointer"
                @click="previewFile(row.fileUrl)"
              />
              <el-icon
                v-else-if="isVideo(row.fileUrl)"
                size="40"
                color="#409EFF"
                style="cursor: pointer"
                @click="previewFile(row.fileUrl)"
              >
                <VideoCamera />
              </el-icon>
              <el-icon v-else size="40" color="#909399">
                <Document />
              </el-icon>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column prop="fileType" label="类型" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.fileType?.includes('image')" type="success">
              {{ getFileTypeName(row.fileType, row.fileUrl) }}
            </el-tag>
            <el-tag v-else-if="row.fileType?.includes('video')" type="warning">
              {{ getFileTypeName(row.fileType, row.fileUrl) }}
            </el-tag>
            <el-tag v-else-if="row.fileType?.includes('zip') || row.fileType?.includes('compressed')" type="info">
              {{ getFileTypeName(row.fileType, row.fileUrl) }}
            </el-tag>
            <el-tag v-else>{{ getFileTypeName(row.fileType, row.fileUrl) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180" />

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="previewFile(row.fileUrl)" v-if="isImage(row.fileUrl)||isVideo(row.fileUrl)||isPdf(row.fileUrl)">
              预览
            </el-button>
            <el-button link type="primary" @click="downloadFile(row.fileUrl)">
              下载
            </el-button>
            <el-button link type="primary" @click="copyUrl(row.fileUrl)">
              复制链接
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-section">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadFiles"
          @current-change="loadFiles"
        />
      </div>
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" title="文件预览" width="80%">
      <div v-if="isImage(previewUrl)" class="preview-image">
        <img :src="previewUrl" alt="预览" style="max-width: 100%" />
      </div>
      <div v-else-if="isVideo(previewUrl)" class="preview-video">
        <video :src="previewUrl" controls style="width: 100%"></video>
      </div>
      <div v-else-if="isPdf(previewUrl)" class="preview-pdf">
        <iframe :src="previewUrl" width="100%" height="600px" style="border: none"></iframe>
      </div>
      <div v-else class="preview-other">
        <el-result icon="info" title="该文件类型不支持在线预览">
          <template #extra>
            <el-button type="primary" @click="downloadFile(previewUrl)">
              下载文件
            </el-button>
          </template>
        </el-result>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoCamera, Document } from '@element-plus/icons-vue'
import { fileApi, type FileInfo } from '@/api/file'

const loading = ref(false)
const fileList = ref<FileInfo[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const previewVisible = ref(false)
const previewUrl = ref('')

const filterForm = reactive({
  prefix: ''
})

// 加载文件列表
const loadFiles = async () => {
  loading.value = true
  try {
    const res = await fileApi.listFiles({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      prefix: filterForm.prefix
    })
    
    fileList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选
const resetFilter = () => {
  filterForm.prefix = ''
  pageNum.value = 1
  loadFiles()
}

// 判断是否为图片
const isImage = (url: string) => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp']
  return imageExtensions.some(ext => url.toLowerCase().endsWith(ext))
}

// 判断是否为视频
const isVideo = (url: string) => {
  const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv']
  return videoExtensions.some(ext => url.toLowerCase().endsWith(ext))
}

// 判断是否为 PDF
const isPdf = (url: string) => {
  return url.toLowerCase().endsWith('.pdf')
}
const isDocx = (url: string) => {
  return url.toLowerCase().endsWith('.docx')
}

// 预览文件
const previewFile = (url: string) => {
  previewUrl.value = url
  previewVisible.value = true
}

// 下载文件
const downloadFile = (url: string) => {
  window.open(url, '_blank')
}

// 复制链接
const copyUrl = (url: string) => {
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 格式化文件大小
const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

// 获取文件类型名称
const getFileTypeName = (fileType?: string, url?: string) => {
  if (fileType) {
    if (fileType.includes('image')) return '图片'
    if (fileType.includes('video')) return '视频'
    if (fileType.includes('zip') || fileType.includes('compressed')) return '压缩包'
    if (fileType.includes('pdf')) return 'PDF'
    if (fileType.includes('text')) return '文本'
    if (fileType.includes('word')) return 'Word'
    if (fileType.includes('docx')) return 'DOCX'
    if (fileType.includes('doc')) return 'DOC'
  }
  
  // 根据 URL 判断
  if (url) {
    if (isImage(url)) return '图片'
    if (isVideo(url)) return '视频'
    if (isPdf(url)) return 'PDF'
    if (isDocx(url)) return 'DOCX'
    if (url.toLowerCase().endsWith('.zip') || url.toLowerCase().endsWith('.rar')) return '压缩包'
  }
  
  return '其他'
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped>
.file-manage-container {
  padding: 20px;
}

.manage-card {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-section {
  margin-bottom: 20px;
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
}

.pagination-section {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.preview-image {
  text-align: center;
}

.preview-video {
  text-align: center;
}

.preview-pdf {
  width: 100%;
  height: 600px;
}

.preview-other {
  padding: 40px 0;
}
</style>

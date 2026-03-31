<template>
  <div class="file-upload-container">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>文件上传</span>
        </div>
      </template>

      <!-- 大文件上传（支持断点续传） -->
      <div class="chunk-upload-section">
        <h3>🚀 大文件上传（支持断点续传、秒传）</h3>
        <p class="tip">说明：适合上传大文件（>50MB），自动分片上传，支持暂停和恢复</p>
        <ChunkFileUploader
            v-model="uploadedUrl"
            :chunk-size="2 * 1024 * 1024"
        />
        <div v-if="uploadedUrl" class="upload-result">
          <el-alert
              title="上传成功"
              type="success"
              :closable="false"
          >
            <template #default>
              <div class="result-content">
                <span>文件 URL: </span>
                <el-tag>{{ uploadedUrl }}</el-tag>
                <el-button size="small" @click="copyUploadedUrl">复制 URL</el-button>
              </div>
            </template>
          </el-alert>
        </div>
      </div>

      <el-divider />

      <!-- 上传类型选择 -->
      <el-radio-group v-model="uploadType" class="mb-4">
        <el-radio-button value="single">单文件上传</el-radio-button>
        <el-radio-button value="multiple">批量上传</el-radio-button>
      </el-radio-group>

      <!-- 单文件上传 -->
      <div v-if="uploadType === 'single'" class="upload-area">
        <el-upload
          ref="singleUploadRef"
          class="upload-demo"
          drag
          :on-change="handleFileChange"
          :before-upload="beforeUpload"
          :http-request="uploadSingleFile"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持图片、视频、压缩包等格式，单个文件不超过 50MB
            </div>
          </template>
        </el-upload>
      </div>

      <!-- 批量上传 -->
      <div v-else class="upload-area">
        <el-upload
          ref="multipleUploadRef"
          class="upload-demo"
          drag
          :on-change="handleFileChange"
          :before-upload="beforeUpload"
          :limit="9"
          :http-request="uploadMultipleFiles"
          multiple
          accept=".zip,.rar,.7z,.gz,.tar,.jpg,.jpeg,.png,.gif,.webp,.bmp,.mp4,.avi,.mov,.wmv,.flv,.pdf,.doc,.docx,.txt"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持图片、视频、压缩包等格式，最多上传 9 个文件，每个不超过 50MB
            </div>
          </template>
        </el-upload>
      </div>

      <!-- 上传结果展示 -->
      <div v-if="uploadedFiles.length > 0" class="uploaded-list">
        <h3>已上传文件（{{ uploadedFiles.length }}）</h3>
        <el-table :data="uploadedFiles" border style="margin-top: 10px">
          <el-table-column prop="name" label="文件名" />
          <el-table-column prop="size" label="大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.size) }}
            </template>
          </el-table-column>
          <el-table-column label="预览" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="previewFile(row.url)">
                预览
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="copyUrl(row.url)">
                复制链接
              </el-button>
              <el-button link type="danger" @click="deleteUploadedFile(row.url)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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
      <div v-else class="preview-other">
        <el-result icon="info" title="该文件类型不支持预览">
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
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'
import type { UploadRequestOptions } from 'element-plus'

const uploadType = ref('single')
const singleUploadRef = ref()
const multipleUploadRef = ref()
const uploadedFiles = ref<Array<{ name: string; size: number; url: string }>>([])
const previewVisible = ref(false)
const previewUrl = ref('')
const uploading = ref(false)
import ChunkFileUploader from '@/components/ChunkFileUploader.vue'

const uploadedUrl = ref('')
// 处理文件变化
const handleFileChange = () => {
  // 可以在这里处理文件选择后的逻辑
}
// 上传前验证
const beforeUpload = (file: any) => {
  const maxSize = 500 * 1024 * 1024 // 500MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 500MB')
    return false
  }
  return true
}
// 上传单个文件
const uploadSingleFile = async (options: UploadRequestOptions) => {
  uploading.value = true
  try {
    const response = await fileApi.uploadFile(options.file)
    if (response.code === 200) {
      ElMessage.success('上传成功')
      uploadedFiles.value.push({
        name: options.file.name,
        size: options.file.size,
        url: response.data
      })
      
      // 重置上传组件
      setTimeout(() => {
        singleUploadRef.value?.clearFiles()
      }, 100)
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

// 批量上传文件
const uploadMultipleFiles = async (options: UploadRequestOptions) => {
  uploading.value = true
  try {
    const response = await fileApi.uploadFiles([options.file])
    if (response.code === 200) {
      ElMessage.success('上传成功')
      const urls = response.data || []
      urls.forEach((url: string) => {
        uploadedFiles.value.push({
          name: options.file.name,
          size: options.file.size,
          url
        })
      })
      
      // 重置上传组件
      setTimeout(() => {
        multipleUploadRef.value?.clearFiles()
      }, 100)
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
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

// 预览文件
const previewFile = (url: string) => {
  previewUrl.value = url
  previewVisible.value = true
}

// 复制 URL
const copyUrl = (url: string) => {
  navigator.clipboard.writeText(url)
  ElMessage.success('复制成功')
}

// 删除已上传的文件
const deleteUploadedFile = async (url: string) => {
  try {
    await ElMessageBox.confirm('确定要删除此文件吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 调用后端删除接口
    await fileApi.deleteFile(url)
    
    uploadedFiles.value = uploadedFiles.value.filter(f => f.url !== url)
    ElMessage.success('删除成功')
  } catch (error) {
    // User cancelled
  }
}

// 下载文件
const downloadFile = (url: string) => {
  window.open(url, '_blank')
}

// 复制上传成功的 URL
const copyUploadedUrl = async () => {
  if (!uploadedUrl.value) return
  try {
    await navigator.clipboard.writeText(uploadedUrl.value)
    ElMessage.success('复制成功')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.file-upload-container {
  padding: 20px;
}

.upload-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mb-4 {
  margin-bottom: 20px;
}

.upload-area {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.upload-demo {
  width: 100%;
  max-width: 600px;
}

.uploaded-list {
  margin-top: 30px;
}

.preview-image {
  text-align: center;
}

.preview-video {
  text-align: center;
}

.preview-other {
  padding: 40px 0;
}
</style>

<template>
  <div class="chunk-file-uploader">
    <el-upload
      v-model:file-list="fileList"
      :auto-upload="false"
      :on-change="handleFileChange"
      :multiple="multiple"
      :limit="limit"
      :accept="accept"
      list-type="picture-card"
      class="uploader"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>

    <!-- 上传进度对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="文件上传中" width="600px" :close-on-click-modal="false">
      <div v-for="(item, index) in uploadProgressList" :key="index" class="upload-item">
        <div class="upload-info">
          <span>{{ item.fileName }}</span>
          <span class="upload-percent">{{ item.percent }}%</span>
        </div>
        <el-progress :percentage="item.percent" :status="item.status" />
        <div v-if="item.status === 'success'" class="upload-result">
          <el-button link type="primary" @click="copyUrl(item.url)">复制链接</el-button>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="uploadDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import SparkMD5 from 'spark-md5'
import { chunkUploadApi } from '@/api/chunkUpload'
import type { UploadUserFile, UploadProps } from 'element-plus'

const props = defineProps({
  multiple: {
    type: Boolean,
    default: false
  },
  limit: {
    type: Number,
    default: 9
  },
  accept: {
    type: String,
    default: ''
  },
  chunkSize: {
    type: Number,
    default: 2 * 1024 * 1024 // 默认 2MB 一个分片
  }
})

const emit = defineEmits(['update:modelValue'])

const fileList = ref<UploadUserFile[]>([])
const uploadDialogVisible = ref(false)
const uploadProgressList = ref<Array<{
  fileName: string
  percent: number
  status: '' | 'success' | 'exception'
  url?: string
}>>([])

// 处理文件选择
const handleFileChange: UploadProps['onChange'] = (file) => {
  const rawFile = file.raw
  if (!rawFile) return
  
  // 开始分片上传
  startChunkUpload(rawFile)
}

// 计算文件 MD5
const calculateFileMD5 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const blobSlice = File.prototype.slice || (File as any).prototype.mozSlice || (File as any).prototype.webkitSlice
    const chunks: Blob[] = []
    let currentChunk = 0
    const chunkSize = props.chunkSize
    const chunksCount = Math.ceil(file.size / chunkSize)
    
    const spark = new SparkMD5.ArrayBuffer()
    
    function loadNext() {
      const start = currentChunk * chunkSize
      const end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize
      
      const reader = new FileReader()
      reader.onload = (e) => {
        spark.append(e.target?.result as ArrayBuffer)
        currentChunk++
        
        if (currentChunk < chunksCount) {
          loadNext()
        } else {
          resolve(spark.end())
        }
      }
      reader.onerror = () => reject(new Error('读取文件失败'))
      reader.readAsArrayBuffer(blobSlice.call(file, start, end))
    }
    
    loadNext()
  })
}

// 开始分片上传
const startChunkUpload = async (file: File) => {
  try {
    // 1. 计算文件 MD5
    ElMessage.info('正在计算文件指纹...')
    const fileMd5 = await calculateFileMD5(file)
    console.log('文件 MD5:', fileMd5)
    
    // 2. 计算分片数量
    const totalChunks = Math.ceil(file.size / props.chunkSize)
    
    // 3. 初始化上传
    const initRes = await chunkUploadApi.initChunkUpload(
      file.name,
      file.size,
      fileMd5,
      totalChunks
    )
    
    if (initRes.code !== 200) {
      ElMessage.error(initRes.msg || '初始化失败')
      return
    }
    
    const initData = initRes.data as any
    
    // 检查是否秒传成功
    if (initData.isQuickUpload) {
      ElMessage.success('✨ 秒传成功！文件已存在')
      addUploadResult(file.name, 100, 'success', initData)
      return
    }
    
    const uploadId = initData.uploadId
    
    // 4. 添加进度条目
    const progressIndex = uploadProgressList.value.length
    uploadProgressList.value.push({
      fileName: file.name,
      percent: 0,
      status: ''
    })
    
    uploadDialogVisible.value = true
    
    // 5. 查询已上传的分片（断点续传）
    const uploadedChunksRes = await chunkUploadApi.getUploadedChunks(uploadId)
    const uploadedChunks = new Set<number>(uploadedChunksRes.data as number[])
    console.log('已上传的分片:', uploadedChunks)
    
    // 6. 上传分片
    const blobSlice = File.prototype.slice || (File as any).prototype.mozSlice || (File as any).prototype.webkitSlice
    let uploadedCount = uploadedChunks.size
    
    for (let i = 0; i < totalChunks; i++) {
      // 跳过已上传的分片
      if (uploadedChunks.has(i)) {
        updateProgress(progressIndex, Math.round(((i + 1) / totalChunks) * 100), '')
        continue
      }
      
      const start = i * props.chunkSize
      const end = Math.min(start + props.chunkSize, file.size)
      const chunk = blobSlice.call(file, start, end)
      
      // 上传分片
      const uploadRes = await chunkUploadApi.uploadChunk(uploadId, i, fileMd5, chunk)
      
      if (uploadRes.code !== 200) {
        throw new Error(`分片 ${i} 上传失败：${uploadRes.msg}`)
      }
      
      uploadedCount++
      const percent = Math.round((uploadedCount / totalChunks) * 100)
      updateProgress(progressIndex, percent, '')
    }
    
    // 7. 合并分片
    ElMessage.info('正在合并文件...')
    const mergeRes = await chunkUploadApi.mergeChunks(
      uploadId,
      file.name,
      fileMd5,
      file.size,
      totalChunks,
      file.type
    )
    
    if (mergeRes.code !== 200) {
      throw new Error(mergeRes.msg || '合并失败')
    }
    
    // 8. 更新状态为成功
    updateProgress(progressIndex, 100, 'success', mergeRes.data as string)
    ElMessage.success('✓ 上传成功')
    
    // 通知父组件
    emit('update:modelValue', mergeRes.data)
    
  } catch (error: any) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败：' + error.message)
  }
}

// 更新进度
const updateProgress = (index: number, percent: number, status: '' | 'success' | 'exception', url?: string) => {
  if (uploadProgressList.value[index]) {
    uploadProgressList.value[index].percent = percent
    uploadProgressList.value[index].status = status
    if (url) {
      uploadProgressList.value[index].url = url
    }
  }
}

// 添加上传结果
const addUploadResult = (fileName: string, percent: number, status: 'success', url: string) => {
  uploadProgressList.value.push({
    fileName,
    percent,
    status,
    url
  })
  uploadDialogVisible.value = true
}

// 复制 URL
const copyUrl = async (url?: string) => {
  if (!url) return
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('复制成功')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.chunk-file-uploader {
  width: 100%;
}

.uploader :deep(.el-upload) {
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.uploader :deep(.el-upload:hover) {
  border-color: var(--el-color-primary);
}

.upload-item {
  margin-bottom: 20px;
}

.upload-item:last-child {
  margin-bottom: 0;
}

.upload-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.upload-percent {
  font-size: 14px;
  color: var(--el-color-primary);
}

.upload-result {
  margin-top: 8px;
  text-align: right;
}
</style>

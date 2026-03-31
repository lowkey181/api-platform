<template>
  <div class="file-uploader">
    <el-upload
        v-model:file-list="fileList"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="uploadData"
        :on-success="handleSuccess"
        :on-error="handleError"
        :before-upload="beforeUpload"
        :on-remove="handleRemove"
        :multiple="multiple"
        :limit="limit"
        :accept="accept"
        list-type="picture-card"
        class="uploader"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>

    <!-- 预览对话框 -->
    <el-dialog v-model="dialogVisible">
      <img w-full :src="previewUrl" alt="预览" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import type { UploadUserFile, UploadProps } from 'element-plus'
import CryptoJS from 'crypto-js'

const props = defineProps({
  modelValue: {
    type: [String, Array] as any,
    default: ''
  },
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
    default: 'image/*,video/*,.zip,.rar,.7z,.gz,.tar'
  },
  maxFileSize: {
    type: Number,
    default: 50 * 1024 * 1024 // 50MB
  }
})
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const fileList = ref<UploadUserFile[]>([])
const dialogVisible = ref(false)
const previewUrl = ref('')
const fileMd5Map = ref<Map<string, string>>(new Map())

const uploadUrl = computed(() => {
  return import.meta.env.VITE_API_BASE_URL + '/api/file/upload'
})

const headers = computed(() => {
  return {
    'Authorization': `Bearer ${userStore.token}`
  }
})

const uploadHeaders = computed(() => {
  return headers.value
})

const uploadData = computed(() => {
  return {
    fileMd5: fileMd5Map.value.get('current') || ''
  }
})

// 初始化回显
if (props.modelValue) {
  const urls = Array.isArray(props.modelValue) ? props.modelValue : [props.modelValue]
  fileList.value = urls.map((url: string, index: number) => ({
    name: `file-${index}`,
    url
  }))
}

// 计算文件 MD5
const calculateFileMD5 = async (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = function(e) {
      try {
        const arrayBuffer = e.target?.result as ArrayBuffer
        const wordArray = CryptoJS.lib.WordArray.create(arrayBuffer)
        const hash = CryptoJS.MD5(wordArray)
        resolve(hash.toString(CryptoJS.enc.Hex))
      } catch (error) {
        reject(error)
      }
    }

    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}


const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  if (file.size > props.maxFileSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxFileSize / 1024 / 1024}MB`)
    return false
  }

  // 可选：额外验证压缩包扩展名
  const fileName = file.name.toLowerCase()
  const allowedCompressedExtensions = ['.zip', '.rar', '.7z', '.gz', '.tar', '.tgz']
  const hasAllowedExtension = allowedCompressedExtensions.some(ext => fileName.endsWith(ext))

  if (hasAllowedExtension && file.size > props.maxFileSize) {
    ElMessage.error('压缩包大小不能超过 50MB')
    return false
  }

  // 计算文件 MD5（用于秒传）
  try {
    ElMessage.info('正在计算文件指纹...')
    const md5 = await calculateFileMD5(file as File)
    fileMd5Map.value.set('current', md5)
    console.log('文件 MD5:', md5)
  } catch (error) {
    console.error('MD5 计算失败:', error)
    // MD5 计算失败不影响上传，后端会重新计算
  }

  return true
}


const handleSuccess = (response: any, file: UploadUserFile) => {
  console.log('上传成功：', response)
  if (response.code === 200|| response.) {
    console.log('秒传成功！文件已存在')
    ElMessage.success('秒传成功！文件已存在')
    if (response.isQuickUpload) {
      ElMessage.success('秒传成功！文件已存在')
    } else {
      ElMessage.success('上传成功')
    }
    const urls = fileList.value.map(f => f.url).filter(Boolean) as string[]
    emit('update:modelValue', props.multiple ? urls : urls[0])
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleError = () => {
  ElMessage.error('上传失败')
}

const handleRemove: UploadProps['onRemove'] = () => {
  const urls = fileList.value.map(f => f.url).filter(Boolean) as string[]
  emit('update:modelValue', props.multiple ? urls : urls[0])
}

const handlePreview = (file: UploadUserFile) => {
  if (file.url) {
    previewUrl.value = file.url
    dialogVisible.value = true
  }
}
</script>

<style scoped>
.file-uploader {
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
</style>

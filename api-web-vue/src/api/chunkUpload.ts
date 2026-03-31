import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const chunkUploadApi = {
  // 初始化分片上传
  initChunkUpload: (fileName: string, fileSize: number, fileMd5: string, totalChunks: number) => {
    return request.post<Result>('/api/file/chunk/init', null, {
      params: {
        fileName,
        fileSize,
        fileMd5,
        totalChunks
      }
    })
  },

  // 上传分片
  uploadChunk: (uploadId: string, chunkIndex: number, fileMd5: string, chunk: Blob) => {
    const formData = new FormData()
    formData.append('uploadId', uploadId)
    formData.append('chunkIndex', chunkIndex.toString())
    formData.append('fileMd5', fileMd5)
    formData.append('chunk', chunk)

    return request.post<Result>('/api/file/chunk/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 合并分片
  mergeChunks: (
    uploadId: string,
    fileName: string,
    fileMd5: string,
    fileSize: number,
    totalChunks: number,
    fileType?: string
  ) => {
    return request.post<Result>('/api/file/chunk/merge', null, {
      params: {
        uploadId,
        fileName,
        fileMd5,
        fileSize,
        totalChunks,
        fileType
      }
    })
  },

  // 查询已上传的分片
  getUploadedChunks: (uploadId: string) => {
    return request.get<Result>(`/api/file/chunk/chunks/${uploadId}`)
  },
}

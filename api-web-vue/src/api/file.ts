import request from '@/utils/request'
import type { Result } from '@/utils/request'

export interface FileInfo {
  id?: number
  fileName: string
  fileUrl: string
  fileSize: number
  fileType?: string
  storageType?: string
  objectName?: string
  uploaderId?: number
  createTime?: string
  updateTime?: string
}

export const fileApi = {
  /**
   * 上传单文件
   */
  async uploadFile(file: File): Promise<Result<string>> {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request<Result<string>>({
      url: '/api/file/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return res.data
  },

  /**
   * 批量上传文件
   */
  async uploadFiles(files: File[]): Promise<Result<string[]>> {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    const res = await request<Result<string[]>>({
      url: '/api/file/uploads',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return res.data
  },

  /**
   * 删除文件
   */
  deleteFile(url: string) {
    return request<Result>({
      url: '/api/file/delete',
      method: 'delete',
      params: { url }
    })
  },

  /**
   * 分页查询文件列表
   */
  listFiles(params: {
    pageNum?: number
    pageSize?: number
    prefix?: string
  }) {
    return request<Result<{
      list: FileInfo[]
      total: number
      pageNum: number
      pageSize: number
    }>>({
      url: '/api/file/list',
      method: 'get',
      params
    })
  },

  /**
   * 获取所有文件（不分页）
   */
  listAllFiles(prefix?: string) {
    return request<Result<FileInfo[]>>({
      url: '/api/file/all',
      method: 'get',
      params: { prefix }
    })
  },

  /**
   * 重命名文件
   */
  renameFile(oldObjectName: string, newFileName: string) {
    return request.put<Result>('/api/file/rename', null, {
      params: { oldObjectName, newFileName }
    })
  },
}

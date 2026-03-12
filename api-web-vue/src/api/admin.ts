import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const adminApi = {
  // 用户管理
  getUserList: (pageNum: number, pageSize: number) => {
    const params = new URLSearchParams()
    params.append('pageNum', pageNum.toString())
    params.append('pageSize', pageSize.toString())
    return request.post<Result>('/user/selectPage', params)
  },

  updateUserStatus: (id: number, status: number) => {
    const params = new URLSearchParams()
    params.append('id', id.toString())
    params.append('status', status.toString())
    return request.post<Result>('/user/updateStatus', params)
  },

  deleteUser: (id: number) => {
    const params = new URLSearchParams()
    params.append('id', id.toString())
    return request.post<Result>('/user/delete', params)
  },
}

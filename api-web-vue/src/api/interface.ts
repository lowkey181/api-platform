import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const interfaceApi = {
  // 分页查询接口列表
  selectPage: (pageNum: number, pageSize: number, status?: number) => {
    return request.get<Result>('/apiInterface/selectPage', {
      params: {
        pageNum,
        pageSize,
        status,
      },
    })
  },

  // 新增接口
  insert: (data: any) => {
    return request.post<Result>('/apiInterface/insert', data)
  },

  // 更新接口
  update: (data: any) => {
    return request.post<Result>('/apiInterface/update', data)
  },

  // 删除接口
  delete: (id: number) => {
    return request.get<Result>('/apiInterface/delete', {
      params: { id },
    })
  },
}

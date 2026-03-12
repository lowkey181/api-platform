import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const productApi = {
  // 分页查询产品列表
  selectPage: (pageNum: number, pageSize: number, interfaceId?: number, status?: number) => {
    return request.get<Result>('/api/product/selectPage', {
      params: {
        pageNum,
        pageSize,
        interfaceId,
        status,
      },
    })
  },

  // 新增产品
  insert: (data: any) => {
    return request.post<Result>('/api/product/insert', data)
  },

  // 更新产品
  update: (data: any) => {
    return request.post<Result>('/api/product/update', data)
  },

  // 删除产品
  delete: (id: number) => {
    return request.get<Result>('/api/product/delete', {
      params: { id },
    })
  },

  // 更新产品状态
  updateStatus: (id: number, status: number) => {
    return request.get<Result>('/api/product/updateStatus', {
      params: { id, status },
    })
  },
}

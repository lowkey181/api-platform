import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const interfaceApi = {
  // 分页查询接口列表
  selectPage: (pageNum: number, pageSize: number, status?: number, authorId?: number) => {
    return request.get<Result>('/apiInterface/selectPage', {
      params: {
        pageNum,
        pageSize,
        status,
        authorId,
      },
    })
  },

  // 查询单个接口详情
  getById: (id: number) => {
    return request.get<Result>(`/apiInterface/${id}`)
  },

  // 获取接口的输入/输出参数
  getParams: (interfaceId: number) => {
    return request.get<Result>(`/apiInterface/params/${interfaceId}`)
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

  // 保存接口参数（单次传入一个数组，前端分两次调用）
  saveParams: (interfaceId: number, params: any[]) => {
    return request.post<Result>(`/apiInterface/params/${interfaceId}`, params)
  },
}

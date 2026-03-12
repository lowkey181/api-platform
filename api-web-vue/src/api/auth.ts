import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const authApi = {
  // 分页查询用户接口授权
  selectPage: (pageNum: number, pageSize: number) => {
    return request.get<Result>('/userInterfaceAuth/selectPage', {
      params: {
        pageNum,
        pageSize,
      },
    })
  },

  // 调用API
  callApi: (userId: number, interfaceId: number) => {
    return request.post<Result>('/userInterfaceAuth/callApi', {
      userId,
      interfaceId,
    })
  },

  // 新增授权
  insert: (data: any) => {
    return request.post<Result>('/userInterfaceAuth/insert', data)
  },
}

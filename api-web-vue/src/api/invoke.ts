import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const invokeApi = {
  // 获取接口 URL（用于通过网关调用）
  getInterfaceUrl: (interfaceId: number) => {
    return request.get<Result>('/invoke/call', {
      params: { interfaceId },
    })
  },

  // 通过后端代理调用接口（后端通过 RestTemplate 调用网关）
  invoke: (interfaceId: number, accessKey: string, sign: string, timestamp: string, nonce: string, Authorization: string, body?: string) => {
    const params: any = { interfaceId, accessKey, sign, timestamp, nonce, Authorization }
    if (body) params.body = body
    return request.get<Result>('/invoke/invoke', { params })
  },

  // 后端代签代发，测试接口（body 作为 @RequestParam 传递）
  test: (interfaceId: number, body?: string) => {
    const params = new URLSearchParams()
    params.append('interfaceId', String(interfaceId))
    if (body) params.append('body', body)
    return request.post<Result>('/invoke/test', params)
  },
}

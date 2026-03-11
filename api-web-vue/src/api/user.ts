import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const userApi = {
  login: (data: any) => {
    const params = new URLSearchParams()
    params.append('username', data.username)
    params.append('password', data.password)
    return request.post<any, Result<string>>('/user/login', params)
  },
  register: (data: any) => {
    const params = new URLSearchParams()
    params.append('username', data.username)
    params.append('password', data.password)
    return request.post<Result>('/user/register', params)
  },
  logout: () => {
    return request.post<Result>('/user/logout')
  },
  getInfo: () => {
    return request.get<any, Result<any>>('/user/info')
  },
}

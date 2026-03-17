import axios from 'axios'
import type { Result } from '@/utils/request'

// 创建独立的 axios 实例，避免循环依赖
const signService = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 为 sign 请求添加 token，但不添加签名（避免循环依赖）
signService.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

signService.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    return Promise.reject(error)
  }
)

export const signApi = {
  // 获取签名（后端代签，secretKey 不返回前端）
  generate: () => {
    return signService.get<Result>('/sign/generate')
  },
}

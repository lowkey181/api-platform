import axios from 'axios'
import type { Result } from '@/utils/request'

const signService = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

signService.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

signService.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
)

export const signApi = {
  generate: (accessKey?: string, timestamp?: string, nonce?: string) => {
    const body: any = {}
    if (accessKey) body.accessKey = accessKey
    if (timestamp) body.timestamp = timestamp
    if (nonce) body.nonce = nonce
    return signService.post<Result>('/sign/generate', body)
  },
}

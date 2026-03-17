import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 定义接口返回数据类型
export interface Result<T = any> {
  code: number
  msg: string
  data: T
}

const service: AxiosInstance = axios.create({
  baseURL: '/api', // 已经在 vite.config.ts 配置了 proxy
  timeout: 10000,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 注意：网关验签头（accessKey, sign, timestamp, nonce）不在这里添加
    // 这些头只在调用网关时动态生成，见 MyInterfaces.vue 的 handleCall 函数
    // 每次调用接口时都需要新的 timestamp 和 nonce，不能存储在 localStorage

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<Result>): any => {
    const res = response.data
    // 如果 code 不是 200，说明报错了
    if (res.code !== 200) {
      ElMessage.error(res.msg || 'Error')
      
      // 401: 未登录或 Token 过期
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.msg || 'Error'))
    } else {
      return res
    }
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service

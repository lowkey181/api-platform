import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { signUtil } from './sign'

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

    // --- 网关验签逻辑 ---
    // 从 localStorage 获取当前用户的应用密钥信息
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const { accessKey, secretKey } = userInfo

    // 只有当 accessKey 和 secretKey 存在时才添加验签头
    if (accessKey && secretKey) {
      const timestamp = Date.now().toString()
      const nonce = signUtil.generateNonce()
      
      const signParams = {
        accessKey,
        timestamp,
        nonce
      }

      // 生成签名
      const sign = signUtil.generateSign(signParams, secretKey)

      // 注入网关要求的 4 个请求头
      config.headers['accessKey'] = accessKey
      config.headers['sign'] = sign
      config.headers['timestamp'] = timestamp
      config.headers['nonce'] = nonce
    }

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

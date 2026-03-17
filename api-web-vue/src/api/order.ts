import axios from 'axios'
import request from '@/utils/request'
import type { Result } from '@/utils/request'

// 创建独立的 axios 实例用于支付宝支付（返回 HTML，不需要 JSON 拦截器）
const alipayService = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 为支付宝请求添加 token
alipayService.interceptors.request.use(
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

export const orderApi = {
  // 创建订单并发起支付（返回 HTML 表单）
  createOrder: (productId: number) => {
    return alipayService.get('/alipay/createOrder', {
      params: {
        productId,
      },
    })
  },
}

import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const orderApi = {
  // 创建订单并发起支付
  createOrder: (productId: number) => {
    const params = new URLSearchParams()
    params.append('id', productId.toString())
    return request.get<Result>(`/alipay/pay?id=${productId}`)
  },
}

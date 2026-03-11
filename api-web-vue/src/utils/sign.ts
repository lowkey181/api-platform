import CryptoJS from 'crypto-js'

/**
 * 签名工具类（对接后端 HmacSHA256 逻辑）
 */
export const signUtil = {
  /**
   * 生成签名
   * @param params 参与签名的参数（包含 accessKey, timestamp, nonce）
   * @param secretKey 密钥
   */
  generateSign: (params: Record<string, string>, secretKey: string): string => {
    // 1. 参数排序（按 key 升序）
    const sortedKeys = Object.keys(params).sort()
    
    // 2. 拼接字符串 k=v&k=v
    const content = sortedKeys
      .map(key => `${key}=${params[key]}`)
      .join('&')
    
    // 3. HmacSHA256 加密
    const hmac = CryptoJS.HmacSHA256(content, secretKey)
    
    // 4. 转为 16 进制小写
    return hmac.toString(CryptoJS.enc.Hex).toLowerCase()
  },

  /**
   * 生成随机 Nonce
   */
  generateNonce: (length = 16): string => {
    return Math.random().toString(36).substring(2, 2 + length)
  }
}

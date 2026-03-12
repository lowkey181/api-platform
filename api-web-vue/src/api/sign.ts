import request from '@/utils/request'
import type { Result } from '@/utils/request'
export const signApi = {
  sign: (data: any) => {
    return request.post<Result>('/api/sign/generate', data)
  },
}
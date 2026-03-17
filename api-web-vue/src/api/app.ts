import request from '@/utils/request'
import type { Result } from '@/utils/request'

export const appApi = {
  createApp: (appName: string) => {
    const params = new URLSearchParams()
    params.append('appName', appName)
    return request.post<Result>('/user/app/key/create', params)
  },
  // Placeholder for listing apps, assuming it will be needed
  listApps: () => {
    return request.get<Result>('/user/app/list')
  },
  allApps: () => {
    return request.get<Result>('/user/app/admin/list')
  },
}

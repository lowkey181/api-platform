import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  
  const getUserInfo = () => {
    const info = localStorage.getItem('userInfo')
    if (info && info !== 'undefined' && info !== 'null') {
      try {
        return JSON.parse(info)
      } catch (e) {
        console.error('Failed to parse userInfo from localStorage', e)
        return {}
      }
    }
    return {}
  }
  
  const userInfo = ref<any>(getUserInfo())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value.role === 'ADMIN')

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(newUserInfo: any) {
    userInfo.value = newUserInfo
    localStorage.setItem('userInfo', JSON.stringify(newUserInfo))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    setToken,
    setUserInfo,
    logout,
  }
})

// ============================================
// 用户状态 - 登录态/权限/路由菜单
// ============================================
import { defineStore } from 'pinia'
import { ref } from 'vue'
import authApi from '@/api/modules/auth'
import type { LoginUserInfo } from '@/types/api'
import { getToken, removeTokens, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() ?? '')
  const userInfo = ref<LoginUserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  // 登录
  async function login(payload: {
    username: string
    password: string
    captcha: string
    captchaKey: string
  }) {
    const res = await authApi.login(payload)
    token.value = res.data.token
    setToken(res.data.token)
    return res
  }

  // 获取用户信息
  async function fetchUserInfo() {
    const res = await authApi.userInfo()
    userInfo.value = res.data
    roles.value = res.data.roles ?? []
    permissions.value = res.data.permissions ?? []
    return res.data
  }

  // 退出
  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // ignore
    } finally {
      reset()
    }
  }

  function reset() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    removeTokens()
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    login,
    fetchUserInfo,
    logout,
    reset,
  }
})

export default useUserStore

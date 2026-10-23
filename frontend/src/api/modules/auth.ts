// ============================================
// 认证 API - 登录/登出/验证码/用户信息
// ============================================
import request from '@/utils/request'
import type { LoginUserInfo } from '@/types/api'

export interface LoginPayload {
  username: string
  password: string
  captcha: string
  captchaKey: string
}

export interface LoginResult {
  token: string
  refreshToken?: string
}

export default {
  login: (data: LoginPayload) => request.post<LoginResult>('/auth/login', data as never),

  logout: () => request.post('/auth/logout'),

  userInfo: () => request.get<LoginUserInfo>('/auth/info'),

  // 获取登录验证码图片 base64
  getCaptcha: (silent = false) =>
    request.get<{ key: string; img: string }>('/auth/captcha', undefined, { silent }),

  // 找回密码 - 发送邮箱验证码(后端 POST /auth/forgot-password)
  sendResetCode: (data: { email: string }) => request.post<void>('/auth/forgot-password', data),

  // 找回密码 - 校验验证码并重置(后端 POST /auth/reset-password)
  resetPassword: (data: { email: string; code: string; newPassword: string }) =>
    request.post<void>('/auth/reset-password', data),

  // 个人中心 - 修改当前登录用户密码(校验旧密码)
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.post<void>('/auth/change-password', data),
}

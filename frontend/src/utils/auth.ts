// ============================================
// Token 存取工具 - 封装 localStorage 操作
// ============================================

const ACCESS_TOKEN_KEY = 'lc_access_token'
const REFRESH_TOKEN_KEY = 'lc_refresh_token'

export const getToken = (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY)

export const setToken = (token: string): void => {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export const getRefreshToken = (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY)

export const setRefreshToken = (token: string): void => {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export const removeTokens = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

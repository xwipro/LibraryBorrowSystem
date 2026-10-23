// ============================================
// Axios 请求实例 + 拦截器
// ============================================
import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, removeTokens } from './auth'
import { setupMock } from '@/mock'

NProgress.configure({ showSpinner: false, trickleSpeed: 80 })

// 业务返回结构 - 后端统一约定
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

// 扩展 AxiosRequestConfig: silent=true 时拦截器不弹 ElMessage
export interface CustomConfig extends AxiosRequestConfig {
  silent?: boolean
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' },
})

// Mock 开关: VITE_USE_MOCK=true (开发环境) 时接管所有请求
setupMock(service)

// 请求拦截 - 注入 token + 进度条
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    NProgress.start()
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  },
)

// 响应拦截 - 解构业务数据 + 统一错误处理
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    NProgress.done()
    const res = response.data
    // 文件流直接返回
    if (response.config.responseType === 'blob') {
      return response as unknown as AxiosResponse
    }
    if (res.code === 200 || res.code === 0) {
      return res as unknown as AxiosResponse
    }
    // 401: token 过期
    if (res.code === 401) {
      handleLogout()
      return Promise.reject(new Error(res.message || '登录已过期'))
    }
    const cfg = response.config as CustomConfig
    if (!cfg.silent) {
      ElMessage.error(res.message || '请求失败')
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    NProgress.done()
    const cfg = (error?.config ?? {}) as CustomConfig
    if (cfg.silent) {
      return Promise.reject(error)
    }
    const status = error?.response?.status
    if (status === 401) {
      handleLogout()
    } else if (status === 403) {
      ElMessage.error('没有权限访问该资源')
    } else if (status === 500) {
      ElMessage.error('服务器开小差了，请稍后重试')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

function handleLogout() {
  ElMessageBox.confirm('登录状态已失效，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      removeTokens()
      window.location.href = '/login'
    })
    .catch(() => {})
}

// 业务封装方法
const request = {
  get<T = unknown>(url: string, params?: object, config?: CustomConfig) {
    return service.get<unknown, ApiResult<T>>(url, { params, ...config })
  },
  post<T = unknown>(url: string, data?: object, config?: CustomConfig) {
    return service.post<unknown, ApiResult<T>>(url, data, config)
  },
  put<T = unknown>(url: string, data?: object, config?: CustomConfig) {
    return service.put<unknown, ApiResult<T>>(url, data, config)
  },
  delete<T = unknown>(url: string, params?: object, config?: CustomConfig) {
    return service.delete<unknown, ApiResult<T>>(url, { params, ...config })
  },
  upload<T = unknown>(url: string, formData: FormData, config?: CustomConfig) {
    return service.post<unknown, ApiResult<T>>(url, formData, {
      ...config,
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export default request

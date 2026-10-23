// ============================================
// 系统 API - 用户/角色/权限/日志/统计看板
// ============================================
import request from '@/utils/request'
import type { CustomConfig } from '@/utils/request'
import type { PageQuery, PageResult, SysUser } from '@/types/api'

export interface SysUserQuery extends PageQuery {
  roleId?: number
  status?: 0 | 1
}

export interface DashboardStats {
  totalBooks: number
  todayBorrows: number
  totalReaders: number
  overdueBooks: number
}

export interface BorrowTrendItem {
  date: string
  count: number
}

export interface CategoryStatItem {
  name: string
  value: number
}

export interface SysLogItem {
  id: number
  username: string
  module: string
  action: string
  ip: string
  time: string
  status: 0 | 1
}

export default {
  // 用户管理
  userPage: (params: SysUserQuery) => request.get<PageResult<SysUser>>('/system/user/page', params),

  userCreate: (data: Partial<SysUser>) => request.post<SysUser>('/system/user', data),

  userUpdate: (id: number, data: Partial<SysUser>) =>
    request.put<SysUser>(`/system/user/${id}`, data),

  userRemove: (id: number) => request.delete(`/system/user/${id}`),

  // 重置密码: 超级管理员需传 newPassword(本人输入); 其他用户不传, 后端随机生成并邮件发送
  resetPassword: (id: number, data?: { newPassword?: string }) =>
    request.put(`/system/user/${id}/reset-password`, data),

  logPage: (params: PageQuery & { module?: string }) =>
    request.get<PageResult<SysLogItem>>('/system/log/page', params),

  // 角色管理
  roleList: () => request.get<{ id: number; name: string; code: string }[]>('/system/role/list'),

  // 统计看板 (config 可传 { silent: true } 由页面自行展示错误态)
  stats: (config?: CustomConfig) =>
    request.get<DashboardStats>('/dashboard/stats', undefined, config),

  borrowTrend: (days = 30, config?: CustomConfig) =>
    request.get<BorrowTrendItem[]>('/dashboard/borrow-trend', { days }, config),

  categoryStats: (config?: CustomConfig) =>
    request.get<CategoryStatItem[]>('/dashboard/category-stats', undefined, config),

  hotBooks: (limit = 10, config?: CustomConfig) =>
    request.get<{ title: string; count: number }[]>('/dashboard/hot-books', { limit }, config),
}

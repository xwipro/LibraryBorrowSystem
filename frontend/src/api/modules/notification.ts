// ============================================
// 通知 API - 管理端通知中心(按登录用户隔离)
// ============================================
import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export type NotifyType = 'borrow' | 'system' | 'overdue'

export interface NoticeItem {
  id: number
  username: string
  type: NotifyType
  title: string
  content: string
  isRead: boolean
  createTime: string
}

export default {
  // 分页列表(本人)
  page: (params: PageQuery & { type?: NotifyType }) =>
    request.get<PageResult<NoticeItem>>('/notification/page', params),

  // 未读数
  unreadCount: () => request.get<number>('/notification/unread-count'),

  // 标记单条已读(不可逆)
  markRead: (id: number) => request.put<boolean>(`/notification/read/${id}`),

  // 全部已读(不可逆)
  markAllRead: () => request.put<boolean>('/notification/read-all'),

  // 删除单条
  remove: (id: number) => request.delete<boolean>(`/notification/${id}`),

  // 清空全部
  clear: () => request.delete<boolean>('/notification/clear'),
}

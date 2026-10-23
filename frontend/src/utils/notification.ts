// ============================================
// 通知中心共享数据源 - 角标(AdminLayout) 与 通知中心页面共用
// 数据来自后端 /notification/* (按登录用户隔离, 已读不可逆)
// 任何一方变更后派发事件通知另一方刷新角标
// ============================================
import notificationApi from '@/api/modules/notification'

export type NotifyType = 'borrow' | 'system' | 'overdue'

export interface Notice {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  type: NotifyType
}

/** 已读状态变更事件名: 通知中心操作后派发, AdminLayout 监听刷新角标 */
export const NOTIFY_READ_EVENT = 'notify:read-changed'

/** 通知中心内已读状态变化后调用, 通知顶部角标同步刷新 */
export function emitNotifyReadChanged() {
  window.dispatchEvent(new CustomEvent(NOTIFY_READ_EVENT))
}

/** 当前未读数(后端统计, 已读/删除即时生效) */
export async function getUnreadCount(): Promise<number> {
  try {
    const res = await notificationApi.unreadCount()
    return res.data ?? 0
  } catch {
    return 0
  }
}

/** 当前用户通知列表(后端分页, 按时间倒序) */
export async function loadNotices(): Promise<Notice[]> {
  try {
    const res = await notificationApi.page({ pageNum: 1, pageSize: 100 })
    const list = res.data?.list ?? []
    return list.map((n) => ({
      id: n.id,
      title: n.title,
      content: n.content,
      time: formatTime(n.createTime),
      read: !!n.isRead,
      type: n.type,
    }))
  } catch {
    return []
  }
}

/** 2026-09-15T09:20:00 -> 2026-09-15 09:20 */
export function formatTime(t?: string): string {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 16)
}

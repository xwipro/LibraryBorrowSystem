// ============================================
// 公告 API - 管理端发布/管理 + 读者端拉取
// ============================================
import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface Announcement {
  id: number
  title: string
  content: string
  publisherName: string
  status: 0 | 1
  isPinned: boolean
  createTime: string
  updateTime: string
}

export default {
  // 管理端: 发布公告
  create: (data: { title: string; content: string }) => request.post<Announcement>('/announcement', data),

  // 管理端: 分页(置顶优先 + 时间倒序)
  page: (params: PageQuery) => request.get<PageResult<Announcement>>('/announcement/page', params),

  // 管理端: 删除
  remove: (id: number) => request.delete(`/announcement/${id}`),

  // 管理端: 置顶 / 取消置顶(可多条置顶)
  pin: (id: number) => request.put<Announcement>(`/announcement/${id}/pin`),
  unpin: (id: number) => request.put<Announcement>(`/announcement/${id}/unpin`),

  // 读者端: 已发布公告列表(置顶优先 + 时间倒序)
  clientList: (limit = 5) => request.get<Announcement[]>('/announcement/client/list', { limit }),
}

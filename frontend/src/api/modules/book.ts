// ============================================
// 图书 API
// ============================================
import request from '@/utils/request'
import type { CustomConfig } from '@/utils/request'
import type { Book, PageQuery, PageResult } from '@/types/api'

export interface BookQuery extends PageQuery {
  categoryId?: number
  status?: 0 | 1
}

export default {
  // config 可传 { silent: true }, 由页面自行展示错误态
  page: (params: BookQuery, config?: CustomConfig) =>
    request.get<PageResult<Book>>('/book/page', params, config),

  detail: (id: number, config?: CustomConfig) =>
    request.get<Book>(`/book/${id}`, undefined, config),

  create: (data: Partial<Book>) => request.post<Book>('/book', data),

  update: (id: number, data: Partial<Book>) => request.put<Book>(`/book/${id}`, data),

  remove: (id: number) => request.delete(`/book/${id}`),

  // 分类列表
  categories: (config?: CustomConfig) =>
    request.get<{ id: number; name: string }[]>('/book/categories', undefined, config),

  // 推荐图书(协同过滤)
  recommend: (readerId: number) => request.get<Book[]>('/book/recommend', { readerId }),

  // 热门借阅 TOP
  hotTop: (limit = 10) => request.get<Book[]>('/book/hot', { limit }),
}

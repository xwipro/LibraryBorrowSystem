// ============================================
// 读者 API
// ============================================
import request from '@/utils/request'
import type { PageQuery, PageResult, Reader } from '@/types/api'

export interface ReaderQuery extends PageQuery {
  readerType?: string
  status?: 0 | 1
}

export default {
  page: (params: ReaderQuery) => request.get<PageResult<Reader>>('/reader/page', params),

  detail: (id: number) => request.get<Reader>(`/reader/${id}`),

  create: (data: Partial<Reader>) => request.post<Reader>('/reader', data),

  update: (id: number, data: Partial<Reader>) => request.put<Reader>(`/reader/${id}`, data),

  remove: (id: number) => request.delete(`/reader/${id}`),
}

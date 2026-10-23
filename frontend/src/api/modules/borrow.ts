// ============================================
// 借阅 API - 包含续借/归还/预约
// ============================================
import request from '@/utils/request'
import type { BorrowRecord, PageQuery, PageResult, Reservation } from '@/types/api'

export interface BorrowQuery extends PageQuery {
  status?: 0 | 1 | 2 | 3
  readerId?: number
}

export default {
  page: (params: BorrowQuery) => request.get<PageResult<BorrowRecord>>('/borrow/page', params),

  // 办理借阅
  create: (data: { readerId: number; bookId: number }) =>
    request.post<BorrowRecord>('/borrow', data as never),

  // 归还
  return: (id: number) => request.put<BorrowRecord>(`/borrow/${id}/return`),

  // 续借
  renew: (id: number) => request.put<BorrowRecord>(`/borrow/${id}/renew`),

  // 逾期处理
  handleOverdue: (id: number, data: { penalty: number }) =>
    request.put<BorrowRecord>(`/borrow/${id}/overdue`, data as never),

  // 删除历史借阅记录(仅已归还可删)
  remove: (id: number) => request.delete<boolean>(`/borrow/${id}`),

  // 预约列表
  reservationPage: (params: PageQuery & { status?: 0 | 1 | 2 }) =>
    request.get<PageResult<Reservation>>('/reservation/page', params),

  // 审批预约
  approveReservation: (id: number) => request.put<Reservation>(`/reservation/${id}/approve`),

  // 删除历史预约记录(仅已通知/已取消可删)
  removeReservation: (id: number) => request.delete<boolean>(`/reservation/${id}`),
}

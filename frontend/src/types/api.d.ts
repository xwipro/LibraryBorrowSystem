// ============================================
// 全局类型 - 业务实体 & API 契约
// ============================================

// 分页请求
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
}

// 分页响应
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// 通用选项
export interface SelectOption {
  label: string
  value: string | number
}

// ===== 图书 =====
export interface Book {
  id: number
  isbn: string
  title: string
  author: string
  publisher: string
  publishDate: string
  categoryId: number
  categoryName?: string
  cover?: string
  summary?: string
  stockCount: number
  totalCount: number
  location?: string
  status: 0 | 1 // 0: 下架 1: 在架
  rating?: number
  borrowCount?: number
  createTime?: string
  updateTime?: string
}

// ===== 读者 =====
export interface Reader {
  id: number
  readerNo: string
  name: string
  gender: 0 | 1
  phone?: string
  email?: string
  avatar?: string
  readerType: string
  borrowCount: number
  maxBorrow: number
  status: 0 | 1
  createTime?: string
}

// ===== 借阅记录 =====
export interface BorrowRecord {
  id: number
  readerId: number
  readerName?: string
  bookId: number
  bookTitle?: string
  isbn?: string
  borrowTime: string
  dueTime: string
  returnTime?: string
  status: 0 | 1 | 2 | 3 // 0:借阅中 1:已归还 2:已逾期 3:已预约
  renewCount: number
}

// ===== 预约记录 =====
export interface Reservation {
  id: number
  readerId: number
  readerName?: string
  bookId: number
  bookTitle?: string
  reserveTime: string
  expireTime: string
  status: 0 | 1 | 2 // 0:待处理 1:已通知 2:已取消
}

// ===== 系统用户 =====
export interface SysUser {
  id: number
  username: string
  realName: string
  avatar?: string
  phone?: string
  email?: string
  roleId: number
  roleName?: string
  status: 0 | 1
  createTime?: string
  lastLoginTime?: string
}

// ===== 登录用户信息 =====
export interface LoginUserInfo {
  id: number
  username: string
  realName: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

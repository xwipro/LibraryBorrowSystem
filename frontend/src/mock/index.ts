// ============================================
// 本地 Mock 适配器
//
// 用途: 后端未启动时, 让页面仍能拿到结构正确的数据预览
// 开关: localStorage('app.mock.enabled') > 环境变量 VITE_USE_MOCK
//   - .env.development: VITE_USE_MOCK=true  -> 默认走 mock
//   - .env.production : VITE_USE_MOCK=false -> 走真实接口
// 运行时切换: setMockEnabled(true / false), 无需重启 dev server
// ============================================
import type { AxiosAdapter, AxiosInstance, AxiosResponse } from 'axios'
import {
  approveReservation,
  borrowBook,
  buildBorrowTrend,
  buildCategoryStats,
  buildHotBooks,
  buildStats,
  createBook,
  createReader,
  createSysUser,
  findBook,
  findReader,
  mockBooks,
  mockCategories,
  mockLoginUser,
  mockRoles,
  overdueBorrow,
  queryBookPage,
  queryBorrowPage,
  queryLogPage,
  queryReaderPage,
  queryReservationPage,
  querySysUserPage,
  removeBook,
  removeReader,
  removeSysUser,
  renewBorrow,
  returnBorrow,
  updateBook,
  updateReader,
  updateSysUser,
  type MockBookQuery,
} from './data'

const STORAGE_KEY = 'app.mock.enabled'

function readEnvEnabled(): boolean {
  return import.meta.env.VITE_USE_MOCK === 'true'
}

let enabled = readEnvEnabled()
let boundService: AxiosInstance | null = null

if (typeof window !== 'undefined') {
  const stored = window.localStorage.getItem(STORAGE_KEY)
  if (stored !== null) {
    enabled = stored === 'true'
  }
  if (enabled) {
    console.warn(
      '[mock] 本地 Mock 数据已启用(环境变量 VITE_USE_MOCK 或 localStorage.app.mock.enabled)',
    )
  }
}

export function isMockEnabled(): boolean {
  return enabled
}

/** 运行时开关 Mock, 便于后端就绪后立即切回真实接口 */
export function setMockEnabled(value: boolean): void {
  enabled = value
  try {
    window.localStorage.setItem(STORAGE_KEY, String(value))
  } catch {
    // 隐私模式下 localStorage 可能不可写, 忽略
  }
  syncAdapter()
  console.warn(`[mock] 本地 Mock 数据已${value ? '开启' : '关闭'}`)
}

export function setupMock(service: AxiosInstance): void {
  boundService = service
  syncAdapter()
}

function syncAdapter(): void {
  if (!boundService) return
  if (enabled) {
    boundService.defaults.adapter = mockAdapter
  } else {
    delete boundService.defaults.adapter
  }
}

// ===== 适配器实现 =====
interface MockResult {
  code: number
  message: string
  data: unknown
}

function ok(data: unknown, message = 'success'): MockResult {
  return { code: 200, message, data }
}

function fail(code: number, message: string): MockResult {
  return { code, message, data: null }
}

function normalizePath(url: string): string {
  let path = url.split('?')[0]
  path = path.replace(/^https?:\/\/[^/]+/, '')
  path = path.replace(/^\/api/, '')
  if (!path.startsWith('/')) path = `/${path}`
  return path.replace(/\/+$/, '') || '/'
}

function parseBody(data: unknown): Record<string, unknown> {
  if (!data) return {}
  if (typeof data === 'string') {
    try {
      return JSON.parse(data) as Record<string, unknown>
    } catch {
      return {}
    }
  }
  if (typeof data === 'object') return data as Record<string, unknown>
  return {}
}

function toNumber(value: unknown, fallback: number): number {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

/** 把 axios query 上的 unknown 收敛为筛选字段可接受的 number | string | undefined */
function rawFilter(value: unknown): number | string | undefined {
  if (value === undefined || value === '') return undefined
  if (typeof value === 'number') return value
  if (typeof value === 'string') return value
  return String(value)
}

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

function resolveRequest(
  method: string,
  path: string,
  params: Record<string, unknown>,
  body: Record<string, unknown>,
): MockResult {
  // ===== 数据看板 =====
  if (method === 'get' && path === '/dashboard/stats') {
    return ok(buildStats())
  }
  if (method === 'get' && path === '/dashboard/borrow-trend') {
    return ok(buildBorrowTrend(toNumber(params.days, 30)))
  }
  if (method === 'get' && path === '/dashboard/category-stats') {
    return ok(buildCategoryStats())
  }
  if (method === 'get' && path === '/dashboard/hot-books') {
    return ok(buildHotBooks(toNumber(params.limit, 10)))
  }

  // ===== 图书 =====
  if (method === 'get' && path === '/book/categories') {
    return ok(mockCategories)
  }
  if (method === 'get' && path === '/book/hot') {
    const limit = toNumber(params.limit, 10)
    return ok(
      [...mockBooks].sort((a, b) => (b.borrowCount ?? 0) - (a.borrowCount ?? 0)).slice(0, limit),
    )
  }
  if (method === 'get' && path === '/book/recommend') {
    return ok(mockBooks.slice(0, 6))
  }
  if (method === 'get' && path === '/book/page') {
    const query: MockBookQuery = {
      pageNum: toNumber(params.pageNum, 1),
      pageSize: toNumber(params.pageSize, 10),
      keyword: typeof params.keyword === 'string' ? params.keyword : '',
    }
    if (params.categoryId !== undefined && params.categoryId !== '') {
      query.categoryId = toNumber(params.categoryId, 0)
    }
    if (params.status !== undefined && params.status !== '') {
      query.status = toNumber(params.status, 1)
    }
    return ok(queryBookPage(query))
  }
  if (method === 'post' && path === '/book') {
    return ok(createBook(body))
  }

  const bookIdMatch = /^\/book\/(\d+)$/.exec(path)
  if (bookIdMatch) {
    const id = Number(bookIdMatch[1])
    if (method === 'get') {
      const book = findBook(id)
      return book ? ok(book) : fail(404, '图书不存在')
    }
    if (method === 'put') {
      const book = updateBook(id, body)
      return book ? ok(book) : fail(404, '图书不存在')
    }
    if (method === 'delete') {
      return removeBook(id) ? ok(true) : fail(404, '图书不存在')
    }
  }

  // ===== 读者 =====
  if (method === 'get' && path === '/reader/page') {
    return ok(
      queryReaderPage({
        pageNum: toNumber(params.pageNum, 1),
        pageSize: toNumber(params.pageSize, 10),
        keyword: typeof params.keyword === 'string' ? params.keyword : '',
        readerType: typeof params.readerType === 'string' ? params.readerType : '',
        status: rawFilter(params.status),
      }),
    )
  }
  if (method === 'post' && path === '/reader') {
    return ok(createReader(body))
  }
  const readerIdMatch = /^\/reader\/(\d+)$/.exec(path)
  if (readerIdMatch) {
    const id = Number(readerIdMatch[1])
    if (method === 'get') {
      const reader = findReader(id)
      return reader ? ok(reader) : fail(404, '读者不存在')
    }
    if (method === 'put') {
      const reader = updateReader(id, body)
      return reader ? ok(reader) : fail(404, '读者不存在')
    }
    if (method === 'delete') {
      return removeReader(id) ? ok(true) : fail(404, '读者不存在')
    }
  }

  // ===== 借阅 =====
  if (method === 'get' && path === '/borrow/page') {
    return ok(
      queryBorrowPage({
        pageNum: toNumber(params.pageNum, 1),
        pageSize: toNumber(params.pageSize, 10),
        keyword: typeof params.keyword === 'string' ? params.keyword : '',
        status: rawFilter(params.status),
        readerId: rawFilter(params.readerId),
      }),
    )
  }
  if (method === 'post' && path === '/borrow') {
    const result = borrowBook({
      readerId: toNumber(body.readerId, 0),
      bookId: toNumber(body.bookId, 0),
    })
    return result.ok ? ok(result.data) : fail(400, result.message)
  }
  const borrowActionMatch = /^\/borrow\/(\d+)\/(return|renew|overdue)$/.exec(path)
  if (borrowActionMatch) {
    const id = Number(borrowActionMatch[1])
    const action = borrowActionMatch[2]
    if (method === 'put' && action === 'return') {
      const record = returnBorrow(id)
      return record ? ok(record) : fail(404, '借阅记录不存在')
    }
    if (method === 'put' && action === 'renew') {
      const result = renewBorrow(id)
      return result.ok ? ok(result.data) : fail(400, result.message)
    }
    if (method === 'put' && action === 'overdue') {
      const record = overdueBorrow(id)
      return record ? ok(record) : fail(404, '借阅记录不存在')
    }
  }

  // ===== 预约 =====
  if (method === 'get' && path === '/reservation/page') {
    return ok(
      queryReservationPage({
        pageNum: toNumber(params.pageNum, 1),
        pageSize: toNumber(params.pageSize, 10),
        keyword: typeof params.keyword === 'string' ? params.keyword : '',
        status: rawFilter(params.status),
      }),
    )
  }
  const reserveApproveMatch = /^\/reservation\/(\d+)\/approve$/.exec(path)
  if (reserveApproveMatch && method === 'put') {
    const id = Number(reserveApproveMatch[1])
    const result = approveReservation(id)
    return result.ok ? ok(result.data) : fail(400, result.message)
  }

  // ===== 系统 =====
  if (method === 'get' && path === '/system/user/page') {
    return ok(
      querySysUserPage({
        pageNum: toNumber(params.pageNum, 1),
        pageSize: toNumber(params.pageSize, 10),
        keyword: typeof params.keyword === 'string' ? params.keyword : '',
        roleId: rawFilter(params.roleId),
        status: rawFilter(params.status),
      }),
    )
  }
  if (method === 'post' && path === '/system/user') {
    return ok(createSysUser(body))
  }
  if (method === 'put' && /^\/system\/user\/(\d+)\/reset-password$/.test(path)) {
    return ok(true)
  }
  const sysUserMatch = /^\/system\/user\/(\d+)$/.exec(path)
  if (sysUserMatch) {
    const id = Number(sysUserMatch[1])
    if (method === 'put') {
      const user = updateSysUser(id, body)
      return user ? ok(user) : fail(404, '用户不存在')
    }
    if (method === 'delete') {
      return removeSysUser(id) ? ok(true) : fail(404, '用户不存在')
    }
  }
  if (method === 'get' && path === '/system/role/list') {
    return ok(mockRoles)
  }
  if (method === 'get' && path === '/system/log/page') {
    return ok(
      queryLogPage({
        pageNum: toNumber(params.pageNum, 1),
        pageSize: toNumber(params.pageSize, 10),
        keyword: typeof params.keyword === 'string' ? params.keyword : '',
        module: typeof params.module === 'string' ? params.module : '',
      }),
    )
  }

  // ===== 认证 =====
  if (method === 'get' && path === '/auth/info') {
    return ok(mockLoginUser)
  }

  return fail(404, `Mock 未覆盖该接口: ${method.toUpperCase()} ${path}`)
}

export const mockAdapter: AxiosAdapter = async (config) => {
  const method = (config.method ?? 'get').toLowerCase()
  const path = normalizePath(config.url ?? '')
  const params = (config.params ?? {}) as Record<string, unknown>
  const body = parseBody(config.data)

  // 模拟网络延迟, 便于观察 loading 态
  await delay(120 + Math.round(Math.random() * 180))

  const result = resolveRequest(method, path, params, body)

  const response: AxiosResponse = {
    data: { code: result.code, message: result.message, data: result.data },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  }
  return response
}

// ============================================
// Mock 数据集 - 仅开发环境(VITE_USE_MOCK=true)使用
// 全部数据由固定种子推导, 每次刷新结果一致, 便于对照调试
// ============================================
import type {
  BorrowTrendItem,
  CategoryStatItem,
  DashboardStats,
  SysLogItem,
} from '@/api/modules/system'
import type {
  Book,
  BorrowRecord,
  LoginUserInfo,
  PageResult,
  Reader,
  Reservation,
  SysUser,
} from '@/types/api'

// ===== 图书分类 =====
export const mockCategories: { id: number; name: string }[] = [
  { id: 1, name: '文学小说' },
  { id: 2, name: '计算机技术' },
  { id: 3, name: '历史人文' },
  { id: 4, name: '经济管理' },
  { id: 5, name: '科学科普' },
  { id: 6, name: '艺术设计' },
  { id: 7, name: '教育考试' },
  { id: 8, name: '生活健康' },
]

function categoryName(id: number): string {
  return mockCategories.find((c) => c.id === id)?.name ?? '未分类'
}

// [书名, 作者, 出版社, 出版日期, 分类 id]
type BookSeed = [string, string, string, string, number]

const bookSeeds: BookSeed[] = [
  ['活着', '余华', '作家出版社', '2012-08-01', 1],
  ['百年孤独', '加西亚·马尔克斯', '南海出版公司', '2011-06-01', 1],
  ['平凡的世界', '路遥', '北京十月文艺出版社', '2017-06-01', 1],
  ['围城', '钱钟书', '人民文学出版社', '1991-02-01', 1],
  ['三体（全集）', '刘慈欣', '重庆出版社', '2008-01-01', 1],
  ['白夜行', '东野圭吾', '南海出版公司', '2013-01-01', 1],
  ['追风筝的人', '卡勒德·胡赛尼', '上海人民出版社', '2006-05-01', 1],
  ['人类简史', '尤瓦尔·赫拉利', '中信出版社', '2014-11-01', 3],
  ['万历十五年', '黄仁宇', '中华书局', '2007-01-01', 3],
  ['明朝那些事儿（全九册）', '当年明月', '浙江人民出版社', '2011-08-01', 3],
  ['全球通史', '斯塔夫里阿诺斯', '北京大学出版社', '2006-01-01', 3],
  ['中国历代政治得失', '钱穆', '生活·读书·新知三联书店', '2001-01-01', 3],
  ['深入理解计算机系统', 'Randal E. Bryant', '机械工业出版社', '2016-11-01', 2],
  ['算法导论', 'Thomas H. Cormen', '机械工业出版社', '2013-01-01', 2],
  ['JavaScript 高级程序设计', '马特·弗里斯比', '人民邮电出版社', '2020-11-01', 2],
  ['Vue.js 设计与实现', '霍春阳', '人民邮电出版社', '2022-01-01', 2],
  ['代码整洁之道', '罗伯特·C. 马丁', '人民邮电出版社', '2010-01-01', 2],
  ['重构：改善既有代码的设计', '马丁·福勒', '人民邮电出版社', '2019-04-01', 2],
  ['计算机网络：自顶向下方法', 'James F. Kurose', '机械工业出版社', '2018-06-01', 2],
  ['数据库系统概念', '亚伯拉罕·西尔伯沙茨', '机械工业出版社', '2012-03-01', 2],
  ['经济学原理', '曼昆', '北京大学出版社', '2015-09-01', 4],
  ['穷查理宝典', '查理·芒格', '中信出版社', '2016-05-01', 4],
  ['原则', '瑞·达利欧', '中信出版社', '2018-01-01', 4],
  ['影响力', '罗伯特·西奥迪尼', '北京联合出版公司', '2016-09-01', 4],
  ['思考，快与慢', '丹尼尔·卡尼曼', '中信出版社', '2012-07-01', 4],
  ['时间简史', '史蒂芬·霍金', '湖南科学技术出版社', '2010-01-01', 5],
  ['自私的基因', '理查德·道金斯', '中信出版社', '2012-09-01', 5],
  ['万物简史', '比尔·布莱森', '接力出版社', '2005-01-01', 5],
  ['上帝掷骰子吗', '曹天元', '北京联合出版公司', '2013-01-01', 5],
  ['费曼物理学讲义', '理查德·费曼', '上海科学技术出版社', '2013-04-01', 5],
  ['艺术的故事', '贡布里希', '广西美术出版社', '2008-04-01', 6],
  ['设计中的设计', '原研哉', '山东人民出版社', '2006-11-01', 6],
  ['配色设计原理', '奥博斯科编辑部', '中国青年出版社', '2011-01-01', 6],
  ['写给大家看的设计书', 'Robin Williams', '人民邮电出版社', '2016-01-01', 6],
  ['刻意练习', '安德斯·艾利克森', '机械工业出版社', '2016-11-01', 7],
  ['学习之道', '芭芭拉·奥克利', '机械工业出版社', '2016-11-01', 7],
  ['如何阅读一本书', '莫提默·J. 艾德勒', '商务印书馆', '2004-01-01', 7],
  ['教育学基础', '全国十二所重点师范大学', '教育科学出版社', '2015-08-01', 7],
  ['中国居民膳食指南', '中国营养学会', '人民卫生出版社', '2022-04-01', 8],
  ['睡眠革命', '尼克·利特尔黑尔斯', '北京联合出版公司', '2017-05-01', 8],
]

const SHELF_ZONES = 'ABCDEFG'

let bookIdSeq = 0

function buildBook(seed: BookSeed, index: number): Book {
  const [title, author, publisher, publishDate, categoryId] = seed
  const totalCount = 2 + ((index * 3) % 6)
  const borrowed = index % 5
  const stockCount = Math.max(0, totalCount - borrowed)
  bookIdSeq += 1
  return {
    id: bookIdSeq,
    isbn: `978-7-${String(100 + (index % 800)).padStart(3, '0')}-${String(
      10000 + index * 137,
    ).padStart(5, '0')}-${index % 10}`,
    title,
    author,
    publisher,
    publishDate,
    categoryId,
    categoryName: categoryName(categoryId),
    cover: '',
    summary: `${title}，${categoryName(categoryId)}类馆藏图书，作者 ${author}。`,
    stockCount,
    totalCount,
    location: `${SHELF_ZONES[index % SHELF_ZONES.length]}区-${String(1 + (index % 6)).padStart(
      2,
      '0',
    )}架-${String(1 + (index % 8)).padStart(2, '0')}层`,
    status: index % 13 === 12 ? 0 : 1,
    rating: Number((3.8 + ((index * 7) % 12) / 10).toFixed(1)),
    borrowCount: 3 + ((index * 37) % 88),
    createTime: `2025-${String(1 + (index % 12)).padStart(2, '0')}-${String(
      1 + (index % 27),
    ).padStart(2, '0')} 09:30:00`,
    updateTime: `2026-${String(1 + (index % 8)).padStart(2, '0')}-${String(
      1 + (index % 27),
    ).padStart(2, '0')} 14:20:00`,
  }
}

// 可变副本 - mock 环境下的增删改会作用于此
export const mockBooks: Book[] = bookSeeds.map(buildBook)

// ===== 看板统计 =====
export function buildStats(): DashboardStats {
  const totalBooks = mockBooks.reduce((sum, b) => sum + b.totalCount, 0)
  const trend = buildBorrowTrend(1)
  return {
    totalBooks,
    todayBorrows: trend.length ? trend[trend.length - 1].count : 0,
    totalReaders: 326,
    overdueBooks: 12,
  }
}

// ===== 借阅趋势 =====
export function buildBorrowTrend(days: number): BorrowTrendItem[] {
  const safeDays = Math.min(Math.max(Number(days) || 30, 1), 90)
  const today = new Date()
  const list: BorrowTrendItem[] = []
  for (let i = safeDays - 1; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() - i)
    const seed = d.getFullYear() * 372 + (d.getMonth() + 1) * 31 + d.getDate()
    const wave = Math.sin(seed / 5) * 8 + Math.cos(seed / 13) * 5
    const weekendBonus = d.getDay() === 0 || d.getDay() === 6 ? 9 : 0
    const count = Math.max(8, Math.round(42 + weekendBonus + wave + ((seed * 7) % 11) - 5))
    list.push({
      date: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
        d.getDate(),
      ).padStart(2, '0')}`,
      count,
    })
  }
  return list
}

// ===== 分类占比 =====
export function buildCategoryStats(): CategoryStatItem[] {
  const counter = new Map<number, number>()
  for (const b of mockBooks) {
    counter.set(b.categoryId, (counter.get(b.categoryId) ?? 0) + b.totalCount)
  }
  return mockCategories
    .map((c) => ({ name: c.name, value: counter.get(c.id) ?? 0 }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value)
}

// ===== 热门借阅 TOP =====
export function buildHotBooks(limit: number): { title: string; count: number }[] {
  const safeLimit = Math.min(Math.max(Number(limit) || 10, 1), 50)
  return [...mockBooks]
    .sort((a, b) => (b.borrowCount ?? 0) - (a.borrowCount ?? 0))
    .slice(0, safeLimit)
    .map((b) => ({ title: b.title, count: b.borrowCount ?? 0 }))
}

// ===== 图书分页查询 =====
export interface MockBookQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  // 与前端筛选表单保持一致: 清空筛选时会传空字符串
  categoryId?: number | string
  status?: number | string
}

export function queryBookPage(params: MockBookQuery): PageResult<Book> {
  const pageNum = Math.max(Number(params.pageNum) || 1, 1)
  const pageSize = Math.min(Math.max(Number(params.pageSize) || 10, 1), 100)
  const keyword = (params.keyword ?? '').trim().toLowerCase()

  let rows = [...mockBooks]
  if (keyword) {
    rows = rows.filter(
      (b) =>
        b.title.toLowerCase().includes(keyword) ||
        b.author.toLowerCase().includes(keyword) ||
        b.isbn.toLowerCase().includes(keyword) ||
        b.publisher.toLowerCase().includes(keyword),
    )
  }
  if (params.categoryId !== undefined && params.categoryId !== '') {
    const categoryId = Number(params.categoryId)
    rows = rows.filter((b) => b.categoryId === categoryId)
  }
  if (params.status !== undefined && params.status !== '') {
    const status = Number(params.status)
    rows = rows.filter((b) => b.status === status)
  }

  const total = rows.length
  const start = (pageNum - 1) * pageSize
  return {
    list: rows.slice(start, start + pageSize),
    total,
    pageNum,
    pageSize,
  }
}

export function createBook(payload: Partial<Book>): Book {
  bookIdSeq += 1
  const categoryId = Number(payload.categoryId ?? mockCategories[0].id)
  const totalCount = Number(payload.totalCount ?? 0)
  const book: Book = {
    id: bookIdSeq,
    isbn: payload.isbn ?? `978-7-${String(100 + (bookIdSeq % 800)).padStart(3, '0')}-00000-0`,
    title: payload.title ?? '未命名图书',
    author: payload.author ?? '佚名',
    publisher: payload.publisher ?? '未知出版社',
    publishDate: payload.publishDate ?? '',
    categoryId,
    categoryName: categoryName(categoryId),
    cover: payload.cover ?? '',
    summary: payload.summary ?? '',
    stockCount: Number(payload.stockCount ?? totalCount),
    totalCount,
    location: payload.location ?? '',
    status: (payload.status ?? 1) as 0 | 1,
    rating: 0,
    borrowCount: 0,
    createTime: now(),
    updateTime: now(),
  }
  mockBooks.unshift(book)
  return book
}

export function updateBook(id: number, payload: Partial<Book>): Book | null {
  const index = mockBooks.findIndex((b) => b.id === id)
  if (index < 0) return null
  const current = mockBooks[index]
  const categoryId = Number(payload.categoryId ?? current.categoryId)
  const merged: Book = {
    ...current,
    ...payload,
    id,
    categoryId,
    categoryName: categoryName(categoryId),
    stockCount: Number(payload.stockCount ?? current.stockCount),
    totalCount: Number(payload.totalCount ?? current.totalCount),
    status: (payload.status ?? current.status) as 0 | 1,
    updateTime: now(),
  }
  mockBooks.splice(index, 1, merged)
  return merged
}

export function removeBook(id: number): boolean {
  const index = mockBooks.findIndex((b) => b.id === id)
  if (index < 0) return false
  mockBooks.splice(index, 1)
  return true
}

export function findBook(id: number): Book | null {
  return mockBooks.find((b) => b.id === id) ?? null
}

function now(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate(),
  ).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(
    2,
    '0',
  )}:${String(d.getSeconds()).padStart(2, '0')}`
}

// ===== 通用工具 =====
function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

function fmtDateTime(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(
    d.getHours(),
  )}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/** 相对今天偏移 days 天的固定时间点, 用于推导种子数据 */
function daysOffset(days: number, h = 10, m = 30): Date {
  const d = new Date()
  d.setDate(d.getDate() - days)
  d.setHours(h, m, 0, 0)
  return d
}

/** 通用分页切片 helper, 各模块分页查询统一走这里 */
export function paginate<T>(rows: T[], pageNum?: number, pageSize?: number): PageResult<T> {
  const pn = Math.max(Number(pageNum) || 1, 1)
  const ps = Math.min(Math.max(Number(pageSize) || 10, 1), 100)
  const total = rows.length
  const start = (pn - 1) * ps
  return { list: rows.slice(start, start + ps), total, pageNum: pn, pageSize: ps }
}

function toNum(v: unknown, fallback = 0): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

// ===== 读者 =====
const readerNames = [
  '张伟',
  '王芳',
  '李娜',
  '刘洋',
  '陈静',
  '杨帆',
  '赵磊',
  '黄敏',
  '周杰',
  '吴霞',
  '徐强',
  '孙丽',
  '马涛',
  '朱婷',
  '胡军',
  '郭静',
  '何伟',
  '高洋',
  '林芳',
  '罗成',
]
const readerTypes = ['学生', '教师', '职工', '校外']

function maxBorrowOf(type: string): number {
  if (type === '教师') return 10
  if (type === '职工') return 8
  if (type === '学生') return 5
  return 3
}

let readerIdSeq = 0
function buildReader(index: number): Reader {
  readerIdSeq += 1
  const name = readerNames[index % readerNames.length]
  const readerType = readerTypes[index % readerTypes.length]
  const maxBorrow = maxBorrowOf(readerType)
  return {
    id: readerIdSeq,
    readerNo: `RD2026${String(readerIdSeq).padStart(3, '0')}`,
    name,
    gender: (index % 2) as 0 | 1,
    phone: `13${String(800000000 + index * 137891).slice(0, 9)}`,
    email: `reader${readerIdSeq}@example.com`,
    readerType,
    borrowCount: index % (maxBorrow + 1),
    maxBorrow,
    status: (index % 9 === 8 ? 0 : 1) as 0 | 1,
    createTime: `2025-${pad2(1 + (index % 12))}-${pad2(1 + (index % 27))} 09:15:00`,
  }
}

export const mockReaders: Reader[] = readerNames.map((_, i) => buildReader(i))

export interface ReaderPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  readerType?: string
  status?: number | string
}

export function queryReaderPage(q: ReaderPageQuery): PageResult<Reader> {
  const keyword = (q.keyword ?? '').trim().toLowerCase()
  let rows = [...mockReaders]
  if (keyword) {
    rows = rows.filter(
      (r) =>
        r.name.toLowerCase().includes(keyword) ||
        r.readerNo.toLowerCase().includes(keyword) ||
        (r.phone ?? '').toLowerCase().includes(keyword),
    )
  }
  if (q.readerType) rows = rows.filter((r) => r.readerType === q.readerType)
  if (q.status !== undefined && q.status !== '') {
    rows = rows.filter((r) => r.status === toNum(q.status))
  }
  return paginate(rows, toNum(q.pageNum, 1), toNum(q.pageSize, 10))
}

export function findReader(id: number): Reader | null {
  return mockReaders.find((r) => r.id === id) ?? null
}

export function createReader(payload: Partial<Reader>): Reader {
  readerIdSeq += 1
  const readerType = payload.readerType ?? readerTypes[readerIdSeq % readerTypes.length]
  const reader: Reader = {
    id: readerIdSeq,
    readerNo: payload.readerNo ?? `RD2026${String(readerIdSeq).padStart(3, '0')}`,
    name: payload.name ?? '新读者',
    gender: (payload.gender ?? 0) as 0 | 1,
    phone: payload.phone ?? '',
    email: payload.email ?? '',
    readerType,
    borrowCount: 0,
    maxBorrow: payload.maxBorrow ?? maxBorrowOf(readerType),
    status: (payload.status ?? 1) as 0 | 1,
    createTime: now(),
  }
  mockReaders.unshift(reader)
  return reader
}

export function updateReader(id: number, payload: Partial<Reader>): Reader | null {
  const index = mockReaders.findIndex((r) => r.id === id)
  if (index < 0) return null
  const current = mockReaders[index]
  const readerType = payload.readerType ?? current.readerType
  const merged: Reader = {
    ...current,
    ...payload,
    id,
    readerType,
    maxBorrow: payload.maxBorrow ?? current.maxBorrow,
    status: (payload.status ?? current.status) as 0 | 1,
  }
  mockReaders.splice(index, 1, merged)
  return merged
}

export function removeReader(id: number): boolean {
  const index = mockReaders.findIndex((r) => r.id === id)
  if (index < 0) return false
  mockReaders.splice(index, 1)
  return true
}

// ===== 借阅记录 =====
let borrowIdSeq = 0
function buildBorrowRecord(index: number): BorrowRecord {
  borrowIdSeq += 1
  const readerId = 1 + (index % mockReaders.length)
  const bookId = 1 + (index % mockBooks.length)
  const pattern = index % 10
  let status: 0 | 1 | 2
  let ago: number
  if (pattern < 3) {
    status = 0
    ago = 1 + ((index * 2) % 20) // 1~20 天前借出, 尚未到期
  } else if (pattern < 8) {
    status = 1
    ago = 20 + ((index * 3) % 30) // 20~49 天前借出, 已归还
  } else {
    status = 2
    ago = 35 + ((index * 5) % 25) // 35~59 天前借出, 已逾期
  }
  const borrow = daysOffset(ago, 9 + (index % 10), (index * 7) % 60)
  const due = new Date(borrow)
  due.setDate(due.getDate() + 30)
  const record: BorrowRecord = {
    id: borrowIdSeq,
    readerId,
    bookId,
    borrowTime: fmtDateTime(borrow),
    dueTime: fmtDateTime(due),
    status,
    renewCount: index % 3,
  }
  if (status === 1) {
    const ret = new Date(borrow)
    ret.setDate(ret.getDate() + 15)
    record.returnTime = fmtDateTime(ret)
  }
  return record
}

export const mockBorrowRecords: BorrowRecord[] = Array.from({ length: 40 }, (_, i) =>
  buildBorrowRecord(i),
)

/** join 读者/图书, 给单条记录补上 readerName/bookTitle/isbn */
export function decorateBorrow(r: BorrowRecord): BorrowRecord {
  const reader = mockReaders.find((x) => x.id === r.readerId)
  const book = mockBooks.find((x) => x.id === r.bookId)
  return {
    ...r,
    readerName: reader?.name ?? '未知读者',
    bookTitle: book?.title ?? '未知图书',
    isbn: book?.isbn ?? '',
  }
}

export interface BorrowPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number | string
  readerId?: number | string
}

export function queryBorrowPage(q: BorrowPageQuery): PageResult<BorrowRecord> {
  const keyword = (q.keyword ?? '').trim().toLowerCase()
  let rows = mockBorrowRecords.map(decorateBorrow)
  if (keyword) {
    rows = rows.filter(
      (r) =>
        (r.readerName ?? '').toLowerCase().includes(keyword) ||
        (r.bookTitle ?? '').toLowerCase().includes(keyword),
    )
  }
  if (q.status !== undefined && q.status !== '') {
    rows = rows.filter((r) => r.status === toNum(q.status))
  }
  if (q.readerId !== undefined && q.readerId !== '') {
    rows = rows.filter((r) => r.readerId === toNum(q.readerId))
  }
  return paginate(rows, toNum(q.pageNum, 1), toNum(q.pageSize, 10))
}

export type BorrowResult<T> = { ok: true; data: T } | { ok: false; message: string }

export function borrowBook(payload: {
  readerId: number
  bookId: number
}): BorrowResult<BorrowRecord> {
  const reader = mockReaders.find((r) => r.id === Number(payload.readerId))
  if (!reader) return { ok: false, message: '读者不存在' }
  const book = mockBooks.find((b) => b.id === Number(payload.bookId))
  if (!book) return { ok: false, message: '图书不存在' }
  if (book.stockCount <= 0) return { ok: false, message: '该图书库存不足，无法借阅' }
  book.stockCount -= 1
  reader.borrowCount += 1
  const borrow = new Date()
  const due = new Date()
  due.setDate(due.getDate() + 30)
  borrowIdSeq += 1
  const record: BorrowRecord = {
    id: borrowIdSeq,
    readerId: reader.id,
    bookId: book.id,
    borrowTime: fmtDateTime(borrow),
    dueTime: fmtDateTime(due),
    status: 0,
    renewCount: 0,
  }
  mockBorrowRecords.unshift(record)
  return { ok: true, data: decorateBorrow(record) }
}

export function returnBorrow(id: number): BorrowRecord | null {
  const r = mockBorrowRecords.find((x) => x.id === id)
  if (!r) return null
  r.status = 1
  r.returnTime = now()
  const book = mockBooks.find((b) => b.id === r.bookId)
  if (book) book.stockCount += 1
  const reader = mockReaders.find((x) => x.id === r.readerId)
  if (reader) reader.borrowCount = Math.max(0, reader.borrowCount - 1)
  return decorateBorrow(r)
}

export function renewBorrow(id: number): BorrowResult<BorrowRecord> {
  const r = mockBorrowRecords.find((x) => x.id === id)
  if (!r) return { ok: false, message: '借阅记录不存在' }
  if (r.status !== 0 || r.renewCount >= 2) return { ok: false, message: '不可续借' }
  r.renewCount += 1
  const due = new Date(r.dueTime.replace(' ', 'T'))
  due.setDate(due.getDate() + 30)
  r.dueTime = fmtDateTime(due)
  return { ok: true, data: decorateBorrow(r) }
}

export function overdueBorrow(id: number): BorrowRecord | null {
  const r = mockBorrowRecords.find((x) => x.id === id)
  if (!r) return null
  r.status = 2
  return decorateBorrow(r)
}

// ===== 预约记录 =====
let reservationIdSeq = 0
function buildReservation(index: number): Reservation {
  reservationIdSeq += 1
  const readerId = 1 + (index % mockReaders.length)
  const bookId = 1 + (index % mockBooks.length)
  const reserve = daysOffset((index * 2) % 14, 9 + index, (index * 11) % 60)
  const expire = new Date(reserve)
  expire.setDate(expire.getDate() + 7)
  const status = ([0, 1, 2] as const)[index % 3]
  return {
    id: reservationIdSeq,
    readerId,
    bookId,
    reserveTime: fmtDateTime(reserve),
    expireTime: fmtDateTime(expire),
    status,
  }
}

export const mockReservations: Reservation[] = Array.from({ length: 8 }, (_, i) =>
  buildReservation(i),
)

export function decorateReservation(r: Reservation): Reservation {
  const reader = mockReaders.find((x) => x.id === r.readerId)
  const book = mockBooks.find((x) => x.id === r.bookId)
  return {
    ...r,
    readerName: reader?.name ?? '未知读者',
    bookTitle: book?.title ?? '未知图书',
  }
}

export interface ReservationPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number | string
}

export function queryReservationPage(q: ReservationPageQuery): PageResult<Reservation> {
  const keyword = (q.keyword ?? '').trim().toLowerCase()
  let rows = mockReservations.map(decorateReservation)
  if (keyword) {
    rows = rows.filter(
      (r) =>
        (r.readerName ?? '').toLowerCase().includes(keyword) ||
        (r.bookTitle ?? '').toLowerCase().includes(keyword),
    )
  }
  if (q.status !== undefined && q.status !== '') {
    rows = rows.filter((r) => r.status === toNum(q.status))
  }
  return paginate(rows, toNum(q.pageNum, 1), toNum(q.pageSize, 10))
}

export function approveReservation(id: number): BorrowResult<Reservation> {
  const r = mockReservations.find((x) => x.id === id)
  if (!r) return { ok: false, message: '预约记录不存在' }
  if (r.status !== 0) return { ok: false, message: '仅待处理的预约可审批' }
  r.status = 1
  return { ok: true, data: decorateReservation(r) }
}

// ===== 角色 / 系统用户 =====
export interface MockRole {
  id: number
  name: string
  code: string
}
export const mockRoles: MockRole[] = [
  { id: 1, name: '超级管理员', code: 'admin' },
  { id: 2, name: '普通管理员', code: 'manager' },
  { id: 3, name: '读者', code: 'reader' },
]

function roleName(id: number): string {
  return mockRoles.find((r) => r.id === id)?.name ?? '未知角色'
}

let sysUserIdSeq = 0
function buildSysUser(index: number): SysUser {
  sysUserIdSeq += 1
  const roleId = index === 0 ? 1 : index % 3 === 0 ? 3 : 2
  const usernames = [
    'admin',
    'operator1',
    'librarian1',
    'reader1',
    'operator2',
    'librarian2',
    'reader2',
    'auditor',
  ]
  const realNames = ['管理员', '陈晓', '李管理员', '王读者', '刘洋', '赵敏', '孙读者', '周审计']
  return {
    id: sysUserIdSeq,
    username: usernames[index % usernames.length],
    realName: realNames[index % realNames.length],
    phone: `139${String(10000000 + index * 723451).slice(0, 8)}`,
    email: `${usernames[index % usernames.length]}@lib.com`,
    roleId,
    roleName: roleName(roleId),
    status: (index % 7 === 6 ? 0 : 1) as 0 | 1,
    createTime: `2025-${pad2(1 + (index % 12))}-${pad2(1 + (index % 27))} 10:00:00`,
    lastLoginTime: fmtDateTime(daysOffset(index % 5, 8 + (index % 10), (index * 13) % 60)),
  }
}

export const mockSysUsers: SysUser[] = Array.from({ length: 8 }, (_, i) => buildSysUser(i))

export interface SysUserPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  roleId?: number | string
  status?: number | string
}

export function querySysUserPage(q: SysUserPageQuery): PageResult<SysUser> {
  const keyword = (q.keyword ?? '').trim().toLowerCase()
  let rows = [...mockSysUsers]
  if (keyword) {
    rows = rows.filter(
      (u) =>
        u.username.toLowerCase().includes(keyword) ||
        u.realName.toLowerCase().includes(keyword) ||
        (u.phone ?? '').toLowerCase().includes(keyword),
    )
  }
  if (q.roleId !== undefined && q.roleId !== '') {
    rows = rows.filter((u) => u.roleId === toNum(q.roleId))
  }
  if (q.status !== undefined && q.status !== '') {
    rows = rows.filter((u) => u.status === toNum(q.status))
  }
  return paginate(rows, toNum(q.pageNum, 1), toNum(q.pageSize, 10))
}

export function createSysUser(payload: Partial<SysUser>): SysUser {
  sysUserIdSeq += 1
  const roleId = Number(payload.roleId ?? 2)
  const user: SysUser = {
    id: sysUserIdSeq,
    username: payload.username ?? `user${sysUserIdSeq}`,
    realName: payload.realName ?? '新用户',
    phone: payload.phone ?? '',
    email: payload.email ?? '',
    roleId,
    roleName: roleName(roleId),
    status: (payload.status ?? 1) as 0 | 1,
    createTime: now(),
  }
  mockSysUsers.unshift(user)
  return user
}

export function updateSysUser(id: number, payload: Partial<SysUser>): SysUser | null {
  const index = mockSysUsers.findIndex((u) => u.id === id)
  if (index < 0) return null
  const current = mockSysUsers[index]
  const roleId = Number(payload.roleId ?? current.roleId)
  const merged: SysUser = {
    ...current,
    ...payload,
    id,
    roleId,
    roleName: roleName(roleId),
    status: (payload.status ?? current.status) as 0 | 1,
  }
  mockSysUsers.splice(index, 1, merged)
  return merged
}

export function removeSysUser(id: number): boolean {
  const index = mockSysUsers.findIndex((u) => u.id === id)
  if (index < 0) return false
  mockSysUsers.splice(index, 1)
  return true
}

// ===== 操作日志 =====
const logModules = ['图书管理', '读者管理', '借阅管理', '系统设置']
const logActions: Record<string, string[]> = {
  图书管理: ['新增图书', '修改图书', '删除图书', '导入图书'],
  读者管理: ['新增读者', '修改读者', '删除读者', '挂失读者'],
  借阅管理: ['办理借阅', '归还图书', '续借图书', '审批预约'],
  系统设置: ['登录系统', '修改密码', '重置密码', '新增用户'],
}
const logUsernames = ['admin', 'operator1', 'librarian1', 'auditor']

let logIdSeq = 0
function buildLog(index: number): SysLogItem {
  logIdSeq += 1
  const mod = logModules[index % logModules.length]
  const actions = logActions[mod]
  const action = actions[index % actions.length]
  return {
    id: logIdSeq,
    username: logUsernames[index % logUsernames.length],
    module: mod,
    action,
    ip: `192.168.${index % 5}.${10 + ((index * 7) % 240)}`,
    time: fmtDateTime(daysOffset(index % 30, 8 + (index % 12), (index * 17) % 60)),
    status: (index % 11 === 10 ? 0 : 1) as 0 | 1,
  }
}

export const mockLogs: SysLogItem[] = Array.from({ length: 30 }, (_, i) => buildLog(i))

export interface LogPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  module?: string
}

export function queryLogPage(q: LogPageQuery): PageResult<SysLogItem> {
  const keyword = (q.keyword ?? '').trim().toLowerCase()
  let rows = [...mockLogs]
  if (keyword) {
    rows = rows.filter(
      (l) =>
        l.username.toLowerCase().includes(keyword) ||
        l.action.toLowerCase().includes(keyword) ||
        l.ip.toLowerCase().includes(keyword),
    )
  }
  if (q.module) rows = rows.filter((l) => l.module === q.module)
  return paginate(rows, toNum(q.pageNum, 1), toNum(q.pageSize, 10))
}

// ===== 当前登录用户 =====
export const mockLoginUser: LoginUserInfo = {
  id: 1,
  username: 'admin',
  realName: '管理员',
  roles: ['admin'],
  permissions: ['*'],
  avatar: '',
}

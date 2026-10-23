<script setup lang="ts">
// ============================================
// 借阅管理 - 状态页签 + 搜索 + 办理借阅 + 归还/续借/逾期处理
// 数据源: src/api/modules/borrow.ts / reader.ts / book.ts
// ============================================
defineOptions({ name: 'BorrowManage' })

import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import borrowApi, { type BorrowQuery } from '@/api/modules/borrow'
import readerApi from '@/api/modules/reader'
import bookApi from '@/api/modules/book'
import type { Book, BorrowRecord, Reader } from '@/types/api'

// ===== 列表 =====
const loading = ref(false)
const errorMsg = ref('')
const tableData = ref<BorrowRecord[]>([])
const total = ref(0)

// 筛选: 关键词 + 状态下拉(空=全部, 与读者管理/图书列表等页面筛选风格一致)
const filters = reactive({
  keyword: '',
  status: '' as number | string,
  pageNum: 1,
  pageSize: 20,
})

// ===== 表格高度自适应: 撑满视口剩余空间, 分页贴底 =====
const tableHeight = ref(420)

function calcTableHeight() {
  const root = document.querySelector('.borrow-manage') as HTMLElement | null
  if (!root) return
  const title = (root.querySelector('.page-title') as HTMLElement | null)?.offsetHeight ?? 44
  const filter = (root.querySelector('.filter-bar') as HTMLElement | null)?.offsetHeight ?? 0
  const pager = (root.querySelector('.pager') as HTMLElement | null)?.offsetHeight ?? 56
  const margin = 48
  tableHeight.value = Math.max(240, root.clientHeight - title - filter - pager - margin)
}

function onResize() {
  calcTableHeight()
}

function buildParams(): BorrowQuery {
  const params: BorrowQuery = { pageNum: filters.pageNum, pageSize: filters.pageSize }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (filters.status !== '' && filters.status !== undefined) params.status = Number(filters.status) as 0 | 1 | 2 | 3
  return params
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await borrowApi.page(buildParams())
    tableData.value = res.data?.list ?? []
    total.value = res.data?.total ?? 0
  } catch (error) {
    tableData.value = []
    total.value = 0
    errorMsg.value = error instanceof Error ? error.message : '列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadList()
}

function handleReset() {
  filters.keyword = ''
  filters.status = ''
  filters.pageNum = 1
  void loadList()
}

function handlePageChange(page: number) {
  filters.pageNum = page
  void loadList()
}

function handleSizeChange(size: number) {
  filters.pageSize = size
  filters.pageNum = 1
  void loadList()
}

// el-table 作用域插槽的 row 类型为 DefaultRow, 统一在此收敛
function asBorrow(row: unknown): BorrowRecord {
  return row as BorrowRecord
}

type TagType = 'warning' | 'success' | 'danger' | 'info'
// 状态色与全系统语义体系统一:
// 正常态(正常/启用/在架/已通知)=绿 | 结束态(注销/下架/禁用/已取消)=灰
// 异常(逾期)=红 | 待处理(预约/待审批)=橙
function statusTag(status: number): { label: string; type: TagType } {
  switch (status) {
    case 0:
      return { label: '借阅中', type: 'success' }
    case 1:
      return { label: '已归还', type: 'info' }
    case 2:
      return { label: '已逾期', type: 'danger' }
    case 3:
      return { label: '已预约', type: 'warning' }
    default:
      return { label: '未知', type: 'info' }
  }
}

// ===== 办理借阅弹窗 =====
const borrowDialogVisible = ref(false)
const borrowSubmitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<{ readerId: number | undefined; bookId: number | undefined }>({
  readerId: undefined,
  bookId: undefined,
})

const rules: FormRules = {
  readerId: [{ required: true, message: '请选择读者', trigger: 'change' }],
  bookId: [{ required: true, message: '请选择图书', trigger: 'change' }],
}

// 读者下拉
const readers = ref<Reader[]>([])
const readersLoading = ref(false)
// 图书下拉(仅可借)
const books = ref<Book[]>([])
const booksLoading = ref(false)

async function loadReaders() {
  readersLoading.value = true
  try {
    const res = await readerApi.page({ pageNum: 1, pageSize: 100 })
    readers.value = res.data?.list ?? []
  } catch {
    readers.value = []
  } finally {
    readersLoading.value = false
  }
}

async function loadBooks() {
  booksLoading.value = true
  try {
    const res = await bookApi.page({ pageNum: 1, pageSize: 100 }, { silent: true })
    books.value = (res.data?.list ?? []).filter((b) => b.stockCount > 0)
  } catch {
    books.value = []
  } finally {
    booksLoading.value = false
  }
}

function openBorrowDialog() {
  form.readerId = undefined
  form.bookId = undefined
  formRef.value?.clearValidate()
  borrowDialogVisible.value = true
  void loadReaders()
  void loadBooks()
}

async function handleBorrowSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  borrowSubmitting.value = true
  try {
    await borrowApi.create({ readerId: form.readerId!, bookId: form.bookId! })
    ElMessage.success('借阅办理成功')
    borrowDialogVisible.value = false
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出
  } finally {
    borrowSubmitting.value = false
  }
}

// ===== 归还 =====
async function handleReturn(row: BorrowRecord) {
  try {
    await ElMessageBox.confirm(
      `确认为读者「${row.readerName ?? ''}」归还《${row.bookTitle ?? ''}》吗？`,
      '归还确认',
      { type: 'warning', confirmButtonText: '确认归还', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await borrowApi.return(row.id)
    ElMessage.success('归还成功')
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出
  }
}

// ===== 续借 =====
async function handleRenew(row: BorrowRecord) {
  try {
    await borrowApi.renew(row.id)
    ElMessage.success('续借成功')
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出(含次数上限校验)
  }
}

// ===== 删除历史记录(仅已归还可删) =====
async function handleRemove(row: BorrowRecord) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.readerName ?? ''}」借阅《${row.bookTitle ?? ''}》的历史记录吗？删除后不可恢复。`,
      '删除记录',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await borrowApi.remove(row.id)
    ElMessage.success('记录已删除')
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出
  }
}

// ===== 逾期处理弹窗 =====
const overdueDialogVisible = ref(false)
const overdueSubmitting = ref(false)
const overdueForm = reactive<{ id: number; penalty: number }>({ id: 0, penalty: 0 })

function openOverdueDialog(row: BorrowRecord) {
  overdueForm.id = row.id
  overdueForm.penalty = 0
  overdueDialogVisible.value = true
}

async function handleOverdueSubmit() {
  overdueSubmitting.value = true
  try {
    await borrowApi.handleOverdue(overdueForm.id, { penalty: overdueForm.penalty })
    ElMessage.success('逾期罚款完成，记录已归档为已归还')
    overdueDialogVisible.value = false
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出
  } finally {
    overdueSubmitting.value = false
  }
}

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', onResize)
  void loadList()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <div class="page-container borrow-manage">
    <h2 class="page-title">借阅管理</h2>

    <!-- 搜索筛选 -->
    <div class="card filter-bar">
      <div class="filter-item">
        <span class="filter-label">关键词</span>
        <el-input
          v-model="filters.keyword"
          placeholder="读者姓名 / 书名"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-item">
        <span class="filter-label">状态</span>
        <el-select
          v-model="filters.status"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option label="借阅中" :value="0" />
          <el-option label="已归还" :value="1" />
          <el-option label="已逾期" :value="2" />
          <el-option label="已预约" :value="3" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="filter-spacer"></div>
      <el-button type="primary" @click="openBorrowDialog">办理借阅</el-button>
    </div>

    <!-- 数据表格 -->
    <div class="card table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        :height="tableHeight"
      >
        <template #empty>
          <el-empty
            :description="errorMsg ? '数据加载失败，请重试' : '暂无符合条件的借阅记录'"
            :image-size="90"
          />
        </template>
        <el-table-column label="读者" min-width="70" show-overflow-tooltip>
          <template #default="{ row }">{{ asBorrow(row).readerName ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="图书" min-width="90" show-overflow-tooltip>
          <template #default="{ row }">{{ asBorrow(row).bookTitle ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="isbn" label="ISBN" min-width="70" show-overflow-tooltip>
          <template #default="{ row }">{{ asBorrow(row).isbn ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="借阅时间" min-width="70">
          <template #default="{ row }">{{ asBorrow(row).borrowTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="应还时间" min-width="70">
          <template #default="{ row }">{{ asBorrow(row).dueTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="实还时间" min-width="70">
          <template #default="{ row }">{{ asBorrow(row).returnTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(asBorrow(row).status).type" size="small">
              {{ statusTag(asBorrow(row).status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="renewCount" label="续借次数" min-width="70" align="center" />
        <el-table-column label="操作" min-width="80" align="center">
          <template #default="{ row }">
            <template v-if="asBorrow(row).status === 0">
              <el-button link type="primary" @click="handleReturn(asBorrow(row))">归还</el-button>
              <el-button link type="primary" @click="handleRenew(asBorrow(row))">续借</el-button>
            </template>
            <el-button
              v-else-if="asBorrow(row).status === 2"
              link
              type="danger"
              @click="openOverdueDialog(asBorrow(row))"
            >
              逾期处理
            </el-button>
            <el-button
              v-else-if="asBorrow(row).status === 1"
              link
              type="danger"
              @click="handleRemove(asBorrow(row))"
            >
              删除
            </el-button>
            <span v-else class="op-empty">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          :current-page="filters.pageNum"
          :page-size="filters.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 办理借阅弹窗 -->
    <el-dialog
      v-model="borrowDialogVisible"
      title="办理借阅"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="读者" prop="readerId">
          <el-select
            v-model="form.readerId"
            placeholder="请选择读者"
            :loading="readersLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in readers"
              :key="item.id"
              :label="`${item.name}（${item.readerNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图书" prop="bookId">
          <el-select
            v-model="form.bookId"
            placeholder="请选择可借图书"
            :loading="booksLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in books"
              :key="item.id"
              :label="`${item.title}（库存 ${item.stockCount}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="borrowSubmitting" @click="handleBorrowSubmit">
          确认借阅
        </el-button>
      </template>
    </el-dialog>

    <!-- 逾期处理弹窗 -->
    <el-dialog
      v-model="overdueDialogVisible"
      title="逾期处理"
      width="440px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form label-width="92px">
        <el-form-item label="罚款金额(元)">
          <el-input-number
            v-model="overdueForm.penalty"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="overdueDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="overdueSubmitting" @click="handleOverdueSubmit">
          确认处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.borrow-manage {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  overflow: hidden;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.filter-spacer {
  flex: 1;
}

.table-card {
  padding: 16px 20px 20px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-card :deep(.el-table) {
  flex: 1;
  min-height: 0;
}

.op-empty {
  color: var(--text-placeholder);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

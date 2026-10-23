<script setup lang="ts">
// ============================================
// 图书管理 - 搜索筛选 + 分页表格 + 新增/编辑/删除
// 数据源: src/api/modules/book.ts
// ============================================
defineOptions({ name: 'BookList' })

import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import bookApi, { type BookQuery } from '@/api/modules/book'
import type { Book } from '@/types/api'

const router = useRouter()

// 跳转图书详情
function goDetail(row: Book) {
  router.push(`/admin/book/detail/${row.id}`)
}

// ===== 列表 =====
const loading = ref(false)
const errorMsg = ref('')
const tableData = ref<Book[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  categoryId: '' as number | '',
  status: '' as 0 | 1 | '',
  pageNum: 1,
  pageSize: 20,
})

// ===== 表格高度自适应: 撑满视口剩余空间, 避免底部留白 =====
const tableHeight = ref(420)

function calcTableHeight() {
  const root = document.querySelector('.book-list') as HTMLElement | null
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

// ===== 分类下拉 =====
const categories = ref<{ id: number; name: string }[]>([])
const categoryLoadFailed = ref(false)

function buildParams(): BookQuery {
  const params: BookQuery = { pageNum: filters.pageNum, pageSize: filters.pageSize }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (typeof filters.categoryId === 'number') params.categoryId = filters.categoryId
  if (filters.status === 0 || filters.status === 1) params.status = filters.status
  return params
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await bookApi.page(buildParams(), { silent: true })
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

async function loadCategories() {
  try {
    const res = await bookApi.categories({ silent: true })
    categories.value = res.data ?? []
    categoryLoadFailed.value = false
  } catch {
    categories.value = []
    categoryLoadFailed.value = true
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadList()
}

function handleReset() {
  filters.keyword = ''
  filters.categoryId = ''
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

// el-table 作用域插槽的 row 类型为 DefaultRow, 统一在此收敛为 Book
function asBook(row: unknown): Book {
  return row as Book
}

function categoryName(row: Book): string {
  if (row.categoryName) return row.categoryName
  return categories.value.find((item) => item.id === row.categoryId)?.name ?? '—'
}

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', onResize)
  void loadCategories()
  void loadList()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

// ===== 新增 / 编辑 =====
interface BookForm {
  id?: number
  isbn: string
  title: string
  author: string
  publisher: string
  publishDate: string
  categoryId: number | undefined
  totalCount: number
  stockCount: number
  location: string
  status: 0 | 1
  summary: string
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<BookForm>({
  id: undefined,
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  categoryId: undefined,
  totalCount: 1,
  stockCount: 1,
  location: '',
  status: 1,
  summary: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  isbn: [
    { required: true, message: '请输入 ISBN', trigger: 'blur' },
    {
      pattern: /^(97[89])?\d{9}[\dXx]$/,
      message: 'ISBN 需为 10 位或 13 位数字（末位可为 X）',
      trigger: 'blur',
    },
  ],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择图书分类', trigger: 'change' }],
  totalCount: [{ required: true, type: 'number', message: '请输入馆藏总量', trigger: 'blur' }],
  location: [{ required: true, message: '请输入馆藏位置', trigger: 'blur' }],
}

function resetForm() {
  form.id = undefined
  form.isbn = ''
  form.title = ''
  form.author = ''
  form.publisher = ''
  form.publishDate = ''
  form.categoryId = undefined
  form.totalCount = 1
  form.stockCount = 1
  form.location = ''
  form.status = 1
  form.summary = ''
  formRef.value?.clearValidate()
}

function handleCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: Book) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.isbn = row.isbn
  form.title = row.title
  form.author = row.author
  form.publisher = row.publisher ?? ''
  form.publishDate = row.publishDate ?? ''
  form.categoryId = row.categoryId
  form.totalCount = row.totalCount
  form.stockCount = row.stockCount
  form.location = row.location ?? ''
  form.status = row.status
  form.summary = row.summary ?? ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (form.stockCount > form.totalCount) {
    ElMessage.warning('可借数量不能大于馆藏总量')
    return
  }

  const payload: Partial<Book> = {
    isbn: form.isbn.trim(),
    title: form.title.trim(),
    author: form.author.trim(),
    publisher: form.publisher.trim(),
    publishDate: form.publishDate,
    categoryId: form.categoryId,
    totalCount: form.totalCount,
    stockCount: form.stockCount,
    location: form.location.trim(),
    status: form.status,
    summary: form.summary.trim(),
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await bookApi.create(payload)
      ElMessage.success('新增成功')
    } else if (form.id !== undefined) {
      await bookApi.update(form.id, payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出，此处仅保持弹窗打开便于修改
  } finally {
    submitting.value = false
  }
}

// ===== 删除 =====
const deletingId = ref<number | null>(null)

async function handleDelete(row: Book) {
  try {
    await ElMessageBox.confirm(`确定删除《${row.title}》吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await bookApi.remove(row.id)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && filters.pageNum > 1) {
      filters.pageNum -= 1
    }
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <div class="page-container book-list">
    <h2 class="page-title">图书管理</h2>

    <!-- 搜索筛选 -->
    <div class="card filter-bar">
      <div class="filter-item">
        <span class="filter-label">关键词</span>
        <el-input
          v-model="filters.keyword"
          placeholder="书名 / 作者 / ISBN"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-item">
        <span class="filter-label">分类</span>
        <el-select
          v-model="filters.categoryId"
          :placeholder="categoryLoadFailed ? '分类加载失败' : '全部分类'"
          :disabled="categoryLoadFailed"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">状态</span>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="在架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="filter-spacer"></div>
      <el-button type="primary" plain @click="handleCreate">新增图书</el-button>
    </div>

    <!-- 错误提示 -->
    <el-alert
      v-if="errorMsg"
      class="err-alert"
      type="error"
      :closable="false"
      show-icon
      title="图书列表加载失败"
    >
      <template #default>
        <div class="err-body">
          <span>{{ errorMsg }}，请检查后端服务是否已启动。</span>
          <el-button type="primary" size="small" :loading="loading" @click="loadList">
            重新加载
          </el-button>
        </div>
      </template>
    </el-alert>

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
            :description="errorMsg ? '数据加载失败，请重试' : '暂无符合条件的图书'"
            :image-size="90"
          />
        </template>
        <el-table-column label="封面" width="76" align="center">
          <template #default="{ row }">
            <el-image v-if="row.cover" :src="row.cover" fit="cover" class="cover" />
            <span v-else class="cover-empty">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="isbn" label="ISBN" min-width="70" show-overflow-tooltip />
        <el-table-column label="书名" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="goDetail(asBook(row))">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" min-width="60" show-overflow-tooltip />
        <el-table-column prop="publisher" label="出版社" min-width="80" show-overflow-tooltip />
        <el-table-column label="分类" min-width="60">
          <template #default="{ row }">{{ categoryName(asBook(row)) }}</template>
        </el-table-column>
        <el-table-column label="库存(可借/馆藏)" min-width="70" align="center">
          <template #default="{ row }">
            <span :class="{ 'stock-warn': row.stockCount === 0 }">
              {{ row.stockCount }} / {{ row.totalCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="70" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(asBook(row))">编辑</el-button>
            <el-button
              link
              type="danger"
              :loading="deletingId === row.id"
              @click="handleDelete(asBook(row))"
            >
              删除
            </el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增图书' : '编辑图书'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="书名" prop="title">
              <el-input v-model="form.title" placeholder="请输入书名" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="请输入 ISBN" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版社" prop="publisher">
              <el-input v-model="form.publisher" placeholder="请输入出版社" maxlength="60" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option
                  v-for="item in categories"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版日期" prop="publishDate">
              <el-date-picker
                v-model="form.publishDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择出版日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="馆藏总量" prop="totalCount">
              <el-input-number v-model="form.totalCount" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可借数量" prop="stockCount">
              <el-input-number
                v-model="form.stockCount"
                :min="0"
                :max="form.totalCount"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">在架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="馆藏位置" prop="location">
          <el-input v-model="form.location" placeholder="如 A区-3排-2层" maxlength="60" />
        </el-form-item>

        <el-form-item label="简介" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="图书简介（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
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

.err-alert {
  margin-bottom: 16px;
}

.err-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.book-list {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  overflow: hidden;
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

.cover {
  width: 40px;
  height: 52px;
  border-radius: 4px;
  display: block;
  margin: 0 auto;
}

.cover-empty {
  font-size: 12px;
  color: var(--text-placeholder);
}

.title-link {
  color: var(--color-primary);
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.75;
    text-decoration: underline;
  }
}

.stock-warn {
  color: var(--color-danger);
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

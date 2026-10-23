<script setup lang="ts">
// ============================================
// 读者管理 - 搜索筛选 + 分页表格 + 新增/编辑/删除
// 数据源: src/api/modules/reader.ts
// ============================================
defineOptions({ name: 'ReaderManage' })

import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import readerApi, { type ReaderQuery } from '@/api/modules/reader'
import type { Reader } from '@/types/api'

// ===== 读者类型固定选项 =====
const READER_TYPES = ['学生', '教师', '职工', '校外'] as const

// ===== 列表 =====
const loading = ref(false)
const errorMsg = ref('')
const tableData = ref<Reader[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  readerType: '' as string,
  status: '' as 0 | 1 | '',
  pageNum: 1,
  pageSize: 20,
})

// ===== 表格高度自适应: 撑满视口剩余空间, 避免底部留白 =====
const tableHeight = ref(420)

function calcTableHeight() {
  const root = document.querySelector('.reader-manage') as HTMLElement | null
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

function buildParams(): ReaderQuery {
  const params: ReaderQuery = { pageNum: filters.pageNum, pageSize: filters.pageSize }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (filters.readerType) params.readerType = filters.readerType
  if (filters.status === 0 || filters.status === 1) params.status = filters.status
  return params
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await readerApi.page(buildParams())
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
  filters.readerType = ''
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

// el-table 作用域插槽的 row 类型为 DefaultRow, 统一在此收敛为 Reader
function asReader(row: unknown): Reader {
  return row as Reader
}

function genderText(gender: 0 | 1): string {
  return gender === 1 ? '男' : '女'
}

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', onResize)
  void loadList()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

// ===== 新增 / 编辑 =====
interface ReaderForm {
  id?: number
  name: string
  gender: 0 | 1
  readerType: string
  phone: string
  email: string
  maxBorrow: number
  status: 0 | 1
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ReaderForm>({
  id: undefined,
  name: '',
  gender: 1,
  readerType: READER_TYPES[0],
  phone: '',
  email: '',
  maxBorrow: 5,
  status: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  readerType: [{ required: true, message: '请选择读者类型', trigger: 'change' }],
}

function resetForm() {
  form.id = undefined
  form.name = ''
  form.gender = 1
  form.readerType = READER_TYPES[0]
  form.phone = ''
  form.email = ''
  form.maxBorrow = 5
  form.status = 1
  formRef.value?.clearValidate()
}

function handleCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: Reader) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.name = row.name
  form.gender = row.gender
  form.readerType = row.readerType || READER_TYPES[0]
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  form.maxBorrow = row.maxBorrow
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  const payload: Partial<Reader> = {
    name: form.name.trim(),
    gender: form.gender,
    readerType: form.readerType,
    phone: form.phone.trim(),
    email: form.email.trim(),
    maxBorrow: form.maxBorrow,
    status: form.status,
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await readerApi.create(payload)
      ElMessage.success('新增成功')
    } else if (form.id !== undefined) {
      await readerApi.update(form.id, payload)
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

async function handleDelete(row: Reader) {
  try {
    await ElMessageBox.confirm(`确定删除读者「${row.name}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await readerApi.remove(row.id)
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
  <div class="page-container reader-manage">
    <h2 class="page-title">读者管理</h2>

    <!-- 搜索筛选 -->
    <div class="card filter-bar">
      <div class="filter-item">
        <span class="filter-label">关键词</span>
        <el-input
          v-model="filters.keyword"
          placeholder="姓名 / 读者号 / 手机"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-item">
        <span class="filter-label">读者类型</span>
        <el-select
          v-model="filters.readerType"
          placeholder="全部类型"
          clearable
          style="width: 150px"
        >
          <el-option v-for="t in READER_TYPES" :key="t" :label="t" :value="t" />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">状态</span>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="正常" :value="1" />
          <el-option label="注销" :value="0" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="filter-spacer"></div>
      <el-button type="primary" plain @click="handleCreate">新增读者</el-button>
    </div>

    <!-- 错误提示 -->
    <el-alert
      v-if="errorMsg"
      class="err-alert"
      type="error"
      :closable="false"
      show-icon
      title="读者列表加载失败"
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
            :description="errorMsg ? '数据加载失败，请重试' : '暂无符合条件的读者'"
            :image-size="90"
          />
        </template>
        <el-table-column prop="readerNo" label="读者号" min-width="70" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="70" show-overflow-tooltip />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(asReader(row).gender) }}</template>
        </el-table-column>
        <el-table-column prop="readerType" label="类型" width="70" align="center" />
        <el-table-column prop="phone" label="手机" min-width="80" show-overflow-tooltip />
        <el-table-column label="已借/可借" min-width="70" align="center">
          <template #default="{ row }">
            <span :class="{ 'over-borrow': asReader(row).borrowCount >= asReader(row).maxBorrow }">
              {{ asReader(row).borrowCount }} / {{ asReader(row).maxBorrow }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="asReader(row).status === 1 ? 'success' : 'info'" size="small">
              {{ asReader(row).status === 1 ? '正常' : '注销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(asReader(row))">编辑</el-button>
            <el-button
              link
              type="danger"
              :loading="deletingId === asReader(row).id"
              @click="handleDelete(asReader(row))"
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
      :title="dialogMode === 'create' ? '新增读者' : '编辑读者'"
      width="620px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="读者类型" prop="readerType">
              <el-select v-model="form.readerType" placeholder="请选择类型" style="width: 100%">
                <el-option v-for="t in READER_TYPES" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大可借" prop="maxBorrow">
              <el-input-number v-model="form.maxBorrow" :min="1" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱（选填）" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">注销</el-radio>
          </el-radio-group>
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
.reader-manage {
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

.over-borrow {
  color: var(--color-danger);
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

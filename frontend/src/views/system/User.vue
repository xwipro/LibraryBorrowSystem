<script setup lang="ts">
// ============================================
// 系统管理 - 用户管理
// 搜索(关键词/角色/状态) + 分页表格 + 新增/编辑 + 重置密码 + 删除
// 数据源: src/api/modules/system.ts
// ============================================
defineOptions({ name: 'SystemUser' })

import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import systemApi from '@/api/modules/system'
import type { SysUser } from '@/types/api'

type RoleItem = { id: number; name: string; code: string }

// ===== 列表 =====
const loading = ref(false)
const errorMsg = ref('')
const tableData = ref<SysUser[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  roleId: '' as number | '',
  status: '' as 0 | 1 | '',
  pageNum: 1,
  pageSize: 20,
})

// ===== 表格高度自适应: 撑满视口剩余空间, 避免底部留白 =====
const tableHeight = ref(420)

function calcTableHeight() {
  const root = document.querySelector('.system-user') as HTMLElement | null
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

// ===== 角色下拉 =====
const roles = ref<RoleItem[]>([])
const roleLoadFailed = ref(false)

function buildParams() {
  const params: {
    pageNum: number
    pageSize: number
    keyword?: string
    roleId?: number
    status?: 0 | 1
  } = { pageNum: filters.pageNum, pageSize: filters.pageSize }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (typeof filters.roleId === 'number') params.roleId = filters.roleId
  if (filters.status === 0 || filters.status === 1) params.status = filters.status
  return params
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.userPage(buildParams())
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

async function loadRoles() {
  try {
    const res = await systemApi.roleList()
    roles.value = res.data ?? []
    roleLoadFailed.value = false
  } catch {
    roles.value = []
    roleLoadFailed.value = true
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadList()
}

function handleReset() {
  filters.keyword = ''
  filters.roleId = ''
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

// el-table 作用域插槽的 row 类型为 DefaultRow, 统一在此收敛为 SysUser
function asUser(row: unknown): SysUser {
  return row as SysUser
}

// 超级管理员: 不可删除; 重置密码需本人输入(而非随机生成)
function isSuperAdmin(row: SysUser): boolean {
  return row.roleName === '超级管理员'
}

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', onResize)
  void loadRoles()
  void loadList()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

// ===== 新增 / 编辑 =====
interface UserForm {
  id?: number
  username: string
  realName: string
  phone: string
  email: string
  roleId: number | undefined
  status: 0 | 1
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<UserForm>({
  id: undefined,
  username: '',
  realName: '',
  phone: '',
  email: '',
  roleId: undefined,
  status: 1,
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20 位字母、数字或下划线', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function resetForm() {
  form.id = undefined
  form.username = ''
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.roleId = undefined
  form.status = 1
  formRef.value?.clearValidate()
}

function handleCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: SysUser) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.username = row.username
  form.realName = row.realName
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  form.roleId = row.roleId
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

  const payload: Partial<SysUser> = {
    username: form.username.trim(),
    realName: form.realName.trim(),
    phone: form.phone.trim(),
    email: form.email.trim(),
    roleId: form.roleId,
    status: form.status,
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await systemApi.userCreate(payload)
      ElMessage.success('新增成功')
    } else if (form.id !== undefined) {
      await systemApi.userUpdate(form.id, payload)
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

// ===== 重置密码 =====
const resettingId = ref<number | null>(null)

// 超级管理员: 由本人输入新密码(不能被随机重置, 保证账号绝对安全)
async function handleResetPassword(row: SysUser) {
  if (isSuperAdmin(row)) {
    let newPassword: string
    try {
      const { value } = await ElMessageBox.prompt('超级管理员密码由本人设置，请输入新密码：', '重置密码', {
        inputType: 'password',
        inputPattern: /^.{6,32}$/,
        inputErrorMessage: '密码长度需为 6-32 位',
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入 6-32 位新密码',
      })
      newPassword = value.trim()
    } catch {
      return
    }
    resettingId.value = row.id
    try {
      await systemApi.resetPassword(row.id, { newPassword })
      ElMessage.success('超级管理员密码已更新')
    } catch {
      // 失败提示由 request 拦截器统一弹出
    } finally {
      resettingId.value = null
    }
    return
  }

  // 普通用户: 随机生成密码并通过邮件发送, 管理面板不展示
  try {
    await ElMessageBox.confirm(
      `确定重置用户「${row.username}」的密码吗？系统将随机生成新密码并通过邮件发送至 ${row.email ?? '其邮箱'}，重置后原密码立即失效。`,
      '重置密码',
      { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  resettingId.value = row.id
  try {
    await systemApi.resetPassword(row.id)
    ElMessage.success('重置成功，新密码已发送至用户邮箱')
  } catch {
    // 失败提示由 request 拦截器统一弹出
  } finally {
    resettingId.value = null
  }
}

// ===== 删除 =====
const deletingId = ref<number | null>(null)

async function handleDelete(row: SysUser) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await systemApi.userRemove(row.id)
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
  <div class="page-container system-user">
    <h2 class="page-title">用户管理</h2>

    <!-- 搜索筛选 -->
    <div class="card filter-bar">
      <div class="filter-item">
        <span class="filter-label">关键词</span>
        <el-input
          v-model="filters.keyword"
          placeholder="用户名 / 姓名"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-item">
        <span class="filter-label">角色</span>
        <el-select
          v-model="filters.roleId"
          :placeholder="roleLoadFailed ? '角色加载失败' : '全部角色'"
          :disabled="roleLoadFailed"
          clearable
          style="width: 160px"
        >
          <el-option v-for="item in roles" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">状态</span>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="filter-spacer"></div>
      <el-button type="primary" @click="handleCreate">新增用户</el-button>
    </div>

    <!-- 错误提示 -->
    <el-alert
      v-if="errorMsg"
      class="err-alert"
      type="error"
      :closable="false"
      show-icon
      title="用户列表加载失败"
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
            :description="errorMsg ? '数据加载失败，请重试' : '暂无符合条件的用户'"
            :image-size="90"
          />
        </template>
        <el-table-column prop="username" label="用户名" min-width="80" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="70" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="80">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="90" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="70" show-overflow-tooltip>
          <template #default="{ row }">{{ row.roleName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="90">
          <template #default="{ row }">{{ row.lastLoginTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="110" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(asUser(row))">编辑</el-button>
            <el-button
              link
              type="warning"
              :loading="resettingId === row.id"
              @click="handleResetPassword(asUser(row))"
            >
              重置密码
            </el-button>
            <el-button
              v-if="!isSuperAdmin(asUser(row))"
              link
              type="danger"
              :loading="deletingId === row.id"
              @click="handleDelete(asUser(row))"
            >
              删除
            </el-button>
            <!-- 超管不可删除: 渲染等宽占位, 保证操作列右缘与其他行对齐 -->
            <el-button v-else link type="danger" class="op-placeholder" tabindex="-1" aria-hidden="true">
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
      :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
      width="620px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                :disabled="dialogMode === 'edit'"
                maxlength="20"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入姓名" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="60" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="角色" prop="roleId">
              <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
                <el-option
                  v-for="item in roles"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.system-user {
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

// 超管行删除按钮占位: 保留等宽布局, 视觉不可见, 使操作列右缘与其他行对齐
.op-placeholder {
  visibility: hidden;
  pointer-events: none;
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

<script setup lang="ts">
// ============================================
// 预约管理 - 关键词 + 状态筛选 + 审批通过
// 数据源: src/api/modules/borrow.ts (reservationPage / approveReservation)
// ============================================
defineOptions({ name: 'ReservationManage' })

import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import borrowApi from '@/api/modules/borrow'
import type { Reservation } from '@/types/api'

// ===== 列表 =====
const loading = ref(false)
const errorMsg = ref('')
const tableData = ref<Reservation[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  status: '' as '' | 0 | 1 | 2,
  pageNum: 1,
  pageSize: 20,
})

// ===== 表格高度自适应: 撑满视口剩余空间, 分页贴底 =====
const tableHeight = ref(420)

function calcTableHeight() {
  const root = document.querySelector('.reservation-manage') as HTMLElement | null
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

function buildParams() {
  const params: { pageNum: number; pageSize: number; keyword?: string; status?: 0 | 1 | 2 } = {
    pageNum: filters.pageNum,
    pageSize: filters.pageSize,
  }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (filters.status === 0 || filters.status === 1 || filters.status === 2) {
    params.status = filters.status
  }
  return params
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await borrowApi.reservationPage(buildParams())
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
function asReservation(row: unknown): Reservation {
  return row as Reservation
}

type TagType = 'warning' | 'success' | 'info'
function statusTag(status: number): { label: string; type: TagType } {
  switch (status) {
    case 0:
      return { label: '待处理', type: 'warning' }
    case 1:
      return { label: '已通知', type: 'success' }
    case 2:
      return { label: '已取消', type: 'info' }
    default:
      return { label: '未知', type: 'info' }
  }
}

// ===== 审批通过 =====
const approvingId = ref<number | null>(null)

async function handleApprove(row: Reservation) {
  try {
    await ElMessageBox.confirm(
      `确认通过读者「${row.readerName ?? ''}」对《${row.bookTitle ?? ''}》的预约吗？`,
      '审批确认',
      { type: 'warning', confirmButtonText: '审批通过', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  approvingId.value = row.id
  try {
    await borrowApi.approveReservation(row.id)
    ElMessage.success('已审批通过')
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出
  } finally {
    approvingId.value = null
  }
}

// ===== 删除历史记录(仅已通知/已取消可删, 待处理不可删) =====
const removingId = ref<number | null>(null)

async function handleRemove(row: Reservation) {
  try {
    await ElMessageBox.confirm(
      `确定删除读者「${row.readerName ?? ''}」对《${row.bookTitle ?? ''}》的预约记录吗？删除后不可恢复。`,
      '删除记录',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  removingId.value = row.id
  try {
    await borrowApi.removeReservation(row.id)
    ElMessage.success('记录已删除')
    await loadList()
  } catch {
    // 失败提示由 request 拦截器统一弹出
  } finally {
    removingId.value = null
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
  <div class="page-container reservation-manage">
    <h2 class="page-title">预约管理</h2>

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
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="待处理" :value="0" />
          <el-option label="已通知" :value="1" />
          <el-option label="已取消" :value="2" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
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
            :description="errorMsg ? '数据加载失败，请重试' : '暂无符合条件的预约记录'"
            :image-size="90"
          />
        </template>
        <el-table-column prop="id" label="预约号" width="70" align="center" />
        <el-table-column label="读者" min-width="70" show-overflow-tooltip>
          <template #default="{ row }">{{ asReservation(row).readerName ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="图书" min-width="90" show-overflow-tooltip>
          <template #default="{ row }">{{ asReservation(row).bookTitle ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="90">
          <template #default="{ row }">{{ asReservation(row).reserveTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="过期时间" min-width="90">
          <template #default="{ row }">{{ asReservation(row).expireTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(asReservation(row).status).type" size="small">
              {{ statusTag(asReservation(row).status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="80" align="center">
          <template #default="{ row }">
            <el-button
              v-if="asReservation(row).status === 0"
              link
              type="primary"
              :loading="approvingId === asReservation(row).id"
              @click="handleApprove(asReservation(row))"
            >
              审批通过
            </el-button>
            <el-button
              v-else-if="asReservation(row).status === 1 || asReservation(row).status === 2"
              link
              type="danger"
              :loading="removingId === asReservation(row).id"
              @click="handleRemove(asReservation(row))"
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
  </div>
</template>

<style scoped lang="scss">
.reservation-manage {
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

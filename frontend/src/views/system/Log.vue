<script setup lang="ts">
// ============================================
// 系统管理 - 操作日志
// 只读列表: 关键词(操作人/操作内容) + 模块筛选 + 分页
// 数据源: src/api/modules/system.ts (logPage)
// ============================================
defineOptions({ name: 'SystemLog' })

import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import systemApi, { type SysLogItem } from '@/api/modules/system'

// ===== 固定模块选项 =====
const moduleOptions = ['图书管理', '读者管理', '借阅管理', '预约管理', '系统设置']

// ===== 列表 =====
const loading = ref(false)
const errorMsg = ref('')
const tableData = ref<SysLogItem[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  module: '',
  pageNum: 1,
  pageSize: 20,
})

// ===== 表格高度自适应: 撑满视口剩余空间, 避免底部留白 =====
const tableHeight = ref(420)

function calcTableHeight() {
  const root = document.querySelector('.system-log') as HTMLElement | null
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
  const params: { pageNum: number; pageSize: number; keyword?: string; module?: string } = {
    pageNum: filters.pageNum,
    pageSize: filters.pageSize,
  }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (filters.module) params.module = filters.module
  return params
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.logPage(buildParams())
    tableData.value = res.data?.list ?? []
    total.value = res.data?.total ?? 0
  } catch (error) {
    tableData.value = []
    total.value = 0
    errorMsg.value = error instanceof Error ? error.message : '日志列表加载失败'
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
  filters.module = ''
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

// el-table 作用域插槽的 row 类型为 DefaultRow, 统一在此收敛为 SysLogItem
function asLog(row: unknown): SysLogItem {
  return row as SysLogItem
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
  <div class="page-container system-log">
    <h2 class="page-title">操作日志</h2>

    <!-- 搜索筛选 -->
    <div class="card filter-bar">
      <div class="filter-item">
        <span class="filter-label">关键词</span>
        <el-input
          v-model="filters.keyword"
          placeholder="操作人 / 操作内容"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-item">
        <span class="filter-label">模块</span>
        <el-select v-model="filters.module" placeholder="全部模块" clearable style="width: 160px">
          <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 错误提示 -->
    <el-alert
      v-if="errorMsg"
      class="err-alert"
      type="error"
      :closable="false"
      show-icon
      title="日志列表加载失败"
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
            :description="errorMsg ? '数据加载失败，请重试' : '暂无符合条件的日志'"
            :image-size="90"
          />
        </template>
        <el-table-column prop="time" label="时间" min-width="130" show-overflow-tooltip />
        <el-table-column prop="username" label="操作用户" min-width="90" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" min-width="90" show-overflow-tooltip />
        <el-table-column prop="action" label="操作" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" min-width="100" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="asLog(row).status === 1 ? 'success' : 'danger'" size="small">
              {{ asLog(row).status === 1 ? '成功' : '失败' }}
            </el-tag>
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
.system-log {
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

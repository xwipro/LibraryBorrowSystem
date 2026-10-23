<script setup lang="ts">
// ============================================
// 系统管理 - 角色管理
// 角色列表(roleList) + 配置权限(el-tree 静态权限树, 仅视觉)
// 数据源: src/api/modules/system.ts
// ============================================
defineOptions({ name: 'SystemRole' })

import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import systemApi from '@/api/modules/system'

type RoleItem = { id: number; name: string; code: string }

// ===== 角色列表 =====
const loading = ref(false)
const errorMsg = ref('')
const roles = ref<RoleItem[]>([])

// ===== 表格高度自适应: 撑满视口剩余空间, 避免底部留白 =====
const tableHeight = ref(420)

function calcTableHeight() {
  const root = document.querySelector('.system-role') as HTMLElement | null
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

async function loadRoles() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await systemApi.roleList()
    roles.value = res.data ?? []
  } catch (error) {
    roles.value = []
    errorMsg.value = error instanceof Error ? error.message : '角色列表加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', onResize)
  void loadRoles()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

// ===== 配置权限(静态权限树, 仅视觉展示) =====
interface PermNode {
  label: string
  key: string
  children?: PermNode[]
}

const permTree: PermNode[] = [
  {
    label: '系统管理',
    key: 'system',
    children: [
      { label: '用户管理', key: 'system:user' },
      { label: '角色管理', key: 'system:role' },
      { label: '操作日志', key: 'system:log' },
    ],
  },
  {
    label: '图书管理',
    key: 'book',
    children: [
      { label: '图书列表', key: 'book:list' },
      { label: '图书详情', key: 'book:detail' },
    ],
  },
  {
    label: '借阅管理',
    key: 'borrow',
    children: [
      { label: '借阅办理', key: 'borrow:handle' },
      { label: '预约审批', key: 'borrow:approve' },
    ],
  },
  {
    label: '读者管理',
    key: 'reader',
  },
]

const permDialogVisible = ref(false)
const currentRole = ref<RoleItem | null>(null)
const treeRef = ref<InstanceType<typeof ElTree>>()

function handleConfigPerm(row: RoleItem) {
  currentRole.value = row
  permDialogVisible.value = true
}

function handlePermSubmit() {
  // 仅做视觉展示, 不真正请求后端
  ElMessage.success('权限已保存')
  permDialogVisible.value = false
}
</script>

<template>
  <div class="page-container system-role">
    <h2 class="page-title">角色管理</h2>

    <!-- 错误提示 -->
    <el-alert
      v-if="errorMsg"
      class="err-alert"
      type="error"
      :closable="false"
      show-icon
      title="角色列表加载失败"
    >
      <template #default>
        <div class="err-body">
          <span>{{ errorMsg }}，请检查后端服务是否已启动。</span>
          <el-button type="primary" size="small" :loading="loading" @click="loadRoles">
            重新加载
          </el-button>
        </div>
      </template>
    </el-alert>

    <!-- 角色表格 -->
    <div class="card table-card">
      <el-table
        v-loading="loading"
        :data="roles"
        border
        stripe
        style="width: 100%"
        :height="tableHeight"
      >
        <template #empty>
          <el-empty
            :description="errorMsg ? '数据加载失败，请重试' : '暂无角色'"
            :image-size="90"
          />
        </template>
        <el-table-column prop="id" label="ID" width="100" align="center" />
        <el-table-column prop="name" label="角色名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" label="角色编码" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" min-width="160" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleConfigPerm(row as RoleItem)">
              配置权限
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 配置权限弹窗 -->
    <el-dialog
      v-model="permDialogVisible"
      :title="`配置权限 - ${currentRole?.name ?? ''}`"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-tree
        ref="treeRef"
        :data="permTree"
        show-checkbox
        node-key="key"
        :props="{ label: 'label', children: 'children' }"
        default-expand-all
        class="perm-tree"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.system-role {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  overflow: hidden;
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
  padding: 16px 20px;
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

.perm-tree {
  max-height: 360px;
  overflow-y: auto;
  padding: 8px 4px;
}
</style>

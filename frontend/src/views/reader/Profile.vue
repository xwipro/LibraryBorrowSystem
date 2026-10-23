<script setup lang="ts">
// ============================================
// 读者详情页 - 从 route.query.id 取读者信息
// 数据源: src/api/modules/reader.ts
// ============================================
defineOptions({ name: 'ReaderProfile' })

import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import readerApi from '@/api/modules/reader'
import type { Reader } from '@/types/api'

const route = useRoute()

const loading = ref(false)
const detail = ref<Reader | null>(null)
const errorMsg = ref('')

function genderText(gender: 0 | 1 | undefined): string {
  return gender === 1 ? '男' : '女'
}

async function loadDetail() {
  const id = Number(route.query.id ?? 1) || 1
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await readerApi.detail(id)
    detail.value = res.data ?? null
  } catch (error) {
    detail.value = null
    errorMsg.value = error instanceof Error ? error.message : '读者信息加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <div class="page-container reader-profile">
    <div class="page-head">
      <h2 class="page-title" style="margin-bottom: 0">个人中心</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <!-- 加载失败 -->
    <el-result
      v-if="errorMsg"
      icon="error"
      title="读者信息加载失败"
      :sub-title="errorMsg + '，请检查后端服务是否已启动。'"
    >
      <template #extra>
        <el-button type="primary" @click="loadDetail">重新加载</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- 基本信息 -->
      <div v-loading="loading" class="card">
        <el-descriptions v-if="detail" title="基本信息" :column="2" border style="max-width: 900px">
          <el-descriptions-item label="读者号">{{ detail.readerNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(detail.gender) }}</el-descriptions-item>
          <el-descriptions-item label="读者类型">{{ detail.readerType }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ detail.phone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
          <el-descriptions-item label="已借 / 可借额度">
            {{ detail.borrowCount }} / {{ detail.maxBorrow }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small">
              {{ detail.status === 1 ? '正常' : '注销' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ detail.createTime || '—' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else-if="!loading" description="未找到该读者信息" :image-size="90" />
      </div>

      <!-- 在借情况（占位） -->
      <div class="card">
        <h3 class="section-title">在借情况</h3>
        <p class="placeholder-text">在借图书列表将在此展示（借阅记录接口接入后启用）。</p>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card {
  padding: 20px;
  margin-bottom: 16px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--text-primary);
}

.placeholder-text {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>

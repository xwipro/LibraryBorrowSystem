<script setup lang="ts">
// ============================================
// 图书详情页（管理端）- 路由 /admin/book/detail/:id
// 数据源: src/api/modules/book.ts (detail)
// ============================================
defineOptions({ name: 'BookDetailAdmin' })

import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import bookApi from '@/api/modules/book'
import type { Book } from '@/types/api'

const route = useRoute()

const loading = ref(false)
const detail = ref<Book | null>(null)
const errorMsg = ref('')

function firstChar(title: string): string {
  return (title || '书').trim().charAt(0)
}

async function loadDetail() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    errorMsg.value = '图书 id 无效'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await bookApi.detail(id, { silent: true })
    detail.value = res.data ?? null
  } catch (error) {
    detail.value = null
    errorMsg.value = error instanceof Error ? error.message : '图书信息加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <div class="page-container book-detail">
    <div class="page-head">
      <h2 class="page-title" style="margin-bottom: 0">图书详情</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" v-loading="true" class="card loading-card"></div>

    <!-- 加载失败 / 未找到 -->
    <el-result
      v-else-if="errorMsg || !detail"
      icon="warning"
      title="未找到该图书"
      :sub-title="errorMsg || '请检查访问链接是否正确，或图书已被删除。'"
    >
      <template #extra>
        <el-button type="primary" @click="loadDetail">重新加载</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- 上部：封面 + 基本信息 -->
      <div class="card main-card">
        <div class="cover-block">
          <el-image v-if="detail.cover" :src="detail.cover" fit="cover" class="cover-img" />
          <div v-else class="cover-fallback">
            <span class="cover-fallback-text">{{ firstChar(detail.title) }}</span>
          </div>
        </div>

        <el-descriptions class="info" title="基本信息" :column="2" border>
          <el-descriptions-item label="ISBN">{{ detail.isbn }}</el-descriptions-item>
          <el-descriptions-item label="书名">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ detail.author }}</el-descriptions-item>
          <el-descriptions-item label="出版社">{{ detail.publisher || '—' }}</el-descriptions-item>
          <el-descriptions-item label="出版日期">
            {{ detail.publishDate || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            {{ detail.categoryName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="库存(可借/馆藏)">
            {{ detail.stockCount }} / {{ detail.totalCount }}
          </el-descriptions-item>
          <el-descriptions-item label="位置">{{ detail.location || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small">
              {{ detail.status === 1 ? '在架' : '下架' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评分">
            <span class="rating-star">★</span>
            {{ detail.rating ?? '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 下部：简介 -->
      <div class="card">
        <h3 class="section-title">内容简介</h3>
        <p v-if="detail.summary" class="summary">{{ detail.summary }}</p>
        <p v-else class="summary empty">暂无简介</p>
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

.loading-card {
  min-height: 300px;
}

.main-card {
  display: flex;
  gap: 24px;
  padding: 24px;
  margin-bottom: 16px;
}

.cover-block {
  flex: 0 0 200px;
  height: 260px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-gradient-card);
}

.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-fallback-text {
  font-size: 72px;
  font-weight: 600;
  color: var(--brand-primary);
}

.info {
  flex: 1;
  min-width: 0;
}

.rating-star {
  color: var(--color-warning);
  margin-right: 4px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--text-primary);
}

.summary {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.summary.empty {
  color: var(--text-placeholder);
}
</style>

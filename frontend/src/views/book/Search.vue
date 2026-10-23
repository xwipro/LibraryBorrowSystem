<script setup lang="ts">
// ============================================
// 读者端 - 图书检索页
// 数据源: src/api/modules/book.ts (page / categories)
// ============================================
defineOptions({ name: 'BookSearch' })

import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import bookApi, { type BookQuery } from '@/api/modules/book'
import type { Book } from '@/types/api'

const router = useRouter()

// ===== 列表 =====
const loading = ref(false)
const tableData = ref<Book[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  categoryId: '' as number | '',
  pageNum: 1,
  pageSize: 12,
})

// ===== 分类下拉 =====
const categories = ref<{ id: number; name: string }[]>([])

function buildParams(): BookQuery {
  const params: BookQuery = { pageNum: filters.pageNum, pageSize: filters.pageSize }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (typeof filters.categoryId === 'number') params.categoryId = filters.categoryId
  return params
}

async function loadList() {
  loading.value = true
  try {
    const res = await bookApi.page(buildParams(), { silent: true })
    tableData.value = res.data?.list ?? []
    total.value = res.data?.total ?? 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await bookApi.categories({ silent: true })
    categories.value = res.data ?? []
  } catch {
    categories.value = []
  }
}

function handleSearch() {
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

// 无封面时用书名首字做色块
function firstChar(title: string): string {
  return (title || '书').trim().charAt(0)
}

function goDetail(book: Book) {
  void router.push({ path: '/admin/book/detail/' + book.id })
}

onMounted(() => {
  void loadCategories()
  void loadList()
})
</script>

<template>
  <div class="page-container book-search">
    <h2 class="page-title">图书检索</h2>

    <!-- 搜索筛选 -->
    <div class="card filter-bar">
      <div class="filter-item">
        <span class="filter-label">关键词</span>
        <el-input
          v-model="filters.keyword"
          placeholder="书名 / 作者 / ISBN"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-item">
        <span class="filter-label">分类</span>
        <el-select
          v-model="filters.categoryId"
          placeholder="全部分类"
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
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <!-- 结果网格 -->
    <div v-loading="loading" class="grid-wrap">
      <div v-if="!loading && tableData.length === 0" class="empty-wrap">
        <el-empty description="未找到符合条件的图书" :image-size="90" />
      </div>

      <div v-else class="book-grid">
        <div v-for="book in tableData" :key="book.id" class="book-card" @click="goDetail(book)">
          <div class="cover">
            <el-image v-if="book.cover" :src="book.cover" fit="cover" class="cover-img" />
            <span v-else class="cover-fallback">{{ firstChar(book.title) }}</span>
          </div>
          <div class="info">
            <p class="title" :title="book.title">{{ book.title }}</p>
            <p class="meta">{{ book.author || '佚名' }}</p>
            <div class="tags">
              <el-tag size="small" effect="plain">{{ book.categoryName || '—' }}</el-tag>
              <el-tag v-if="book.stockCount > 0" type="success" size="small" effect="plain">
                在架
              </el-tag>
              <el-tag v-else type="info" size="small" effect="plain">无库存</el-tag>
            </div>
            <div class="bottom">
              <span class="rating">
                <span class="rating-star">★</span>
                {{ book.rating ?? '—' }}
              </span>
              <span class="stock">库存 {{ book.stockCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pager">
      <el-pagination
        :current-page="filters.pageNum"
        :page-size="filters.pageSize"
        :page-sizes="[12, 24, 48]"
        :total="total"
        background
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
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

.grid-wrap {
  min-height: 200px;
}

.empty-wrap {
  padding: 40px 0;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.book-card {
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-card);
  }
}

.cover {
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-gradient-card);
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-fallback {
  font-size: 44px;
  font-weight: 600;
  color: var(--brand-primary);
}

.info {
  padding: 12px;
}

.title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 12px;
}

.rating {
  color: var(--text-regular);
}

.rating-star {
  color: var(--color-warning);
  margin-right: 2px;
}

.stock {
  color: var(--text-secondary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>

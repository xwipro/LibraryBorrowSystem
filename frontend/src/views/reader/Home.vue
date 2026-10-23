<script setup lang="ts">
// ============================================
// 读者门户首页
// 区块: hero(保留) + 公告轮播 + 分类快捷入口 + 热门推荐 + 编辑推荐
// 数据源: bookApi.categories / hotTop / recommend
// ============================================
defineOptions({ name: 'ReaderHome' })

import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import announcementApi from '@/api/modules/announcement'
import bookApi from '@/api/modules/book'
import type { Book } from '@/types/api'

const router = useRouter()

// ===== 公告轮播(后端公告数据, 管理端发布后实时可见) =====
interface AnnouncementItem {
  id: number
  title: string
  createTime?: string
  content?: string
}
const announcements = ref<AnnouncementItem[]>([])
const annLoading = ref(false)

async function loadAnnouncements() {
  annLoading.value = true
  try {
    const res = await announcementApi.clientList(5)
    announcements.value = (res.data ?? []).map((a) => ({
      id: a.id,
      title: a.title,
      createTime: a.createTime,
      content: a.content,
    }))
  } catch {
    announcements.value = []
  } finally {
    annLoading.value = false
  }
}

// ===== 分类快捷入口 =====
interface Category {
  id: number
  name: string
}
const categories = ref<Category[]>([])
const catLoading = ref(false)
const catError = ref(false)

async function loadCategories() {
  catLoading.value = true
  catError.value = false
  try {
    const res = await bookApi.categories({ silent: true })
    categories.value = res.data ?? []
  } catch {
    categories.value = []
    catError.value = true
  } finally {
    catLoading.value = false
  }
}

function goCategory(cat: Category) {
  void router.push({ path: '/book/search', query: { categoryId: cat.id } })
}

// ===== 热门推荐 =====
const hotBooks = ref<Book[]>([])
const hotLoading = ref(false)

async function loadHot() {
  hotLoading.value = true
  try {
    const res = await bookApi.hotTop(6)
    hotBooks.value = res.data ?? []
  } catch {
    hotBooks.value = []
  } finally {
    hotLoading.value = false
  }
}

// ===== 编辑推荐 =====
const recBooks = ref<Book[]>([])
const recLoading = ref(false)

async function loadRecommend() {
  recLoading.value = true
  try {
    const res = await bookApi.recommend(1)
    recBooks.value = res.data ?? []
  } catch {
    recBooks.value = []
  } finally {
    recLoading.value = false
  }
}

onMounted(() => {
  void loadAnnouncements()
  void loadCategories()
  void loadHot()
  void loadRecommend()
})

// 无封面时取书名首字占位
function coverInitial(book: Book): string {
  return book.title ? book.title.charAt(0) : '?'
}
</script>

<template>
  <div class="reader-home">
    <!-- hero 区(保留现有深蓝渐变) -->
    <section class="hero">
      <div class="hero-text">
        <h1>智阅 · 你的私人阅读伙伴</h1>
        <p>从馆藏 12,000+ 册中, 找到下一本让你心动的好书。</p>
      </div>
    </section>

    <!-- 公告轮播(后端公告) -->
    <section class="section">
      <h2 class="section-title">公告轮播</h2>
      <div v-loading="annLoading" class="card notice-card">
        <el-carousel v-if="announcements.length" direction="vertical" height="56px" :interval="4000">
          <el-carousel-item v-for="a in announcements" :key="a.id">
            <div class="notice-slide">
              <span class="notice-dot"></span>
              <span class="notice-text">{{ a.title }}</span>
              <span class="notice-date">{{ (a.createTime || '').slice(0, 10) }}</span>
            </div>
          </el-carousel-item>
        </el-carousel>
        <div v-else class="notice-empty">暂无公告</div>
      </div>
    </section>

    <!-- 分类快捷入口(动态) -->
    <section class="section">
      <h2 class="section-title">分类快捷入口</h2>
      <div v-loading="catLoading" class="category-grid">
        <div v-for="c in categories" :key="c.id" class="cat-item" @click="goCategory(c)">
          {{ c.name }}
        </div>
      </div>
      <div v-if="catError" class="card error-card">
        <el-empty description="分类加载失败，请稍后重试" :image-size="70" />
      </div>
    </section>

    <!-- 热门推荐 -->
    <section class="section">
      <h2 class="section-title">热门推荐</h2>
      <div v-loading="hotLoading" class="book-grid">
        <div v-for="b in hotBooks" :key="b.id" class="book-card">
          <div class="cover">
            <el-image v-if="b.cover" :src="b.cover" fit="cover" class="cover-img" />
            <div v-else class="cover-photo">{{ coverInitial(b) }}</div>
          </div>
          <div class="info">
            <div class="title" :title="b.title">{{ b.title }}</div>
            <div class="meta">{{ b.author }}</div>
            <div class="meta-row">
              <span class="cat">{{ b.categoryName || '未分类' }}</span>
              <span v-if="b.rating != null" class="rating">★ {{ b.rating.toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!hotLoading && hotBooks.length === 0" class="card error-card">
        <el-empty description="暂无热门图书" :image-size="70" />
      </div>
    </section>

    <!-- 编辑推荐 -->
    <section class="section">
      <h2 class="section-title">编辑推荐</h2>
      <div v-loading="recLoading" class="book-grid">
        <div v-for="b in recBooks" :key="b.id" class="book-card">
          <div class="cover">
            <el-image v-if="b.cover" :src="b.cover" fit="cover" class="cover-img" />
            <div v-else class="cover-photo">{{ coverInitial(b) }}</div>
          </div>
          <div class="info">
            <div class="title" :title="b.title">{{ b.title }}</div>
            <div class="meta">{{ b.author }}</div>
            <div class="meta-row">
              <span class="cat">{{ b.categoryName || '未分类' }}</span>
              <span v-if="b.rating != null" class="rating">★ {{ b.rating.toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!recLoading && recBooks.length === 0" class="card error-card">
        <el-empty description="暂无编辑推荐" :image-size="70" />
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.reader-home {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.hero {
  border-radius: $radius-lg;
  padding: 48px 40px;
  background:
    radial-gradient(circle at 80% 30%, rgba(212, 164, 39, 0.15) 0%, transparent 60%),
    linear-gradient(135deg, #0f172a 0%, #1e3a8a 60%, #1d4ed8 100%);
  color: #fff;
  overflow: hidden;
  position: relative;
}

.hero-text {
  position: relative;
  z-index: 2;
  max-width: 540px;

  h1 {
    font-size: 32px;
    font-weight: 700;
    margin: 0 0 12px;
    letter-spacing: 0.5px;
  }
  p {
    color: rgba(255, 255, 255, 0.75);
    font-size: 15px;
    line-height: 1.6;
    margin: 0;
  }
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.card {
  background: var(--bg-card);
  border-radius: $radius-lg;
  box-shadow: var(--shadow-card);
  padding: 24px;
}

/* 公告轮播 */
.notice-card {
  padding: 8px 24px;
}

.notice-slide {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
}

.notice-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  font-size: 13px;
  color: var(--text-placeholder);
}

.notice-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: var(--brand-primary);
}

.notice-text {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: var(--text-regular);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-date {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-placeholder);
}

/* 分类快捷入口 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 80px;
}

.cat-item {
  background: var(--bg-gradient-card);
  border: 1px solid var(--border-soft);
  border-radius: $radius-md;
  padding: 26px 12px;
  text-align: center;
  font-weight: 600;
  color: var(--brand-link);
  cursor: pointer;
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    background 0.25s ease,
    color 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    background: linear-gradient(135deg, var(--brand-primary-dark), var(--brand-primary));
    color: #fff;
    box-shadow: var(--shadow-hover);
  }
}

/* 图书卡片网格 */
.book-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  min-height: 220px;
}

.book-card {
  background: var(--bg-card);
  border-radius: $radius-md;
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-hover);
  }
}

.cover {
  width: 100%;
  aspect-ratio: 3 / 4;
  background: var(--bg-hover);
}

.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-photo {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-primary-dark), var(--brand-primary));
}

.info {
  padding: 12px 14px 14px;
}

.title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.cat {
  font-size: 12px;
  color: var(--text-placeholder);
}

.rating {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-warning);
}

.error-card {
  padding: 12px;
}

@media (max-width: 768px) {
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .book-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

<script setup lang="ts">
// ============================================
// 数据看板 - 统计卡片 + 借阅趋势 + 分类占比 + 热门借阅 TOP10
// 数据源: src/api/modules/system.ts
// ============================================
defineOptions({ name: 'Dashboard' })

import { computed, onMounted, ref, watch } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import systemApi, {
  type BorrowTrendItem,
  type CategoryStatItem,
  type DashboardStats,
} from '@/api/modules/system'
import BaseChart from '@/components/BaseChart.vue'
import { useChartPalette } from '@/utils/chartTheme'

// ===== 状态 =====
const loading = ref(false)
const failedSections = ref<string[]>([])
const updatedAt = ref('')

const stats = ref<DashboardStats | null>(null)
const trend = ref<BorrowTrendItem[]>([])
const categoryStats = ref<CategoryStatItem[]>([])
const hotBooks = ref<{ title: string; count: number }[]>([])
const trendDays = ref(30)

const hasError = computed(() => failedSections.value.length > 0)
const trendEmpty = computed(() => !loading.value && trend.value.length === 0)
const categoryEmpty = computed(() => !loading.value && categoryStats.value.length === 0)

// ===== 数据加载 =====
// 统一使用 silent: 由页面自身展示错误态, 避免多请求同时失败时弹出多条重复提示
async function loadAll() {
  loading.value = true
  failedSections.value = []
  const results = await Promise.allSettled([
    systemApi.stats({ silent: true }),
    systemApi.borrowTrend(trendDays.value, { silent: true }),
    systemApi.categoryStats({ silent: true }),
    systemApi.hotBooks(10, { silent: true }),
  ])

  const [statsRes, trendRes, categoryRes, hotRes] = results

  if (statsRes.status === 'fulfilled') {
    stats.value = statsRes.value.data
  } else {
    stats.value = null
    failedSections.value.push('统计概览')
  }

  if (trendRes.status === 'fulfilled') {
    trend.value = trendRes.value.data ?? []
  } else {
    trend.value = []
    failedSections.value.push('借阅趋势')
  }

  if (categoryRes.status === 'fulfilled') {
    categoryStats.value = categoryRes.value.data ?? []
  } else {
    categoryStats.value = []
    failedSections.value.push('分类占比')
  }

  if (hotRes.status === 'fulfilled') {
    hotBooks.value = hotRes.value.data ?? []
  } else {
    hotBooks.value = []
    failedSections.value.push('热门借阅')
  }

  if (!hasError.value) {
    updatedAt.value = formatNow()
  }
  loading.value = false
}

async function loadTrend() {
  loading.value = true
  try {
    const res = await systemApi.borrowTrend(trendDays.value, { silent: true })
    trend.value = res.data ?? []
    failedSections.value = failedSections.value.filter((s) => s !== '借阅趋势')
  } catch {
    trend.value = []
    if (!failedSections.value.includes('借阅趋势')) {
      failedSections.value.push('借阅趋势')
    }
  } finally {
    loading.value = false
  }
}

watch(trendDays, () => {
  void loadTrend()
})

onMounted(() => {
  void loadAll()
})

// ===== 统计卡片 =====
function formatNumber(value: number | undefined): string {
  if (value === undefined || value === null || Number.isNaN(value)) return '—'
  return value.toLocaleString('zh-CN')
}

// 热门借阅进度条: 以榜首借阅量为基准换算百分比
function hotPercent(count: number): number {
  const max = Math.max(...hotBooks.value.map((item) => item.count), 1)
  return Math.round((count / max) * 100)
}

// 亮/暗主题图表调色板 (随主题自动切换)
const palette = useChartPalette()

const statCards = computed(() => [
  {
    key: 'totalBooks',
    label: '馆藏总量',
    unit: '册',
    value: formatNumber(stats.value?.totalBooks),
    icon: 'Collection',
    color: palette.value.tokens.primary.fg,
    bg: palette.value.tokens.primary.bg,
    desc: '在架可借阅图书',
  },
  {
    key: 'todayBorrows',
    label: '今日借阅',
    unit: '次',
    value: formatNumber(stats.value?.todayBorrows),
    icon: 'Reading',
    color: palette.value.tokens.success.fg,
    bg: palette.value.tokens.success.bg,
    desc: '今日新增借出记录',
  },
  {
    key: 'totalReaders',
    label: '注册读者',
    unit: '人',
    value: formatNumber(stats.value?.totalReaders),
    icon: 'User',
    color: palette.value.tokens.warning.fg,
    bg: palette.value.tokens.warning.bg,
    desc: '已开通借阅权限',
  },
  {
    key: 'overdueBooks',
    label: '逾期未还',
    unit: '册',
    value: formatNumber(stats.value?.overdueBooks),
    icon: 'Warning',
    color: palette.value.tokens.danger.fg,
    bg: palette.value.tokens.danger.bg,
    desc: '需催还处理',
  },
])

// ===== 图表配置 =====
const trendOption = computed<EChartsCoreOption>(() => {
  const p = palette.value
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: p.tooltip.backgroundColor,
      borderColor: p.tooltip.borderColor,
      textStyle: { color: p.tooltip.textStyle.color },
    },
    grid: { left: 8, right: 18, top: 26, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: p.axisLine } },
      axisLabel: { color: p.textSecondary, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: '借阅量',
      nameTextStyle: { color: p.textPlaceholder, fontSize: 11 },
      splitLine: { lineStyle: { type: 'dashed', color: p.splitLine } },
      axisLabel: { color: p.textSecondary, fontSize: 11 },
    },
    series: [
      {
        name: '借阅量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: trend.value.length <= 14,
        data: trend.value.map((item) => item.count),
        itemStyle: { color: p.seriesColor },
        lineStyle: { width: 3, color: p.seriesColor },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: p.areaFrom },
              { offset: 1, color: p.areaTo },
            ],
          },
        },
      },
    ],
  }
})

const categoryOption = computed<EChartsCoreOption>(() => {
  const p = palette.value
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 册 ({d}%)',
      backgroundColor: p.tooltip.backgroundColor,
      borderColor: p.tooltip.borderColor,
      textStyle: { color: p.tooltip.textStyle.color },
    },
    legend: {
      bottom: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: p.textSecondary, fontSize: 12 },
    },
    series: [
      {
        name: '分类占比',
        type: 'pie',
        radius: ['45%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: p.background, borderWidth: 2, borderRadius: 6 },
        label: { formatter: '{b}\n{d}%', color: p.textSecondary, fontSize: 11 },
        labelLine: { length: 8, length2: 8 },
        data: categoryStats.value.map((item, i) => ({
          ...item,
          itemStyle: { color: p.categoryColors[i % p.categoryColors.length] },
        })),
      },
    ],
  }
})

// ===== 工具函数 =====
const hotBarColor = computed(() => palette.value.seriesColor)

function formatNow(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<template>
  <div class="page-container dashboard">
    <div class="page-head">
      <h2 class="page-title">数据看板</h2>
      <span v-if="updatedAt" class="page-sub">数据更新于 {{ updatedAt }}</span>
    </div>

    <!-- 统一错误提示 -->
    <el-alert
      v-if="hasError"
      class="err-alert"
      type="warning"
      :closable="false"
      show-icon
      title="部分数据加载失败"
    >
      <template #default>
        <div class="err-body">
          <span>{{ failedSections.join('、') }} 加载失败，请检查后端服务是否已启动。</span>
          <el-button type="primary" size="small" :loading="loading" @click="loadAll">
            重新加载
          </el-button>
        </div>
      </template>
    </el-alert>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="item in statCards" :key="item.key" v-loading="loading" class="card stat-card">
        <div class="stat-icon" :style="{ background: item.bg, color: item.color }">
          <el-icon :size="22">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">
            {{ item.value }}
            <span class="stat-unit">{{ item.unit }}</span>
          </div>
          <div class="stat-desc">{{ item.desc }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <div v-loading="loading" class="card chart-card">
        <div class="card-head">
          <div>
            <span class="card-title">借阅趋势</span>
            <span class="card-sub">按日统计的借出册次</span>
          </div>
          <el-radio-group v-model="trendDays" size="small">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
        <el-empty v-if="trendEmpty" description="暂无借阅趋势数据" :image-size="80" />
        <BaseChart v-else :option="trendOption" height="300px" />
      </div>

      <div v-loading="loading" class="card chart-card">
        <div class="card-head">
          <div>
            <span class="card-title">分类占比</span>
            <span class="card-sub">各分类馆藏册数分布</span>
          </div>
        </div>
        <el-empty v-if="categoryEmpty" description="暂无分类统计数据" :image-size="80" />
        <BaseChart v-else :option="categoryOption" height="300px" />
      </div>
    </div>

    <!-- 热门借阅 TOP10 -->
    <div v-loading="loading" class="card hot-card">
      <div class="card-head">
        <div>
          <span class="card-title">热门借阅 TOP10</span>
          <span class="card-sub">按累计借阅次数排序</span>
        </div>
      </div>
      <el-table :data="hotBooks" style="width: 100%" empty-text="暂无热门借阅数据">
        <el-table-column type="index" label="排名" width="80" align="center" />
        <el-table-column prop="title" label="书名" min-width="150" show-overflow-tooltip />
        <el-table-column label="借阅次数" min-width="150">
          <template #default="{ row }">
            <div class="hot-row">
              <el-progress
                :percentage="hotPercent(row.count)"
                :stroke-width="10"
                :show-text="false"
                :color="hotBarColor"
              />
              <span class="hot-count">{{ row.count }} 次</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-sub {
  font-size: 12px;
  color: var(--text-secondary);
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

// ===== 统计卡片 =====
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.stat-icon {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-body {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}

.stat-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-placeholder);
  margin-left: 2px;
}

.stat-desc {
  font-size: 12px;
  color: var(--text-placeholder);
}

// ===== 图表 =====
.chart-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.chart-card {
  min-height: 380px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-sub {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-left: 8px;
}

// ===== 热门借阅 =====
.hot-card {
  margin-top: 16px;
}

.hot-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hot-count {
  flex: 0 0 60px;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: right;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>

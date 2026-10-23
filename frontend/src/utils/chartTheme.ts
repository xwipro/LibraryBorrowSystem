// ============================================
// ECharts 主题配色 - 与亮/暗主题联动
// 作用: 统一提供坐标轴/网格线/文字/提示框等配色, 避免图表配置里硬编码亮色,
//       主题切换时 palette 变化 -> option 重新计算 -> 图表自动刷新。
// 用法: const palette = useChartPalette()  // ComputedRef<ChartPalette>
//       computed(() => ({ xAxis: { axisLine: { lineStyle: { color: palette.value.axisLine } } } }))
// ============================================
import { computed, type ComputedRef } from 'vue'
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/store'

/** 统计卡/色块场景使用的前景+半透明底色 */
export interface ChartToken {
  fg: string
  bg: string
}

export interface ChartPalette {
  /** 主文字(标题/图例) */
  textPrimary: string
  /** 次级文字(坐标轴标签) */
  textSecondary: string
  /** 弱化文字(轴名/单位) */
  textPlaceholder: string
  /** 坐标轴线 */
  axisLine: string
  /** 网格分隔线 */
  splitLine: string
  /** 图表所在卡片底色(饼图扇区描边需与之一致) */
  background: string
  /** 系列主色(折线/柱状) */
  seriesColor: string
  /** 面积渐变起止色 */
  areaFrom: string
  areaTo: string
  /** 分类色板(饼图/多系列) */
  categoryColors: string[]
  /** 提示框样式 */
  tooltip: {
    backgroundColor: string
    borderColor: string
    textStyle: { color: string }
  }
  /** 统计卡等色块场景 */
  tokens: {
    primary: ChartToken
    success: ChartToken
    warning: ChartToken
    danger: ChartToken
  }
}

const LIGHT: ChartPalette = {
  textPrimary: '#1f2937',
  textSecondary: '#6b7280',
  textPlaceholder: '#9ca3af',
  axisLine: '#e5e7eb',
  splitLine: '#f3f4f6',
  background: '#ffffff',
  seriesColor: '#2563eb',
  areaFrom: 'rgba(37, 99, 235, 0.28)',
  areaTo: 'rgba(37, 99, 235, 0.02)',
  categoryColors: ['#2563eb', '#16a34a', '#d4a427', '#dc2626', '#7c3aed', '#0891b2'],
  tooltip: {
    backgroundColor: 'rgba(255, 255, 255, 0.96)',
    borderColor: '#e5e7eb',
    textStyle: { color: '#1f2937' },
  },
  tokens: {
    primary: { fg: '#2563eb', bg: 'rgba(37, 99, 235, 0.12)' },
    success: { fg: '#16a34a', bg: 'rgba(22, 163, 74, 0.12)' },
    warning: { fg: '#d4a427', bg: 'rgba(212, 164, 39, 0.14)' },
    danger: { fg: '#dc2626', bg: 'rgba(220, 38, 38, 0.12)' },
  },
}

const DARK: ChartPalette = {
  textPrimary: '#f1f5f9',
  textSecondary: '#cbd5e1',
  textPlaceholder: '#94a3b8',
  axisLine: '#334155',
  splitLine: 'rgba(148, 163, 184, 0.22)',
  background: '#1e293b',
  seriesColor: '#3b82f6',
  areaFrom: 'rgba(59, 130, 246, 0.35)',
  areaTo: 'rgba(59, 130, 246, 0.02)',
  categoryColors: ['#3b82f6', '#22c55e', '#f5c542', '#f87171', '#a78bfa', '#22d3ee'],
  tooltip: {
    backgroundColor: 'rgba(30, 41, 59, 0.96)',
    borderColor: '#334155',
    textStyle: { color: '#f1f5f9' },
  },
  tokens: {
    primary: { fg: '#60a5fa', bg: 'rgba(96, 165, 250, 0.16)' },
    success: { fg: '#4ade80', bg: 'rgba(74, 222, 128, 0.16)' },
    warning: { fg: '#f5c542', bg: 'rgba(245, 197, 66, 0.16)' },
    danger: { fg: '#f87171', bg: 'rgba(248, 113, 113, 0.16)' },
  },
}

/** 非组件环境按明暗状态取色板 */
export function getChartPalette(isDark: boolean): ChartPalette {
  return isDark ? DARK : LIGHT
}

/** 组件内使用: 跟随 app store 的实际主题自动更新 */
export function useChartPalette(): ComputedRef<ChartPalette> {
  const appStore = useAppStore()
  const { theme } = storeToRefs(appStore)
  return computed(() => getChartPalette(theme.value === 'dark'))
}

export default useChartPalette

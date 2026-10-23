<script setup lang="ts">
// ============================================
// ECharts 通用图表组件 - 按需引入核心模块
// 用法: <BaseChart :option="lineOption" height="300px" />
// ============================================
import { onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, RadarChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  DataZoomComponent,
  ToolboxComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useAppStore } from '@/store'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  RadarChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  DataZoomComponent,
  ToolboxComponent,
  CanvasRenderer,
])

const props = withDefaults(
  defineProps<{
    option: echarts.EChartsCoreOption
    height?: string
    theme?: string
    autoResize?: boolean
  }>(),
  { height: '320px', theme: 'default', autoResize: true },
)

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 亮/暗主题: 跟随全局 themeMode(含"跟随系统")
const appStore = useAppStore()
const { theme: resolvedTheme } = storeToRefs(appStore)

function init() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value, props.theme, { renderer: 'canvas' })
  chartInstance.setOption(props.option)
}

function dispose() {
  chartInstance?.dispose()
  chartInstance = null
}

function resize() {
  chartInstance?.resize()
}

// 主题切换: 重建实例并重新应用当前 option, 避免残留上一主题的坐标轴/文字配色
function reinit() {
  dispose()
  init()
}

watch(
  () => props.option,
  (val) => chartInstance?.setOption(val, true),
  { deep: true },
)

watch(resolvedTheme, () => reinit())

onMounted(() => {
  init()
  if (props.autoResize) {
    window.addEventListener('resize', resize)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  dispose()
})

onActivated(() => {
  if (chartInstance) {
    resize()
  } else {
    init()
  }
})
onDeactivated(() => resize())

defineExpose({ getInstance: () => chartInstance, resize })
</script>

<template>
  <div ref="chartRef" class="base-chart" :style="{ height }"></div>
</template>

<style scoped>
.base-chart {
  width: 100%;
}
</style>

// ============================================
// 应用全局状态 - 侧边栏/主题/设备类型
// 主题支持: light / dark / system(跟随系统)
// ============================================
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

export const useAppStore = defineStore(
  'app',
  () => {
    const sidebarCollapsed = ref(false)
    const isMobile = ref(false)
    // 用户选择的主题模式
    const themeMode = ref<ThemeMode>('light')
    // 实际生效的主题(解析 system 后的结果)
    const theme = ref<'light' | 'dark'>('light')

    // 系统主题 media query
    const mql = window.matchMedia('(prefers-color-scheme: dark)')
    // 系统主题监听只注册一次(避免重复 initTheme 导致重复注册)
    let watchingSystemTheme = false

    /**
     * 按模式解析出实际生效的主题并同步到 <html>:
     * - 类名 html.dark: 供 Element Plus 官方暗色变量与自定义暗色样式使用
     * - 属性 data-theme: 供本项目统一样式钩子使用
     * - style.colorScheme: 让浏览器原生控件(滚动条/表单)跟随主题
     */
    function applyTheme(mode: ThemeMode = themeMode.value) {
      theme.value = mode === 'system' ? (mql.matches ? 'dark' : 'light') : mode
      const isDark = theme.value === 'dark'
      const root = document.documentElement
      root.classList.toggle('dark', isDark)
      root.setAttribute('data-theme', theme.value)
      root.style.colorScheme = theme.value
    }

    /** 系统主题变化: 仅"跟随系统"模式下实时响应, 其他模式忽略 */
    function handleSystemThemeChange() {
      if (themeMode.value === 'system') {
        applyTheme('system')
      }
    }

    function initTheme() {
      applyTheme(themeMode.value)
      if (!watchingSystemTheme) {
        mql.addEventListener('change', handleSystemThemeChange)
        watchingSystemTheme = true
      }
    }

    function setThemeMode(mode: ThemeMode) {
      if (themeMode.value === mode) {
        // 值未变化时 watch 不触发, 这里兜底同步一次
        applyTheme(mode)
      } else {
        // 变更交给下面的 watch 统一应用, 保证只应用一次
        themeMode.value = mode
      }
    }

    function toggleSidebar() {
      sidebarCollapsed.value = !sidebarCollapsed.value
    }

    function setMobile(val: boolean) {
      isMobile.value = val
    }

    // 持久化后初始化
    watch(
      () => themeMode.value,
      () => applyTheme(themeMode.value),
    )

    return {
      sidebarCollapsed,
      isMobile,
      themeMode,
      theme,
      initTheme,
      setThemeMode,
      toggleSidebar,
      setMobile,
    }
  },
  { persist: { pick: ['sidebarCollapsed', 'themeMode'] } },
)

export default useAppStore

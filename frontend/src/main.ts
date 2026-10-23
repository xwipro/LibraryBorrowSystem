// ============================================
// 应用入口
// ============================================
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// Element Plus 官方暗色变量: 变量定义在 html.dark 上, 只有切换到暗色时才生效,
// 内部动态 import 浅色变量, 静默加载失败也不影响使用
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { setupRouterGuard } from './permission'
import './styles/main.scss'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { size: 'default', zIndex: 3000, locale: zhCn })

// 全局注册 Element Plus 图标(用于 :is 动态渲染)
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp as never)
}

// 路由守卫
setupRouterGuard(router)

// 初始化主题(必须在 mount 前, 等 pinia 持久化恢复后再应用)
import { useAppStore } from '@/store'
const appStore = useAppStore()
appStore.initTheme()

// 全局错误处理 - 用 ElMessage 提示
app.config.errorHandler = (err, _instance, info) => {
  console.error('[App Error]', info, err)
  ElMessage.error(`应用异常: ${info}`)
}

// 全局 Promise 未捕获异常 → ElMessage
window.addEventListener('unhandledrejection', (e) => {
  const msg = (e.reason?.message ?? e.reason ?? '未知错误').toString()
  if (!msg.includes('silent') && !msg.includes('cancel')) {
    ElMessage.error(msg.length > 80 ? msg.slice(0, 80) + '...' : msg)
  }
})

app.mount('#app')

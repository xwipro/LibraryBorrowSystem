// ============================================
// 全局路由守卫 - 鉴权 + 进度条 + 标题
//
// ⚠️ 跳过鉴权由环境变量 VITE_DISABLE_AUTH 控制:
//    .env.development = true  (本地调试直接放行)
//    .env.production  = false (走完整鉴权流程)
// ============================================
import NProgress from 'nprogress'
import type { Router } from 'vue-router'
import { useUserStore } from '@/store'
import { getToken } from '@/utils/auth'

// 是否跳过鉴权 - 读取环境变量, 未配置时默认开启鉴权(安全兜底)
const DISABLE_AUTH = import.meta.env.VITE_DISABLE_AUTH === 'true'

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    NProgress.start()

    // 标题
    document.title = to.meta.title
      ? `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
      : import.meta.env.VITE_APP_TITLE

    // ===== 开发模式: 跳过鉴权, 直接放行 =====
    if (DISABLE_AUTH) {
      const userStore = useUserStore()
      if (!userStore.userInfo) {
        userStore.userInfo = {
          id: 1,
          username: 'admin',
          realName: '管理员',
          avatar: '',
          roles: ['admin'],
          permissions: ['*'],
        }
        userStore.roles = ['admin']
        userStore.permissions = ['*']
      }
      next()
      return
    }

    // ===== 正常鉴权逻辑(等后端就绪后把 DISABLE_AUTH 改回 false) =====
    const token = getToken()

    if (token) {
      if (to.path === '/login') {
        next({ path: '/' })
        NProgress.done()
        return
      }
      const userStore = useUserStore()
      if (!userStore.userInfo) {
        try {
          await userStore.fetchUserInfo()
          next()
        } catch {
          userStore.reset()
          next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
          NProgress.done()
        }
        return
      }
      // 角色校验
      if (to.meta.roles?.length) {
        const hasRole = to.meta.roles.some((r) => userStore.roles.includes(r))
        if (!hasRole) {
          next({ path: '/' })
          NProgress.done()
          return
        }
      }
      next()
      return
    }

    // 白名单
    if (['/login', '/404'].includes(to.path)) {
      next()
      return
    }
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    NProgress.done()
  })

  router.afterEach(() => {
    NProgress.done()
  })
}

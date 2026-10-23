<script setup lang="ts">
// ============================================
// 后台布局 - 左侧深色侧边栏 + 顶部操作栏
// 对应 PROJECT2026 UI: "左侧深色侧边导航"
// ============================================
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  useRoute,
  useRouter,
  type RouteLocationNormalizedLoaded,
  type RouteRecordRaw,
} from 'vue-router'
import { useAppStore, useUserStore } from '@/store'
import { routes } from '@/router'
import {
  getUnreadCount,
  NOTIFY_READ_EVENT,
} from '@/utils/notification'

const appStore = useAppStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

// 主题模式下拉
interface ThemeOption {
  value: 'light' | 'dark' | 'system'
  label: string
  icon: string
}
const themeOptions: ThemeOption[] = [
  { value: 'light', label: '亮色', icon: 'Sunny' },
  { value: 'dark', label: '暗色', icon: 'Moon' },
  { value: 'system', label: '跟随系统', icon: 'Monitor' },
]

function handleThemeCmd(cmd: string) {
  appStore.setThemeMode(cmd as 'light' | 'dark' | 'system')
}

const currentThemeLabel = computed(() => {
  const opt = themeOptions.find((o) => o.value === appStore.themeMode)
  return opt?.label ?? '亮色'
})

// 仅取根路径下的菜单
const adminRoot = computed(() => routes.find((r) => r.path === '/'))
const menuRoutes = computed<RouteRecordRaw[]>(
  () => adminRoot.value?.children?.filter((r) => !r.meta?.hidden) ?? [],
)

const activeMenu = computed(() => route.path)

// ============================================
// 通知角标: 实时未读数(与通知中心共用数据源), 已读变化/路由切换时同步刷新
// ============================================
const unreadBadge = ref(0)

async function refreshUnreadBadge() {
  unreadBadge.value = await getUnreadCount()
}

function onNotifyReadChanged() {
  void refreshUnreadBadge()
}

onMounted(() => {
  void refreshUnreadBadge()
  window.addEventListener(NOTIFY_READ_EVENT, onNotifyReadChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener(NOTIFY_READ_EVENT, onNotifyReadChanged)
})

// 路由切换(如从通知中心返回)后重新计算, 保证与最新已读状态一致
watch(
  () => route.path,
  () => void refreshUnreadBadge(),
)

// ============================================
// keep-alive 缓存策略
// 1) 是否缓存以路由 meta.keepAlive 为准, 不再用 fullPath 当 key (否则同一路由不同
//    查询参数会被当成不同组件反复重建, 与 keepAlive 语义冲突);
// 2) 缓存键使用路由 name, 并用 include 精确声明可缓存页面;
// 3) <keep-alive> 的 include 匹配的是"组件名", 因此各页面组件通过
//    defineOptions({ name }) 声明与路由 name 一致的名字。
// ============================================
function collectKeepAliveNames(list: readonly RouteRecordRaw[]): string[] {
  const names: string[] = []
  for (const r of list) {
    if (r.meta?.keepAlive && typeof r.name === 'string') {
      names.push(r.name)
    }
    if (r.children?.length) {
      names.push(...collectKeepAliveNames(r.children))
    }
  }
  return names
}

const cachedViews = computed<string[]>(() => collectKeepAliveNames(adminRoot.value?.children ?? []))

function routeKey(r: RouteLocationNormalizedLoaded): string {
  return typeof r.name === 'string' ? r.name : r.fullPath
}

function handleSelect(index: string) {
  router.push(index)
}

// 面包屑
const breadcrumbs = computed(() =>
  route.matched
    .filter((r) => r.meta?.title && !r.meta?.hidden)
    .map((r) => ({ title: r.meta!.title!, path: r.path })),
)

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/admin/profile')
  }
}
</script>

<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="brand">
        <div class="brand-mark">
          <svg viewBox="0 0 48 48" width="36" height="36" aria-hidden="true">
            <defs>
              <linearGradient id="brandGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#5990fc" />
                <stop offset="100%" stop-color="#3b82f6" />
              </linearGradient>
            </defs>
            <rect width="48" height="48" rx="12" fill="url(#brandGrad)" />
            <path d="M12 14h8a3 3 0 0 1 3 3v18a3 3 0 0 0-3-3h-8z" fill="#fff" />
            <path d="M36 14h-8a3 3 0 0 0-3 3v18a3 3 0 0 1 3-3h8z" fill="#fff" opacity="0.85" />
          </svg>
        </div>
        <transition name="fade-slide">
          <div v-show="!appStore.sidebarCollapsed" class="brand-text">
            <span class="brand-name">图书管理</span>
            <span class="brand-sub">ADMIN 2026</span>
          </div>
        </transition>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="rgba(255,255,255,0.75)"
          active-text-color="#ffffff"
          unique-opened
          @select="handleSelect"
        >
          <el-menu-item v-for="r in menuRoutes" :key="r.path" :index="`/${r.path}`">
            <el-icon v-if="r.meta?.icon">
              <component :is="r.meta.icon" />
            </el-icon>
            <template #title>{{ r.meta?.title }}</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-footer">
        <el-tooltip content="收起菜单" placement="right">
          <button class="collapse-btn" @click="appStore.toggleSidebar()">
            <el-icon>
              <Fold v-if="!appStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </button>
        </el-tooltip>
      </div>
    </aside>

    <!-- 主区 -->
    <div class="main">
      <header class="topbar">
        <el-breadcrumb class="breadcrumb" separator="/">
          <el-breadcrumb-item
            v-for="(c, i) in breadcrumbs"
            :key="i"
            :to="i < breadcrumbs.length - 1 ? c.path : undefined"
          >
            {{ c.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>

        <div class="topbar-actions">
          <!-- 主题切换下拉 -->
          <!-- 注意: el-tooltip 必须放在 el-dropdown 外层, 否则会吞掉 dropdown 的 click 触发事件, 导致主题菜单无法打开 -->
          <el-tooltip :content="`主题: ${currentThemeLabel}`" placement="bottom">
            <el-dropdown trigger="click" @command="handleThemeCmd" placement="bottom-end">
              <el-button circle text class="theme-btn">
                <el-icon>
                  <Sunny v-if="appStore.theme === 'light'" />
                  <Moon v-else />
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu class="theme-menu">
                  <el-dropdown-item
                    v-for="opt in themeOptions"
                    :key="opt.value"
                    :command="opt.value"
                    :class="{ 'is-active': appStore.themeMode === opt.value }"
                  >
                    <el-icon><component :is="opt.icon" /></el-icon>
                    <span>{{ opt.label }}</span>
                    <el-icon v-if="appStore.themeMode === opt.value" class="check">
                      <Check />
                    </el-icon>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-tooltip>

          <!-- 通知中心 -->
          <el-tooltip content="通知中心">
            <el-badge v-if="unreadBadge > 0" :value="unreadBadge" :max="99">
              <el-button circle text @click="router.push('/admin/notifications')">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
            <el-button v-else circle text @click="router.push('/admin/notifications')">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-trigger">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar" class="theme-avatar">
                {{ userStore.userInfo?.realName?.[0] ?? '管' }}
              </el-avatar>
              <span class="user-name">
                {{ userStore.userInfo?.realName ?? '管理员' }}
              </span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <RouterView v-slot="{ Component, route: r }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="routeKey(r)" />
            </keep-alive>
          </transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  background: var(--bg-page);
}

// ====== 侧边栏 ======
.sidebar {
  width: 232px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  display: flex;
  flex-direction: column;
  position: relative;
  transition: width 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow: $shadow-sidebar;
  z-index: 10;

  &.collapsed {
    width: 64px;
  }
}

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  .brand-mark {
    flex-shrink: 0;
    filter: drop-shadow(0 4px 12px rgba(59, 130, 246, 0.4));
  }
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 2px;
  margin-top: 3px;
}

.menu-scroll {
  flex: 1;
  margin: 12px 0;

  :deep(.el-menu) {
    border-right: none;
    padding: 0 12px;
  }

  :deep(.el-menu-item) {
    height: 44px;
    line-height: 44px;
    margin-bottom: 4px;
    border-radius: $radius-md;
    color: rgba(255, 255, 255, 0.75);
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.06);
      color: #fff;
    }
    &.is-active {
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.95), rgba(37, 99, 235, 0.85));
      color: #fff;
      box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
    }
  }
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.collapse-btn {
  width: 100%;
  height: 36px;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.04);
  border: none;
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: #fff;
  }
}

// ====== 主区 ======
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  height: 60px;
  background: var(--bg-topbar);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 0 var(--border-base);
  z-index: 5;
  transition: background 0.3s ease;
}

.breadcrumb {
  font-size: 14px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-btn {
  &:hover {
    background: var(--bg-hover) !important;
  }
}

.theme-menu {
  min-width: 160px;

  .el-dropdown-menu__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 16px;

    .check {
      margin-left: auto;
      color: var(--brand-primary);
    }

    &.is-active {
      color: var(--brand-primary);
      font-weight: 500;
    }
  }
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: $radius-full;
  transition: background 0.2s ease;

  &:hover {
    background: var(--bg-hover);
  }
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
</style>

<!-- 非 scoped: 暴力覆盖 Element Plus 折叠态的内部 padding/margin -->
<style lang="scss">
.admin-layout .sidebar .el-menu.el-menu--collapse {
  padding: 0 !important;
}

.admin-layout .sidebar .el-menu.el-menu--collapse .el-menu-item {
  padding: 0 !important;
  margin: 0 6px 4px !important;
}

// 折叠态 Element Plus 用 .el-menu-tooltip__trigger 包裹内容(不是 .el-menu-item__content!)
.admin-layout .sidebar .el-menu.el-menu--collapse .el-menu-item .el-menu-tooltip__trigger {
  padding: 0 !important;
  margin: 0 !important;
  width: 100% !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  text-align: center !important;
}

.admin-layout .sidebar .el-menu.el-menu--collapse .el-menu-item .el-icon {
  margin-right: 0 !important;
  margin-left: 0 !important;
  font-size: 18px;
}
</style>

<script setup lang="ts">
// ============================================
// 读者门户布局 - 顶部导航栏 + 公告轮播 + 内容区
// 对应 PROJECT2026 UI: "顶部导航栏(logo、导航菜单、搜索框、用户头像)"
// ============================================
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')

const navMenus = [
  { name: '首页', path: '/home' },
  { name: '图书检索', path: '/search' },
  { name: '我的借阅', path: '/borrow/records' },
  { name: '我的预约', path: '/reservation' },
  { name: '消息中心', path: '/notifications' },
]

const activePath = computed(() => {
  const matched = navMenus.find((m) => route.path.startsWith(m.path))
  return matched?.path ?? ''
})

function handleSearch() {
  if (!keyword.value.trim()) return
  router.push({ path: '/search', query: { keyword: keyword.value.trim() } })
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  }
}
</script>

<template>
  <div class="reader-layout">
    <!-- 顶部导航 -->
    <header class="topnav">
      <div class="nav-inner">
        <div class="brand" @click="router.push('/home')">
          <svg viewBox="0 0 48 48" width="34" height="34" aria-hidden="true">
            <defs>
              <linearGradient id="readerBrand" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#1e3a8a" />
                <stop offset="100%" stop-color="#3b82f6" />
              </linearGradient>
            </defs>
            <rect width="48" height="48" rx="12" fill="url(#readerBrand)" />
            <path d="M12 14h8a3 3 0 0 1 3 3v18a3 3 0 0 0-3-3h-8z" fill="#fff" />
            <path d="M36 14h-8a3 3 0 0 0-3 3v18a3 3 0 0 1 3-3h8z" fill="#fff" opacity="0.85" />
          </svg>
          <div class="brand-text">
            <span class="name">智阅图书馆</span>
            <span class="sub">Library 2026</span>
          </div>
        </div>

        <nav class="nav-menus">
          <RouterLink
            v-for="m in navMenus"
            :key="m.path"
            :to="m.path"
            class="nav-item"
            :class="{ active: activePath === m.path }"
          >
            {{ m.name }}
          </RouterLink>
        </nav>

        <div class="nav-actions">
          <el-input
            v-model="keyword"
            class="search-input"
            placeholder="搜索书名 / 作者 / ISBN"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-tooltip content="消息中心">
            <el-badge :value="3" :max="9">
              <el-button circle text @click="router.push('/notifications')">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-trigger">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar" class="theme-avatar">
                {{ userStore.userInfo?.realName?.[0] ?? '读' }}
              </el-avatar>
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
      </div>
    </header>

    <!-- 内容区 -->
    <main class="reader-main">
      <RouterView v-slot="{ Component, route: r }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" :key="r.fullPath" />
        </transition>
      </RouterView>
    </main>

    <!-- 页脚 -->
    <footer class="reader-footer">
      <span>© 2026 智阅图书馆 · 知识流转于指尖</span>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.reader-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-gradient-page);
  transition: background 0.28s ease;
}

// ====== 顶部导航 ======
.topnav {
  position: sticky;
  top: 0;
  z-index: 50;
  background: var(--bg-nav);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-soft);
  box-shadow: 0 1px 0 var(--border-soft);
}

.nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 68px;
  display: flex;
  align-items: center;
  gap: 36px;
  padding: 0 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;

  .name {
    font-size: 17px;
    font-weight: 700;
    background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    letter-spacing: 0.5px;
  }
  .sub {
    font-size: 10px;
    color: var(--text-secondary);
    letter-spacing: 2px;
    margin-top: 2px;
  }
}

.nav-menus {
  display: flex;
  gap: 4px;
  flex: 1;
}

.nav-item {
  position: relative;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--text-regular);
  text-decoration: none;
  font-weight: 500;
  border-radius: $radius-md;
  transition: all 0.22s ease;

  &::after {
    content: '';
    position: absolute;
    bottom: 6px;
    left: 50%;
    transform: translateX(-50%) scaleX(0);
    width: 22px;
    height: 2px;
    border-radius: 2px;
    background: linear-gradient(90deg, #1e3a8a, #3b82f6);
    transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  }

  &:hover {
    color: var(--brand-primary);
    background: rgba(59, 130, 246, 0.06);
  }
  &.active {
    color: var(--brand-primary);
    &::after {
      transform: translateX(-50%) scaleX(1);
    }
  }
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 240px;
  :deep(.el-input__wrapper) {
    border-radius: $radius-full;
    background: rgba(15, 23, 42, 0.04);
    box-shadow: none;
  }
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: $radius-full;
  transition: background 0.2s ease;

  &:hover {
    background: rgba(15, 23, 42, 0.04);
  }
}

// ====== 主区 ======
.reader-main {
  flex: 1;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
}

.reader-footer {
  text-align: center;
  padding: 16px;
  font-size: 12px;
  color: var(--text-secondary);
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

@media (max-width: 768px) {
  .nav-menus {
    display: none;
  }
  .search-input {
    width: 140px;
  }
}
</style>

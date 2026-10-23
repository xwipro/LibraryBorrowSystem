// ============================================
// 路由配置 - 仅后台管理端
// ============================================
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    hidden?: boolean
    requiresAuth?: boolean
    roles?: string[]
    keepAlive?: boolean
  }
}

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
  },

  // ===================== 后台管理端 =====================
  {
    path: '/',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, title: '后台管理' },
    children: [
      {
        path: 'admin/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataLine', requiresAuth: true, keepAlive: true },
      },
      {
        path: 'admin/book/list',
        name: 'BookList',
        component: () => import('@/views/book/List.vue'),
        meta: { title: '图书管理', icon: 'Reading', requiresAuth: true, keepAlive: true },
      },
      {
        path: 'admin/book/detail/:id',
        name: 'BookDetailAdmin',
        component: () => import('@/views/book/Detail.vue'),
        meta: { title: '图书详情', hidden: true, requiresAuth: true },
      },
      {
        path: 'admin/borrow',
        name: 'BorrowManage',
        component: () => import('@/views/borrow/index.vue'),
        meta: { title: '借阅管理', icon: 'Document', requiresAuth: true, keepAlive: true },
      },
      {
        path: 'admin/reader',
        name: 'ReaderManage',
        component: () => import('@/views/reader/Manage.vue'),
        meta: { title: '读者管理', icon: 'User', requiresAuth: true, keepAlive: true },
      },
      {
        path: 'admin/reservation',
        name: 'ReservationManage',
        component: () => import('@/views/reservation/index.vue'),
        meta: { title: '预约管理', icon: 'Calendar', requiresAuth: true, keepAlive: true },
      },
      {
        path: 'admin/system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/User.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', requiresAuth: true, roles: ['admin'] },
      },
      {
        path: 'admin/system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/Role.vue'),
        meta: { title: '角色权限', icon: 'Lock', requiresAuth: true, roles: ['admin'] },
      },
      {
        path: 'admin/system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/Log.vue'),
        meta: { title: '操作日志', icon: 'Tickets', requiresAuth: true, roles: ['admin'] },
      },
      // 以下为顶栏入口页(个人中心/通知中心), 不出现在侧边菜单
      {
        path: 'admin/profile',
        name: 'AdminProfile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', hidden: true, requiresAuth: true },
      },
      {
        path: 'admin/notifications',
        name: 'AdminNotifications',
        component: () => import('@/views/notification/index.vue'),
        meta: { title: '通知中心', hidden: true, requiresAuth: true },
      },
    ],
  },

  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router

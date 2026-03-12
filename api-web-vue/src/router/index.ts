import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
    {
    path: '/1',
    name: '1',
    component: () => import('@/views/admin/1.vue'),
    meta: { title: '支付' },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/',
    redirect: '/user/home',
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'apps',
        name: 'AdminApps',
        component: () => import('@/views/admin/Apps.vue'),
        meta: { title: '应用管理' },
      },
      {
        path: 'interfaces',
        name: 'AdminInterfaces',
        component: () => import('@/views/admin/Interfaces.vue'),
        meta: { title: '接口管理' },
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/Products.vue'),
        meta: { title: '产品套餐' },
      },
    ],
  },
  {
    path: '/user',
    component: () => import('@/layout/UserLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'UserHome',
        component: () => import('@/views/user/Home.vue'),
        meta: { title: '主页' },
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人信息' },
      },
      {
        path: 'my-apps',
        name: 'UserApps',
        component: () => import('@/views/user/MyApps.vue'),
        meta: { title: '我的应用' },
      },
      {
        path: 'api-market',
        name: 'ApiMarket',
        component: () => import('@/views/user/ApiMarket.vue'),
        meta: { title: 'API市场' },
      },
      {
        path: 'my-interfaces',
        name: 'MyInterfaces',
        component: () => import('@/views/user/MyInterfaces.vue'),
        meta: { title: '我的接口' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from) => {
  const userStore = useUserStore()
  
  // 设置标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - API 平台`
  }

  // 需要登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Login' }
  }

  // 管理员权限检查
  if (to.meta.role === 'ADMIN' && !userStore.isAdmin) {
    return { name: 'UserHome' }
  }

  // 已登录跳转到主页
  if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
    if (userStore.isAdmin) {
      return { name: 'AdminDashboard' }
    } else {
      return { name: 'UserHome' }
    }
  }
})

export default router

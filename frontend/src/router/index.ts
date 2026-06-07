import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import MeView from '@/views/MeView.vue'
import DashboardView from '@/views/DashboardView.vue'
import SchoolView from '@/views/SchoolView.vue'
import UserListView from '@/views/UserListView.vue'
import SettingsView from '@/views/SettingsView.vue'
import AuditView from '@/views/AuditView.vue'
import { getStoredAuth } from '@/api/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/me', name: 'me', component: MeView },
  { path: '/dashboard', name: 'dashboard', component: DashboardView },
  { path: '/school', name: 'school', component: SchoolView },
  { path: '/users', name: 'users', component: UserListView },
  { path: '/settings', name: 'settings', component: SettingsView },
  { path: '/audit', name: 'audit', component: AuditView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const auth = getStoredAuth()
  if (!auth?.accessToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router

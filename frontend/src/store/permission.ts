/**
 * RBAC 前端菜单 hook。
 *
 * 后端是权限边界；前端仅做体验优化。
 * 基线：权限判定矩阵 v1.0 §1 第 6 条。
 */

import { computed } from 'vue'
import { authStore } from '@/api/request'

export interface MenuItem {
  key: string
  title: string
  to: string
  requiredRoles?: string[]
}

const ALL_MENUS: MenuItem[] = [
  { key: 'dashboard', title: '看板', to: '/dashboard' },
  { key: 'school',    title: '学校空间', to: '/school',
    requiredRoles: ['SCHOOL_ADMIN', 'SYSTEM_ADMIN'] },
  { key: 'users',     title: '用户与角色', to: '/users',
    requiredRoles: ['SCHOOL_ADMIN', 'SYSTEM_ADMIN'] },
  { key: 'settings',  title: '配置中心', to: '/settings',
    requiredRoles: ['SCHOOL_ADMIN', 'SYSTEM_ADMIN'] },
  { key: 'audit',     title: '审计日志', to: '/audit',
    requiredRoles: ['SCHOOL_ADMIN', 'SYSTEM_ADMIN'] },
  { key: 'me',        title: '当前用户', to: '/me' }
]

export function usePermission() {
  const roles = computed<string[]>(() => authStore.get()?.roles || [])

  const visibleMenus = computed<MenuItem[]>(() => {
    return ALL_MENUS.filter((m) => {
      if (!m.requiredRoles || m.requiredRoles.length === 0) return true
      return m.requiredRoles.some((r) => roles.value.includes(r))
    })
  })

  function canSee(menuKey: string): boolean {
    const m = ALL_MENUS.find((x) => x.key === menuKey)
    if (!m) return false
    if (!m.requiredRoles || m.requiredRoles.length === 0) return true
    return m.requiredRoles.some((r) => roles.value.includes(r))
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  return { visibleMenus, canSee, hasRole, roles }
}

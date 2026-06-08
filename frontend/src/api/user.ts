import { wendaRequest } from './request'
import type { PageResp } from './school'

export interface UserMgmt {
  id: string
  schoolId: string
  username: string
  displayName: string
  email?: string
  phone?: string
  status: string
  userType: string
  version: number
}

export interface Role {
  code: string
  name: string
  description?: string
  isSystem: boolean
}

export const userApi = {
  list: (params: { keyword?: string; status?: string; page?: number; pageSize?: number } = {}) =>
    wendaRequest<PageResp<UserMgmt>>({ method: 'GET', url: '/users', params }),
  get: (userId: string) => wendaRequest<UserMgmt>({ method: 'GET', url: `/users/${userId}` }),
  create: (body: { username: string; displayName: string; email?: string; phone?: string;
                    avatarUrl?: string; userType?: string; initialPassword?: string }) =>
    wendaRequest<UserMgmt>({ method: 'POST', url: '/users', data: body }),
  update: (userId: string, body: Partial<UserMgmt>, ifMatch: number) =>
    wendaRequest<UserMgmt>({
      method: 'PATCH', url: `/users/${userId}`, data: body,
      headers: { 'If-Match': String(ifMatch) }
    }),
  bindRoles: (userId: string, roles: Array<{ roleCode: string; collegeId?: string; isPrimary?: boolean }>) =>
    wendaRequest<void>({ method: 'PUT', url: `/users/${userId}/roles-scopes`, data: { roles } }),
  disable: (userId: string) => wendaRequest<void>({ method: 'POST', url: `/users/${userId}/disable` }),
  resetPassword: (userId: string, newPassword: string) =>
    wendaRequest<void>({
      method: 'POST', url: `/users/${userId}/password-reset`, data: { newPassword }
    })
}

export const roleApi = {
  list: () => wendaRequest<Role[]>({ method: 'GET', url: '/roles' })
}

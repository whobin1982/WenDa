import { wendaRequest, authStore, type AuthBundle } from './request'

export interface LoginRequest {
  schoolCode: string
  username: string
  password: string
}

export interface AuthUser {
  userId: string
  username: string
  displayName: string
  email?: string
  schoolId: string
  tenantId: string
  roles: string[]
  status: string
  userType: string
}

export async function login(req: LoginRequest): Promise<AuthBundle> {
  const data = await wendaRequest<{
    userId: string
    username: string
    accessToken: string
    refreshToken: string
    schoolId: string
    roles: string[]
  }>({
    method: 'POST',
    url: '/auth/login',
    data: req
  })
  const bundle: AuthBundle = {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    userId: data.userId,
    username: data.username,
    schoolId: data.schoolId,
    roles: data.roles || []
  }
  authStore.set(bundle)
  return bundle
}

export async function logout(): Promise<void> {
  const auth = authStore.get()
  try {
    await wendaRequest<void>({
      method: 'POST',
      url: '/auth/logout',
      data: { refreshToken: auth?.refreshToken }
    })
  } finally {
    authStore.clear()
  }
}

export async function fetchMe(): Promise<AuthUser> {
  return wendaRequest<AuthUser>({ method: 'GET', url: '/auth/me' })
}

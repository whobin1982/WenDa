/**
 * 统一 request 封装（基线：接口文档 v0.2 §2.2 / §2.3 / §2.5）。
 *
 * 关键点：
 * 1. 注入 X-Request-Id；
 * 2. POST 创建 / 任务 / 报告 / 授权类接口自动注入 Idempotency-Key（持久到 sessionStorage）；
 * 3. 401 跳登录；403 / SCOPE_FORBIDDEN 提示；网络错误返回 INTERNAL_ERROR 提示；
 * 4. 错误码消费 `api/errorCodes.ts`。
 */

import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { ApiResponse, ErrorCode, isAuthError, isForbidden } from './errorCodes'

const STORAGE_AUTH_KEY = 'wenda.auth'
const STORAGE_IDEMPOTENCY_PREFIX = 'wenda.idempotency.'

export interface AuthBundle {
  accessToken: string
  refreshToken?: string
  userId: string
  username: string
  schoolId: string
  roles: string[]
}

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000
})

function genId(): string {
  return 'req_' + Math.random().toString(36).slice(2, 12) + Date.now().toString(36)
}

function newIdempotencyKey(): string {
  // 32-byte URL-safe
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}

function getStoredAuth(): AuthBundle | null {
  const raw = sessionStorage.getItem(STORAGE_AUTH_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthBundle
  } catch {
    return null
  }
}

function setStoredAuth(b: AuthBundle | null): void {
  if (b) sessionStorage.setItem(STORAGE_AUTH_KEY, JSON.stringify(b))
  else sessionStorage.removeItem(STORAGE_AUTH_KEY)
}

function getIdempotencyKey(method: string, url: string): string {
  const key = method.toUpperCase() + ' ' + url
  let v = sessionStorage.getItem(STORAGE_IDEMPOTENCY_PREFIX + key)
  if (!v) {
    v = newIdempotencyKey()
    sessionStorage.setItem(STORAGE_IDEMPOTENCY_PREFIX + key, v)
  }
  return v
}

function clearIdempotency(method: string, url: string): void {
  sessionStorage.removeItem(STORAGE_IDEMPOTENCY_PREFIX + method.toUpperCase() + ' ' + url)
}

instance.interceptors.request.use((config) => {
  config.headers.set('X-Request-Id', genId())
  const auth = getStoredAuth()
  if (auth?.accessToken) {
    config.headers.set('Authorization', 'Bearer ' + auth.accessToken)
  }
  const m = (config.method || 'get').toLowerCase()
  if (m === 'post' || m === 'put') {
    if (config.url && !config.headers.get('Idempotency-Key')) {
      config.headers.set('Idempotency-Key', getIdempotencyKey(m, config.url))
    }
  }
  return config
})

instance.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResponse<unknown>
    // 成功后清理该 URL 的幂等键，便于用户重试时拿到新键
    if (body && body.success && resp.config.method && resp.config.url) {
      clearIdempotency(resp.config.method, resp.config.url)
    }
    return resp
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const body = error.response?.data
    const code = body?.code
    if (isAuthError(code)) {
      setStoredAuth(null)
      ElMessage.warning('登录已过期，请重新登录。')
      // 由 router 在下次跳转时拉回登录页
    } else if (isForbidden(code)) {
      ElMessage.error(body?.message || '无权限访问。')
    } else if (code === ErrorCode.VALIDATION_ERROR) {
      const field = body.details?.[0]?.field
      ElMessage.error((field ? `${field}: ` : '') + (body?.message || '参数校验失败。'))
    } else {
      ElMessage.error(body?.message || '服务异常，请稍后重试。')
    }
    return Promise.reject(error)
  }
)

export interface WendaRequestOptions extends AxiosRequestConfig {
  /** 显式禁用 Idempotency-Key（仅调试用）。 */
  skipIdempotency?: boolean
}

export async function wendaRequest<T>(config: WendaRequestOptions): Promise<T> {
  const resp = await instance.request<ApiResponse<T>>(config)
  const body = resp.data
  if (!body.success) {
    throw new Error(body.message || 'API 返回失败')
  }
  return body.data
}

export const authStore = {
  get: getStoredAuth,
  set: setStoredAuth,
  clear: () => setStoredAuth(null)
}

export { getStoredAuth }

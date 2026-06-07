import { describe, it, expect } from 'vitest'
import { ErrorCode, isAuthError, isForbidden } from '@/api/errorCodes'

describe('errorCodes dictionary', () => {
  it('contains the required baseline codes', () => {
    expect(ErrorCode.OK).toBe('OK')
    expect(ErrorCode.FORBIDDEN).toBe('FORBIDDEN')
    expect(ErrorCode.VERSION_CONFLICT).toBe('VERSION_CONFLICT')
    expect(ErrorCode.IDEMPOTENCY_CONFLICT).toBe('IDEMPOTENCY_CONFLICT')
    expect(ErrorCode.AI_PROVIDER_DISABLED).toBe('AI_PROVIDER_DISABLED')
    expect(ErrorCode.DEPENDENCY_UNAVAILABLE).toBe('DEPENDENCY_UNAVAILABLE')
  })

  it('isAuthError returns true for UNAUTHORIZED and TOKEN_EXPIRED', () => {
    expect(isAuthError(ErrorCode.UNAUTHORIZED)).toBe(true)
    expect(isAuthError(ErrorCode.TOKEN_EXPIRED)).toBe(true)
    expect(isAuthError(ErrorCode.FORBIDDEN)).toBe(false)
    expect(isAuthError(undefined)).toBe(false)
  })

  it('isForbidden returns true for FORBIDDEN, ACCESS_DENIED, SCOPE_FORBIDDEN', () => {
    expect(isForbidden(ErrorCode.FORBIDDEN)).toBe(true)
    expect(isForbidden(ErrorCode.ACCESS_DENIED)).toBe(true)
    expect(isForbidden(ErrorCode.SCOPE_FORBIDDEN)).toBe(true)
    expect(isForbidden(ErrorCode.OK)).toBe(false)
  })
})

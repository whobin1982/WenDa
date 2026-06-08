package com.wenda.audit;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Audit 状态码映射测试（基线：GOV-002 修复 #7）。
 *
 * <p>通过反射调用 {@link AuditAspect} 私有 {@code mapExceptionStatus}，
 * 断言 BusinessException 的 status 取自 errorCode.httpStatus()，不再一律 422。
 *
 * <p>不做完整 Spring Boot IT 启动（避免与 AuditAspect 内部 AOP 编织冲突）；
 * 本测试只验证 Aspect 内部的状态码映射逻辑。
 */
class AuditStatusCodeMappingTest {

    @Test
    void businessExceptionUsesErrorCodeHttpStatus() throws Exception {
        // 反射调私有 static mapExceptionStatus
        java.lang.reflect.Method m = AuditAspect.class.getDeclaredMethod(
                "mapExceptionStatus", Throwable.class);
        m.setAccessible(true);

        assertEquals(403, (int) m.invoke(null, (Throwable) new BusinessException(ErrorCode.FORBIDDEN, "f")));
        assertEquals(401, (int) m.invoke(null, (Throwable) new BusinessException(ErrorCode.UNAUTHORIZED, "u")));
        assertEquals(404, (int) m.invoke(null, (Throwable) new BusinessException(ErrorCode.NOT_FOUND, "n")));
        assertEquals(409, (int) m.invoke(null, (Throwable) new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "i")));
        assertEquals(400, (int) m.invoke(null, (Throwable) new BusinessException(ErrorCode.BAD_REQUEST, "b")));
        assertEquals(422, (int) m.invoke(null, (Throwable) new BusinessException(ErrorCode.BUSINESS_STATE_INVALID, "s")));
        assertEquals(500, (int) m.invoke(null, (Throwable) new BusinessException(ErrorCode.INTERNAL_ERROR, "e")));
    }

    @Test
    void nonBusinessExceptionFallsBack() throws Exception {
        java.lang.reflect.Method m = AuditAspect.class.getDeclaredMethod(
                "mapExceptionStatus", Throwable.class);
        m.setAccessible(true);

        assertEquals(403, (int) m.invoke(null, new org.springframework.security.access.AccessDeniedException("a")));
        // Spring Security 提供的具体 AuthenticationException 子类（直接 new，避免匿名类
        // 造成 getSimpleName() 解析失败）
        assertEquals(401, (int) m.invoke(null, new org.springframework.security.authentication.BadCredentialsException("a")));
        assertEquals(500, (int) m.invoke(null, new RuntimeException("any")));
    }
}

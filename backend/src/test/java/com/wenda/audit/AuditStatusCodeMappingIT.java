package com.wenda.audit;

import com.wenda.audit.Audited.Risk;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Audit 状态码映射测试（基线：GOV-002 修复 #7）。
 *
 * <p>断言：
 * <ul>
 *   <li>BusinessException(FORBIDDEN) 审计 status_code = 403；</li>
 *   <li>BusinessException(UNAUTHORIZED) 审计 status_code = 401；</li>
 *   <li>BusinessException(NOT_FOUND) 审计 status_code = 404；</li>
 *   <li>BusinessException(IDEMPOTENCY_CONFLICT) 审计 status_code = 409；</li>
 *   <li>BusinessException(INTERNAL_ERROR) 审计 status_code = 500。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("it")
class AuditStatusCodeMappingIT {

    @Autowired
    AuditAspect aspect;

    @Autowired
    AuditService service;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        // 不清理 audit_logs 历史以保留排查
    }

    private int readLastStatusCode(UUID userId, String action) {
        Integer v = jdbc.queryForObject(
                "SELECT status_code FROM audit_logs WHERE user_id = ? AND action = ? "
                        + "ORDER BY created_at DESC LIMIT 1",
                Integer.class, userId, action);
        assertNotNull(v, "未找到审计行（action=" + action + "）");
        return v;
    }

    private int runAroundWithException(BusinessException be, String action) throws Throwable {
        // 显式构造一个被 @Around 拦截的 joinPoint
        ProceedingJoinPoint pjp = new StubJoinPoint("auditTestMethod", new Object[0]);
        // 通过反射调用 @Around 标记的 around 方法
        try {
            java.lang.reflect.Method m = AuditAspect.class.getDeclaredMethod("around",
                    ProceedingJoinPoint.class, Audited.class);
            m.setAccessible(true);
            Audited ann = makeAudited(action, be.errorCode().name().toLowerCase());
            try {
                m.invoke(aspect, pjp, ann);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        } catch (java.lang.NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
        return -1;
    }

    private Audited makeAudited(String action, String resourceType) throws Exception {
        return new Audited() {
            @Override public String action() { return action; }
            @Override public String resourceType() { return resourceType; }
            @Override public String resourceId() { return ""; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Audited.class; }
            @Override public Risk risk() { return Risk.NORMAL; }
        };
    }

    @Test
    void forbidden_业务异常_审计_403() throws Throwable {
        UUID userId = UUID.randomUUID();
        com.wenda.context.RequestContextHolder.setAuth(UUID.randomUUID(), UUID.randomUUID(), userId, "u", java.util.Set.of());
        try {
            runAroundWithException(new BusinessException(ErrorCode.FORBIDDEN, "forbidden"), "TEST_FORBIDDEN");
        } catch (BusinessException expected) {
            // expected
        }
        int status = readLastStatusCode(userId, "TEST_FORBIDDEN");
        assertEquals(403, status, "FORBIDDEN 业务异常应审计 403");
        com.wenda.context.RequestContextHolder.clear();
    }

    @Test
    void unauthorized_业务异常_审计_401() throws Throwable {
        UUID userId = UUID.randomUUID();
        com.wenda.context.RequestContextHolder.setAuth(UUID.randomUUID(), UUID.randomUUID(), userId, "u", java.util.Set.of());
        try {
            runAroundWithException(new BusinessException(ErrorCode.UNAUTHORIZED, "unauth"), "TEST_UNAUTH");
        } catch (BusinessException expected) {
        }
        int status = readLastStatusCode(userId, "TEST_UNAUTH");
        assertEquals(401, status, "UNAUTHORIZED 业务异常应审计 401");
        com.wenda.context.RequestContextHolder.clear();
    }

    @Test
    void notFound_业务异常_审计_404() throws Throwable {
        UUID userId = UUID.randomUUID();
        com.wenda.context.RequestContextHolder.setAuth(UUID.randomUUID(), UUID.randomUUID(), userId, "u", java.util.Set.of());
        try {
            runAroundWithException(new BusinessException(ErrorCode.NOT_FOUND, "nf"), "TEST_NF");
        } catch (BusinessException expected) {
        }
        int status = readLastStatusCode(userId, "TEST_NF");
        assertEquals(404, status, "NOT_FOUND 业务异常应审计 404");
        com.wenda.context.RequestContextHolder.clear();
    }

    @Test
    void idempotencyConflict_业务异常_审计_409() throws Throwable {
        UUID userId = UUID.randomUUID();
        com.wenda.context.RequestContextHolder.setAuth(UUID.randomUUID(), UUID.randomUUID(), userId, "u", java.util.Set.of());
        try {
            runAroundWithException(new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "conflict"), "TEST_IDEMP");
        } catch (BusinessException expected) {
        }
        int status = readLastStatusCode(userId, "TEST_IDEMP");
        assertEquals(409, status, "IDEMPOTENCY_CONFLICT 业务异常应审计 409");
        com.wenda.context.RequestContextHolder.clear();
    }

    @Test
    void internalError_业务异常_审计_500() throws Throwable {
        UUID userId = UUID.randomUUID();
        com.wenda.context.RequestContextHolder.setAuth(UUID.randomUUID(), UUID.randomUUID(), userId, "u", java.util.Set.of());
        try {
            runAroundWithException(new BusinessException(ErrorCode.INTERNAL_ERROR, "err"), "TEST_INT");
        } catch (BusinessException expected) {
        }
        int status = readLastStatusCode(userId, "TEST_INT");
        assertEquals(500, status, "INTERNAL_ERROR 业务异常应审计 500");
        com.wenda.context.RequestContextHolder.clear();
    }

    /** 极简 ProceedingJoinPoint stub，仅返回 null（不执行原方法）。 */
    private static class StubJoinPoint implements ProceedingJoinPoint {
        private final String name;
        private final Object[] args;
        StubJoinPoint(String name, Object[] args) { this.name = name; this.args = args; }
        @Override public Object proceed() { return null; }
        @Override public Object proceed(Object[] args) { return null; }
        @Override public Object getThis() { return null; }
        @Override public Object getTarget() { return null; }
        @Override public Object[] getArgs() { return args; }
        @Override public org.aopalliance.intercept.MethodSignature getSignature() { return null; }
        @Override public java.lang.reflect.Method getMethod() {
            try { return getClass().getMethod(name); } catch (Exception e) { return null; }
        }
        @Override public org.aopalliance.aop.Advice getAdvice() { return null; }
        @Override public boolean isBefore() { return false; }
        @Override public boolean isAfter() { return false; }
        @Override public String toShortString() { return name; }
        @Override public String toLongString() { return name; }
        @Override public Object getStaticPart() { return null; }
    }
}

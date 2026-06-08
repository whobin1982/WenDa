package com.wenda.audit;

import com.wenda.error.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 审计切面（基线：架构 v0.3 §6.2 M-23 + 权限矩阵 v1.0 §4）。
 */
@Aspect
@Component
@Configuration
@EnableAspectJAutoProxy
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result;
        int status = 200;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            status = mapExceptionStatus(t);
            throw t;
        } finally {
            try {
                MethodSignature sig = (MethodSignature) pjp.getSignature();
                String resourceId = extractResourceId(pjp, audited.resourceId());
                Map<String, Object> details = new HashMap<>();
                details.put("method", sig.getMethod().getName());
                details.put("argsCount", pjp.getArgs() == null ? 0 : pjp.getArgs().length);
                auditService.record(
                        audited.action(),
                        audited.resourceType(),
                        resourceId,
                        "X",
                        sig.getName(),
                        status,
                        details,
                        audited.risk());
            } catch (Exception ignored) {
                // 审计失败不影响业务；AOP 内部异常吞掉
            }
        }
    }

    private static int mapExceptionStatus(Throwable t) {
        // 修复 #7：BusinessException 的真实 HTTP 状态由其 errorCode 决定
        // （FORBIDDEN 403 / UNAUTHORIZED 401 / NOT_FOUND 404 / VALIDATION_ERROR 400 /
        //  IDEMPOTENCY_CONFLICT 409 等）。之前一律记 422 与安全审计统计失真。
        if (t instanceof BusinessException be) {
            return be.errorCode().httpStatus().value();
        }
        // 修复 GOV-002 第二轮：用 instanceof 替代 getSimpleName() switch
        // （之前 BadCredentialsException extends AuthenticationException 但 simpleName 是
        // "BadCredentialsException"，switch 匹配不到 → fallback default 500）。
        if (t instanceof org.springframework.security.access.AccessDeniedException) return 403;
        if (t instanceof org.springframework.security.core.AuthenticationException) return 401;
        return 500;
    }

    private static String extractResourceId(ProceedingJoinPoint pjp, String spEL) {
        if (spEL == null || spEL.isBlank()) return null;
        try {
            // 极简 SpEL 子集：#{#paramName.field} → 直接读 param
            String expr = spEL.replace("#{", "").replace("}", "").trim();
            if (expr.startsWith("#")) {
                String param = expr.substring(1);
                int dot = param.indexOf('.');
                String paramName = dot < 0 ? param : param.substring(0, dot);
                String field = dot < 0 ? null : param.substring(dot + 1);
                Object[] args = pjp.getArgs();
                if (args == null) return null;
                var names = ((MethodSignature) pjp.getSignature()).getParameterNames();
                if (names == null) return null;
                for (int i = 0; i < names.length; i++) {
                    if (paramName.equals(names[i])) {
                        Object v = args[i];
                        if (v == null) return null;
                        if (field == null) return String.valueOf(v);
                        try {
                            var m = v.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
                            Object val = m.invoke(v);
                            return val == null ? null : String.valueOf(val);
                        } catch (NoSuchMethodException ignored) {
                            return String.valueOf(v);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 供非注解场景手动记录。 */
    public void record(String action, String resourceType, String resourceId, int status,
                       Map<String, Object> details, Audited.Risk risk) {
        auditService.record(action, resourceType, resourceId, "X", "manual", status, details, risk);
    }

    /** 兼容旧签名（仅 UUID 资源 ID） */
    public void record(String action, String resourceType, UUID resourceId, int status,
                       Map<String, Object> details, Audited.Risk risk) {
        record(action, resourceType, resourceId == null ? null : resourceId.toString(),
                status, details, risk);
    }
}

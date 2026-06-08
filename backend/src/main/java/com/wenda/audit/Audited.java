package com.wenda.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识一个写操作需要审计。基线：架构 v0.3 §6.2 M-23 + 权限矩阵 v1.0。
 *
 * <p>所有 controller 写方法都应标注；切面 {@link AuditAspect} 自动收集上下文并写入
 * {@code audit_logs} 追加表。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Audited {
    String action();
    String resourceType();
    /** SpEL：用于从入参中提取 resourceId；可为空。 */
    String resourceId() default "";
    Risk risk() default Risk.NORMAL;
    enum Risk { NORMAL, SENSITIVE, SECURITY }
}

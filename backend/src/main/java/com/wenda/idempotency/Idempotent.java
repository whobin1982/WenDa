package com.wenda.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识一个接口或类需要 {@code Idempotency-Key} 头支持。
 *
 * <p>基线：接口文档 v0.2 §2.5 通用幂等规则——创建类 / 任务类 / 导入提交 / 报告导出 /
 * 授权创建 / 企业邀请 等接口必须支持。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Idempotent {
}

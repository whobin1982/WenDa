package com.wenda.error;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * 全量错误码枚举（基线：api_error_code_dictionary_v1.0.md + api_contract_v0.2 §2.6/§2.7）。
 *
 * <p>全量共 <b>40 条</b>：3 条成功码（OK / CREATED / ACCEPTED）+ 37 条错误码。
 *
 * <p>新增错误码必须同步更新：
 * <ol>
 *   <li>{@code doc/baseline/api_error_code_dictionary_v1.0.md}；</li>
 *   <li>{@code doc/baseline/api_error_code_matrix_exact_v1.0.md}；</li>
 *   <li>本枚举；</li>
 *   <li>{@code ErrorCodeTest.totalCountMatchesBaseline}（断言 = 40）。</li>
 * </ol>
 *
 * <p>每个枚举项固定 4 个字段：HTTP 状态、英文 code、中文标准 message、是否进入审计。
 */
public enum ErrorCode {

    // ===== 2xx =====
    OK              (HttpStatus.OK,                     "OK",                            "成功。",                           false),
    CREATED         (HttpStatus.CREATED,                "CREATED",                       "创建成功。",                       false),
    ACCEPTED        (HttpStatus.ACCEPTED,               "ACCEPTED",                      "异步任务已创建。",                 true),

    // ===== 4xx =====
    BAD_REQUEST                 (HttpStatus.BAD_REQUEST,    "BAD_REQUEST",                 "请求格式错误。",                   false),
    VALIDATION_ERROR            (HttpStatus.BAD_REQUEST,    "VALIDATION_ERROR",            "参数校验失败。",                   false),
    UNAUTHORIZED                (HttpStatus.UNAUTHORIZED,   "UNAUTHORIZED",                "未登录或 Token 无效。",            true),
    TOKEN_EXPIRED               (HttpStatus.UNAUTHORIZED,   "TOKEN_EXPIRED",               "登录已过期，请重新登录。",         true),
    FORBIDDEN                   (HttpStatus.FORBIDDEN,      "FORBIDDEN",                   "无权限访问资源。",                 true),
    ACCESS_DENIED               (HttpStatus.FORBIDDEN,      "ACCESS_DENIED",               "访问被拒绝。",                     true),
    SCOPE_FORBIDDEN             (HttpStatus.FORBIDDEN,      "SCOPE_FORBIDDEN",             "无权访问该组织或资源范围。",       true),
    NOT_FOUND                   (HttpStatus.NOT_FOUND,      "NOT_FOUND",                   "资源不存在或不可见。",             false),
    CONFLICT                    (HttpStatus.CONFLICT,       "CONFLICT",                    "唯一性冲突或业务冲突。",           false),
    VERSION_CONFLICT            (HttpStatus.CONFLICT,       "VERSION_CONFLICT",            "资源版本不一致。",                 false),
    IDEMPOTENCY_CONFLICT        (HttpStatus.CONFLICT,       "IDEMPOTENCY_CONFLICT",        "幂等键已用于不同请求体。",         true),
    BUSINESS_STATE_INVALID      (HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_STATE_INVALID", "当前状态不允许执行该操作。",      true),
    SCHOOL_SCOPE_REQUIRED       (HttpStatus.UNPROCESSABLE_ENTITY, "SCHOOL_SCOPE_REQUIRED", "缺少学校空间上下文。",            true),
    FILE_TOO_LARGE              (HttpStatus.PAYLOAD_TOO_LARGE,    "FILE_TOO_LARGE",       "文件超过大小限制。",               true),
    UNSUPPORTED_FILE_TYPE       (HttpStatus.UNSUPPORTED_MEDIA_TYPE,"UNSUPPORTED_FILE_TYPE","文件类型不支持。",                true),
    FILE_TYPE_NOT_ALLOWED       (HttpStatus.UNSUPPORTED_MEDIA_TYPE,"FILE_TYPE_NOT_ALLOWED","该业务场景不允许上传此文件类型。",  true),
    FILE_SECURITY_SCAN_PENDING  (HttpStatus.LOCKED,               "FILE_SECURITY_SCAN_PENDING","文件尚未通过安全扫描。",       true),
    FILE_SECURITY_SCAN_FAILED   (HttpStatus.UNPROCESSABLE_ENTITY, "FILE_SECURITY_SCAN_FAILED","文件安全扫描未通过。",          true),
    FILE_ACCESS_DENIED          (HttpStatus.FORBIDDEN,            "FILE_ACCESS_DENIED",     "无权访问该文件。",                 true),
    AI_PROVIDER_DISABLED        (HttpStatus.UNPROCESSABLE_ENTITY, "AI_PROVIDER_DISABLED",  "学校未启用对应 AI Provider。",     true),
    AI_OUTPUT_SCHEMA_INVALID    (HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_SCHEMA_INVALID","AI 输出不符合结构化 Schema。",    true),
    AI_TASK_FAILED              (HttpStatus.UNPROCESSABLE_ENTITY, "AI_TASK_FAILED",        "AI 任务执行失败。",                true),
    TASK_NOT_FOUND              (HttpStatus.NOT_FOUND,            "TASK_NOT_FOUND",        "异步任务不存在或不可见。",         false),
    TASK_ALREADY_RUNNING        (HttpStatus.CONFLICT,             "TASK_ALREADY_RUNNING",  "任务已在执行中。",                 false),
    TASK_TIMEOUT                (HttpStatus.UNPROCESSABLE_ENTITY, "TASK_TIMEOUT",          "任务执行超时。",                   true),
    REPORT_TEMPLATE_NOT_FOUND   (HttpStatus.NOT_FOUND,            "REPORT_TEMPLATE_NOT_FOUND","报告模板不存在或不可用。",       true),
    REPORT_FILE_NOT_READY       (HttpStatus.LOCKED,               "REPORT_FILE_NOT_READY", "报告文件尚未生成完成。",           false),
    KNOWLEDGE_SOURCE_DISABLED   (HttpStatus.UNPROCESSABLE_ENTITY, "KNOWLEDGE_SOURCE_DISABLED","知识库资料已禁用或不可正式引用。",true),
    KNOWLEDGE_SEARCH_UNSUPPORTED_FILTER(HttpStatus.BAD_REQUEST,    "KNOWLEDGE_SEARCH_UNSUPPORTED_FILTER","知识库检索筛选条件不支持。",false),
    AUTHORIZATION_EXPIRED       (HttpStatus.FORBIDDEN,            "AUTHORIZATION_EXPIRED", "学生授权已过期。",                 true),
    AUTHORIZATION_REVOKED       (HttpStatus.FORBIDDEN,            "AUTHORIZATION_REVOKED", "学生授权已撤销。",                 true),
    DISPLAY_SCOPE_FORBIDDEN     (HttpStatus.FORBIDDEN,            "DISPLAY_SCOPE_FORBIDDEN","请求内容超出学生授权展示范围。",  true),
    IMPORT_TEMPLATE_INVALID     (HttpStatus.BAD_REQUEST,          "IMPORT_TEMPLATE_INVALID","导入模板版本或结构无效。",        true),
    IMPORT_CONFLICT_REQUIRES_DECISION(HttpStatus.CONFLICT,        "IMPORT_CONFLICT_REQUIRES_DECISION","导入存在冲突，需要用户选择处理策略。",true),
    RATE_LIMITED                (HttpStatus.TOO_MANY_REQUESTS,    "RATE_LIMITED",          "请求过于频繁。",                   true),

    // ===== 5xx =====
    INTERNAL_ERROR              (HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR",        "服务端异常。",                     true),
    DEPENDENCY_UNAVAILABLE      (HttpStatus.SERVICE_UNAVAILABLE,  "DEPENDENCY_UNAVAILABLE","外部依赖不可用。",                 true);

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;
    private final boolean audited;

    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage, boolean audited) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.audited = audited;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public boolean audited() {
        return audited;
    }

    public static ErrorCode ofCode(String code) {
        for (ErrorCode ec : values()) {
            if (Objects.equals(ec.code, code)) {
                return ec;
            }
        }
        return null;
    }
}

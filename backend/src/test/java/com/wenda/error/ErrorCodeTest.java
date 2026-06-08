package com.wenda.error;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全量错误码字典与基线文件一致性测试（基线：api_error_code_dictionary_v1.0.md）。
 *
 * <p>约束：
 * <ol>
 *   <li>枚举数量 = 40（与基线 1:1 对齐；3 成功 + 37 错误）；</li>
 *   <li>每个枚举项的 code 唯一；</li>
 *   <li>必含 40 条基线 code（见 {@code mustContainBaselineCodes}）；</li>
 *   <li>每个枚举项的 code 非空、defaultMessage 非空。</li>
 * </ol>
 *
 * <p>新增 / 删除错误码必须同步更新本测试与基线文件。
 */
class ErrorCodeTest {

    @Test
    void allCodesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values()) {
            assertTrue(seen.add(ec.code()), "duplicate error code: " + ec.code());
            assertNotNull(ec.code());
            assertTrue(!ec.code().isBlank(), "empty code");
            assertNotNull(ec.defaultMessage());
            assertTrue(!ec.defaultMessage().isBlank(), "empty message for " + ec.code());
            assertNotNull(ec.httpStatus());
        }
    }

    @Test
    void totalCountMatchesBaseline() {
        // 基线字典 v1.0 共 40 条：3 个成功码（OK / CREATED / ACCEPTED）+ 37 个错误码。
        assertEquals(40, ErrorCode.values().length, "枚举总数必须 = 40（与基线字典一致）");
    }

    @Test
    void mustContainBaselineCodes() {
        String[] required = {
                "OK", "CREATED", "ACCEPTED",
                "BAD_REQUEST", "VALIDATION_ERROR", "UNAUTHORIZED", "TOKEN_EXPIRED",
                "FORBIDDEN", "ACCESS_DENIED", "SCOPE_FORBIDDEN",
                "NOT_FOUND", "CONFLICT", "VERSION_CONFLICT", "IDEMPOTENCY_CONFLICT",
                "BUSINESS_STATE_INVALID", "SCHOOL_SCOPE_REQUIRED",
                "FILE_TOO_LARGE", "UNSUPPORTED_FILE_TYPE", "FILE_TYPE_NOT_ALLOWED",
                "FILE_SECURITY_SCAN_PENDING", "FILE_SECURITY_SCAN_FAILED", "FILE_ACCESS_DENIED",
                "AI_PROVIDER_DISABLED", "AI_OUTPUT_SCHEMA_INVALID", "AI_TASK_FAILED",
                "TASK_NOT_FOUND", "TASK_ALREADY_RUNNING", "TASK_TIMEOUT",
                "REPORT_TEMPLATE_NOT_FOUND", "REPORT_FILE_NOT_READY",
                "KNOWLEDGE_SOURCE_DISABLED", "KNOWLEDGE_SEARCH_UNSUPPORTED_FILTER",
                "AUTHORIZATION_EXPIRED", "AUTHORIZATION_REVOKED", "DISPLAY_SCOPE_FORBIDDEN",
                "IMPORT_TEMPLATE_INVALID", "IMPORT_CONFLICT_REQUIRES_DECISION",
                "RATE_LIMITED", "INTERNAL_ERROR", "DEPENDENCY_UNAVAILABLE"
        };
        Set<String> have = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values()) have.add(ec.code());
        for (String r : required) {
            assertTrue(have.contains(r), "缺少基线错误码：" + r);
        }
    }

    @Test
    void ofCodeReturnsEnum() {
        assertEquals(ErrorCode.OK, ErrorCode.ofCode("OK"));
        assertEquals(ErrorCode.FORBIDDEN, ErrorCode.ofCode("FORBIDDEN"));
        assertEquals(null, ErrorCode.ofCode("UNKNOWN_CODE_XYZ"));
    }
}

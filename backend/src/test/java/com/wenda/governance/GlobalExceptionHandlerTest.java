package com.wenda.governance;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局异常处理测试（基线：接口文档 v0.2 §2.3 / §2.6）。
 */
class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionMapsToCorrectCode() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND, "学校空间不存在。");
        assertEquals(ErrorCode.NOT_FOUND, ex.errorCode());
        assertEquals("学校空间不存在。", ex.getMessage());
    }

    @Test
    void errorCodeAuditedFlagMatchesBaseline() {
        Map<ErrorCode, Boolean> expected = new HashMap<>();
        expected.put(ErrorCode.OK, false);
        expected.put(ErrorCode.CREATED, false);
        expected.put(ErrorCode.ACCEPTED, true);
        expected.put(ErrorCode.UNAUTHORIZED, true);
        expected.put(ErrorCode.FORBIDDEN, true);
        expected.put(ErrorCode.NOT_FOUND, false);
        expected.put(ErrorCode.BUSINESS_STATE_INVALID, true);
        expected.put(ErrorCode.AI_PROVIDER_DISABLED, true);
        expected.put(ErrorCode.INTERNAL_ERROR, true);
        expected.put(ErrorCode.DEPENDENCY_UNAVAILABLE, true);
        expected.forEach((k, v) -> assertEquals(v, k.audited(), k.code()));
    }

    @Test
    void errorCodeHttpStatusMatchesBaseline() {
        assertEquals(200, ErrorCode.OK.httpStatus().value());
        assertEquals(201, ErrorCode.CREATED.httpStatus().value());
        assertEquals(202, ErrorCode.ACCEPTED.httpStatus().value());
        assertEquals(400, ErrorCode.BAD_REQUEST.httpStatus().value());
        assertEquals(400, ErrorCode.VALIDATION_ERROR.httpStatus().value());
        assertEquals(401, ErrorCode.UNAUTHORIZED.httpStatus().value());
        assertEquals(401, ErrorCode.TOKEN_EXPIRED.httpStatus().value());
        assertEquals(403, ErrorCode.FORBIDDEN.httpStatus().value());
        assertEquals(409, ErrorCode.VERSION_CONFLICT.httpStatus().value());
        assertEquals(409, ErrorCode.IDEMPOTENCY_CONFLICT.httpStatus().value());
        assertEquals(422, ErrorCode.BUSINESS_STATE_INVALID.httpStatus().value());
        assertEquals(413, ErrorCode.FILE_TOO_LARGE.httpStatus().value());
        assertEquals(415, ErrorCode.UNSUPPORTED_FILE_TYPE.httpStatus().value());
        assertEquals(423, ErrorCode.FILE_SECURITY_SCAN_PENDING.httpStatus().value());
        assertEquals(429, ErrorCode.RATE_LIMITED.httpStatus().value());
        assertEquals(500, ErrorCode.INTERNAL_ERROR.httpStatus().value());
        assertEquals(503, ErrorCode.DEPENDENCY_UNAVAILABLE.httpStatus().value());
    }

    @Test
    void ofCodeNullSafe() {
        assertNull(ErrorCode.ofCode(null));
        assertNull(ErrorCode.ofCode("__UNKNOWN__"));
    }

    @Test
    void allErrorCodesHaveNonEmptyStandardMessage() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertNotNull(ec.defaultMessage());
            assertTrue(ec.defaultMessage().length() > 0,
                    ec.code() + " 必须有标准 message");
        }
    }
}

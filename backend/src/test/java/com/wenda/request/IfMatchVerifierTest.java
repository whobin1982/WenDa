package com.wenda.request;

import com.wenda.config.WendaProperties;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * If-Match 校验测试（基线：接口文档 v0.2 §2.5）。
 */
class IfMatchVerifierTest {

    private IfMatchVerifier verifier;
    private WendaProperties properties;

    @BeforeEach
    void setUp() {
        properties = new WendaProperties();
        verifier = new IfMatchVerifier(properties);
    }

    @Test
    void parseIfMatchQuoted() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("If-Match", "\"5\"");
        Long v = verifier.parseIfMatch(req);
        assertNotNull(v);
        assertEquals(5L, v);
    }

    @Test
    void parseIfMatchBareNumber() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("If-Match", "12");
        assertEquals(12L, verifier.parseIfMatch(req));
    }

    @Test
    void parseIfMatchMissingReturnsNull() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertEquals(null, verifier.parseIfMatch(req));
    }

    @Test
    void parseIfMatchInvalidThrowsBadRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("If-Match", "abc");
        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.parseIfMatch(req));
        assertEquals(ErrorCode.BAD_REQUEST, ex.errorCode());
    }

    @Test
    void assertVersionMismatchThrowsVersionConflict() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () -> verifier.assertVersion(req, 3L, 5L));
    }

    @Test
    void assertVersionMissingThrowsVersionConflict() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> verifier.assertVersion(req, null, 5L));
        assertEquals(ErrorCode.VERSION_CONFLICT, ex.errorCode());
    }
}

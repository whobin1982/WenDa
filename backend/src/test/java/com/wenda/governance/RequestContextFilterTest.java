package com.wenda.governance;

import com.wenda.config.RequestContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import com.wenda.config.WendaProperties;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RequestContextFilter 单元测试（基线：接口文档 v0.2 §2.2 X-Request-Id）。
 */
class RequestContextFilterTest {

    @org.junit.jupiter.api.BeforeEach
    void clear() {
        com.wenda.context.RequestContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        com.wenda.context.RequestContextHolder.clear();
    }

    @Test
    void generatesRequestIdWhenMissing() throws ServletException, IOException {
        var filter = new RequestContextFilter(props());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = (request, response) ->
                captured.set(com.wenda.context.RequestContextHolder.requestId());
        filter.doFilter(req, res, chain);
        assertNotNull(captured.get());
        assertEquals(captured.get(), res.getHeader("X-Request-Id"));
    }

    @Test
    void passesThroughIncomingRequestId() throws ServletException, IOException {
        var filter = new RequestContextFilter(props());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/x");
        req.addHeader("X-Request-Id", "req_client_123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = (request, response) ->
                captured.set(com.wenda.context.RequestContextHolder.requestId());
        filter.doFilter(req, res, chain);
        assertEquals("req_client_123", captured.get());
        assertEquals("req_client_123", res.getHeader("X-Request-Id"));
    }

    @Test
    void requestIdIsClearedAfterChain() throws ServletException, IOException {
        var filter = new RequestContextFilter(props());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        // After chain completes, thread local should be cleared
        org.junit.jupiter.api.Assertions.assertEquals("",
                com.wenda.context.RequestContextHolder.requestId());
    }

    private static WendaProperties props() {
        return new WendaProperties();
    }
}

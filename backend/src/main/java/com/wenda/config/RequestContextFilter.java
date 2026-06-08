package com.wenda.config;

import com.wenda.context.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求上下文过滤器：生成或透传 {@code X-Request-Id}，写入 MDC 供日志使用。
 *
 * <p>基线：接口文档 v0.2 §2.2。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String ATTR_REQUEST_ID = "wenda.requestId";
    public static final String ATTR_START_TS = "wenda.startTs";
    private static final String REQ_ID_PREFIX = "req_";

    private final WendaProperties properties;

    public RequestContextFilter(WendaProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = properties.getRequest().getHeader().getRequestId();
        String incoming = request.getHeader(header);
        String requestId;
        if (StringUtils.hasText(incoming)) {
            int max = properties.getRequest().getRequestIdMaxLen();
            requestId = incoming.length() > max ? incoming.substring(0, max) : incoming;
        } else {
            requestId = REQ_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
        }
        request.setAttribute(ATTR_REQUEST_ID, requestId);
        request.setAttribute(ATTR_START_TS, System.currentTimeMillis());
        MDC.put("requestId", requestId);
        response.setHeader(header, requestId);
        try {
            RequestContextHolder.set(requestId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            RequestContextHolder.clear();
        }
    }
}

package com.wenda.idempotency;

import com.wenda.config.WendaProperties;
import com.wenda.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.UUID;

/**
 * 响应体 advice：把幂等响应持久化到 DB。
 *
 * <p>调用 {@link IdempotencyInterceptor#recordResponse} 时传入：compositeKey、
 * schoolId、userId、method、path、原始 key、status、body。method/path 真实值，
 * DB 字段 {@code key} 存原始头值。
 */
@RestControllerAdvice
public class IdempotencyResponseAdvice implements ResponseBodyAdvice<Object> {

    private final IdempotencyInterceptor interceptor;
    private final WendaProperties properties;

    public IdempotencyResponseAdvice(IdempotencyInterceptor interceptor, WendaProperties properties) {
        this.interceptor = interceptor;
        this.properties = properties;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return properties.getIdempotency().isEnabled();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest sreq)) return body;
        if (request.getMethod() == null
                || !"POST".equalsIgnoreCase(request.getMethod().name())) return body;
        HttpServletRequest raw = sreq.getServletRequest();
        String key = raw.getHeader(properties.getRequest().getHeader().getIdempotencyKey());
        if (key == null || key.isBlank()) return body;
        UUID userId = RequestContextHolder.userId();
        UUID schoolId = RequestContextHolder.schoolId();
        if (schoolId == null || userId == null) return body;
        String compositeKey = schoolId + ":" + userId + ":" + key;
        int status = 200;
        try {
            if (response instanceof ServletServerHttpResponse sresp
                    && sresp.getServletResponse() != null) {
                status = sresp.getServletResponse().getStatus();
            }
        } catch (Exception ignored) {
        }
        interceptor.recordResponse(compositeKey, schoolId, userId,
                request.getMethod().name(),
                raw.getRequestURI(),
                key,
                status,
                body == null ? "" : body.toString());
        return body;
    }
}

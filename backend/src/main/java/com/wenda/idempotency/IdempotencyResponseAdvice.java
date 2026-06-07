package com.wenda.idempotency;

import com.wenda.config.WendaProperties;
import com.wenda.context.RequestContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.UUID;

/**
 * 响应体 advice：缓存幂等响应到数据库。
 *
 * <p>对未标注 {@link Idempotent} 的接口自动 no-op。
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
        String key = sreq.getServletRequest().getHeader(properties.getRequest().getHeader().getIdempotencyKey());
        if (key == null || key.isBlank()) return body;
        UUID userId = RequestContextHolder.userId();
        UUID schoolId = RequestContextHolder.schoolId();
        if (schoolId == null || userId == null) return body;
        String compositeKey = schoolId + ":" + userId + ":" + key;
        int status = 200;
        try {
            if (response instanceof org.springframework.http.server.ServletServerHttpResponse sresp
                    && sresp.getServletResponse() != null) {
                status = sresp.getServletResponse().getStatus();
            }
        } catch (Exception ignored) {
        }
        interceptor.recordResponse(compositeKey, status, body == null ? "" : body.toString());
        return body;
    }
}

package com.wenda.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Set;
import java.util.UUID;

/**
 * 响应体 advice：把幂等响应持久化到 DB（基线：接口文档 v0.2 §2.5 + GOV-002 修复 #1）。
 *
 * <p>修复 #1（外部 GitHub 审查 3 个子项）：
 * <ol>
 *   <li>持久化所有标记 {@link Idempotent} 的 HTTP method：POST / PUT / PATCH / DELETE
 *       （不再仅 POST）。</li>
 *   <li>序列化响应体时使用 Jackson ObjectMapper（不是 body.toString()），确保缓存
 *       内容是合法 API JSON。</li>
 *   <li>异常路径不持久化缓存；业务异常由 {@link IdempotencyInterceptor#preHandle}
 *       的 try/finally 清理 inFlight 占位。</li>
 * </ol>
 *
 * <p>ID 是通过 {@link IdempotencyInterceptor} 注册的 in-flight 占位；
 * advice 不会重复注册。
 */
@RestControllerAdvice
public class IdempotencyResponseAdvice implements ResponseBodyAdvice<Object> {

    /** 标记为 {@link Idempotent} 的 HTTP method 集合（advice 负责持久化缓存）。 */
    private static final Set<String> IDEMPOTENT_METHODS =
            Set.of("POST", "PUT", "PATCH", "DELETE");

    private final IdempotencyInterceptor interceptor;
    private final WendaProperties properties;
    private final ObjectMapper objectMapper;

    public IdempotencyResponseAdvice(IdempotencyInterceptor interceptor,
                                     WendaProperties properties,
                                     ObjectMapper objectMapper) {
        this.interceptor = interceptor;
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        if (request.getMethod() == null) return body;
        String method = request.getMethod().name();
        if (!IDEMPOTENT_METHODS.contains(method.toUpperCase())) return body;

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

        // 仅持久化成功响应（2xx）；失败响应在 Interceptor 的异常 finally 中清理占位。
        if (status < 200 || status >= 300) {
            interceptor.clearInFlight(compositeKey);
            return body;
        }

        String bodyJson = serializeBody(body);
        interceptor.recordResponse(compositeKey, schoolId, userId, method,
                raw.getRequestURI(), key, status, bodyJson);
        return body;
    }

    private String serializeBody(Object body) {
        if (body == null) return "";
        if (body instanceof CharSequence cs) return cs.toString();
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            // 序列化失败：不持久化缓存；让 Interceptor 的 finally 清理 inFlight
            throw new IllegalStateException("failed to serialize idempotent response body", ex);
        }
    }
}

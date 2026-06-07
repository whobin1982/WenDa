package com.wenda.idempotency;

import com.wenda.config.WendaProperties;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 幂等键拦截器。
 *
 * <p>基线：接口文档 v0.2 §2.5 通用幂等规则 + §2.2 Idempotency-Key 头；
 * 需要幂等的接口必须添加 {@link Idempotent} 注解。
 *
 * <p>行为：
 * <ul>
 *   <li>命中：相同 key + 相同 request hash → 直接返回首次结果（基于数据库行）</li>
 *   <li>冲突：相同 key + 不同 request hash → 抛 {@link BusinessException} {@code IDEMPOTENCY_CONFLICT}</li>
 *   <li>未命中：写入占位行 → 执行业务 → 写回响应</li>
 * </ul>
 *
 * <p>对未标注 {@link Idempotent} 的接口不生效。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyInterceptor.class);

    private final WendaProperties properties;
    private final JdbcTemplate jdbc;
    private final ConcurrentMap<String, CachedResponse> inFlight = new ConcurrentHashMap<>();

    public IdempotencyInterceptor(WendaProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        Idempotent anno = hm.getMethodAnnotation(Idempotent.class);
        if (anno == null && hm.getBeanType().getAnnotation(Idempotent.class) == null) {
            return true;
        }
        if (!properties.getIdempotency().isEnabled()) {
            return true;
        }
        String key = request.getHeader(properties.getRequest().getHeader().getIdempotencyKey());
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "缺少 Idempotency-Key 头。");
        }
        if (key.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key 长度不能超过 128。");
        }
        UUID userId = RequestContextHolder.userId();
        UUID schoolId = RequestContextHolder.schoolId();
        if (userId == null || schoolId == null) {
            // 匿名请求（如登录）允许使用 IP + UA 作为辅助键
            String fallback = (request.getRemoteAddr() == null ? "anon" : request.getRemoteAddr())
                    + "|" + (request.getHeader("User-Agent") == null ? "" : request.getHeader("User-Agent"));
            userId = UUID.nameUUIDFromBytes(fallback.getBytes(StandardCharsets.UTF_8));
            schoolId = UUID.nameUUIDFromBytes(("anon-school-" + fallback).getBytes(StandardCharsets.UTF_8));
        }
        String requestHash = hashBody(readBody(request));
        String compositeKey = schoolId + ":" + userId + ":" + key;

        CachedResponse cached = inFlight.get(compositeKey);
        if (cached != null) {
            if (!cached.requestHash.equals(requestHash)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            return false; // skip controller; 上层由 ResponseBodyAdvice 输出
        }

        var rows = jdbc.query(
                "SELECT request_hash, response_status, response_body FROM idempotency_keys "
                        + "WHERE school_id = ? AND user_id = ? AND key = ? AND expires_at > now()",
                (rs, i) -> new Object[]{rs.getString(1), rs.getInt(2), rs.getString(3)},
                schoolId, userId, key);
        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            if (!requestHash.equals(row[0])) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            writeCachedResponse(response, (Integer) row[1], (String) row[2]);
            return false;
        }
        inFlight.put(compositeKey, new CachedResponse(requestHash, null, null));
        return true;
    }

    public void recordResponse(String key, int status, String body) {
        CachedResponse cached = inFlight.remove(key);
        if (cached == null) return;
        UUID userId = RequestContextHolder.userId();
        UUID schoolId = RequestContextHolder.schoolId();
        if (schoolId == null || userId == null) return;
        try {
            jdbc.update(
                    "INSERT INTO idempotency_keys "
                            + "(key, school_id, user_id, method, path, request_hash, response_status, response_body, created_at, expires_at) "
                            + "VALUES (?,?,?,?,?,?,?,?, now(), now() + (? || ' seconds')::interval) "
                            + "ON CONFLICT (school_id, user_id, key) DO NOTHING",
                    key, schoolId, userId, "X", "/x", cached.requestHash, status, body,
                    String.valueOf(properties.getIdempotency().getTtlSeconds()));
        } catch (Exception ex) {
            log.warn("idempotency record failed key={} err={}", key, ex.getMessage());
        }
    }

    private static String readBody(HttpServletRequest request) {
        try (InputStream is = request.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private static String hashBody(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(body.hashCode());
        }
    }

    private static void writeCachedResponse(jakarta.servlet.http.HttpServletResponse response, int status, String body) {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getOutputStream().write(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    public record CachedResponse(String requestHash, Integer status, String body) {}
}

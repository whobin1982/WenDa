package com.wenda.idempotency;

import com.wenda.config.WendaProperties;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 幂等拦截器（基线：接口文档 v0.2 §2.5）。
 *
 * <p>行为（修复版）：
 * <ul>
 *   <li>命中 DB 记录：相同 {@code key + schoolId + userId} + 相同 request hash
 *       → 返回首次结果；</li>
 *   <li>冲突：相同 key + 不同 request hash → {@code IDEMPOTENCY_CONFLICT}；</li>
 *   <li>未命中：DB 落占位行 + 执行业务 + ResponseAdvice 写回响应；</li>
 * </ul>
 *
 * <p>DB 字段约定：
 * <ul>
 *   <li>{@code key} 列：原始 {@code Idempotency-Key} 头值；</li>
 *   <li>唯一键：{@code (school_id, user_id, key)}；</li>
 *   <li>{@code request_hash}：SHA-256(method + path + key + schoolId + userId + body)；</li>
 *   <li>{@code method} / {@code path}：真实值（不再写死 X / /x）。</li>
 * </ul>
 *
 * <p>body 字节由 {@link IdempotencyRequestFilter} 包装的
 * {@code CachedBodyHttpServletRequest} 提供；拦截器从
 * {@code getCachedBody()} 读取（永远可重复读）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyInterceptor.class);

    private final WendaProperties properties;
    private final JdbcTemplate jdbc;
    /** in-memory 缓存：compositeKey → CachedResponse（仅进程内；DB 是持久层）。 */
    private final ConcurrentMap<String, CachedResponse> inFlight = new ConcurrentHashMap<>();

    public IdempotencyInterceptor(WendaProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        Idempotent anno = hm.getMethodAnnotation(Idempotent.class);
        if (anno == null && hm.getBeanType().getAnnotation(Idempotent.class) == null) {
            return true;
        }
        if (!properties.getIdempotency().isEnabled()) {
            return true;
        }

        // 1) 解析 key
        String key = request.getHeader(properties.getRequest().getHeader().getIdempotencyKey());
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "缺少 Idempotency-Key 头。");
        }
        if (key.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key 长度不能超过 128。");
        }

        // 2) 解析 schoolId / userId；匿名场景使用 IP+UA 派生
        UUID userId = RequestContextHolder.userId();
        UUID schoolId = RequestContextHolder.schoolId();
        if (userId == null || schoolId == null) {
            String fallback = (request.getRemoteAddr() == null ? "anon" : request.getRemoteAddr())
                    + "|" + (request.getHeader("User-Agent") == null ? "" : request.getHeader("User-Agent"));
            userId = UUID.nameUUIDFromBytes(fallback.getBytes(StandardCharsets.UTF_8));
            schoolId = UUID.nameUUIDFromBytes(("anon-school-" + fallback).getBytes(StandardCharsets.UTF_8));
        }

        // 3) 读取 body（来自 IdempotencyRequestFilter 的缓存 wrapper）
        byte[] body = readBody(request);
        String method = request.getMethod() == null ? "?" : request.getMethod();
        String path = request.getRequestURI();
        String requestHash = hashRequest(method, path, key, schoolId, userId, body);

        // 4) in-memory 命中
        String compositeKey = schoolId + ":" + userId + ":" + key;
        CachedResponse cached = inFlight.get(compositeKey);
        if (cached != null) {
            if (!cached.requestHash.equals(requestHash)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            writeCachedResponse(response, cached.status, cached.body);
            return false;
        }

        // 5) DB 命中
        var rows = jdbc.query(
                "SELECT request_hash, response_status, response_body FROM idempotency_keys "
                        + "WHERE school_id = ? AND user_id = ? AND key = ? AND expires_at > now()",
                (rs, i) -> new Object[]{
                        rs.getString(1), rs.getInt(2), rs.getString(3)
                },
                schoolId, userId, key);
        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            if (!requestHash.equals(row[0])) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            writeCachedResponse(response, (Integer) row[1], (String) row[2]);
            return false;
        }

        // 6) 未命中：注册 in-flight 占位
        inFlight.put(compositeKey, new CachedResponse(requestHash, null, null));
        return true;
    }

    /**
     * 异常 / 非 2xx 路径：清理 in-flight 占位（基线 GOV-002 修复 #1）。
     *
     * <p>业务异常由 controller 抛出，会绕过 ResponseBodyAdvice 直接到
     * {@link GlobalExceptionHandler}；此时由本钩子清理 inFlight，
     * 避免"同 key 残留占位导致后续请求命中空 body"。
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod hm)) return;
        Idempotent anno = hm.getMethodAnnotation(Idempotent.class);
        if (anno == null && hm.getBeanType().getAnnotation(Idempotent.class) == null) return;
        if (!properties.getIdempotency().isEnabled()) return;

        // 任何带 Idempotency-Key 但未成功持久化（业务异常 / 非 2xx）的请求，清理占位
        String key = request.getHeader(properties.getRequest().getHeader().getIdempotencyKey());
        if (!StringUtils.hasText(key)) return;
        UUID userId = RequestContextHolder.userId();
        UUID schoolId = RequestContextHolder.schoolId();
        if (userId == null || schoolId == null) return;

        int status = 200;
        try {
            status = response.getStatus();
        } catch (Exception ignored) {
        }
        if (ex != null || status >= 300) {
            String compositeKey = schoolId + ":" + userId + ":" + key;
            inFlight.remove(compositeKey);
        }
    }

    /**
     * 由 {@link IdempotencyResponseAdvice} 在非 2xx 响应时显式调用清理 inFlight。
     */
    public void clearInFlight(String compositeKey) {
        if (compositeKey != null) {
            inFlight.remove(compositeKey);
        }
    }

    /**
     * 业务完成后由 {@link IdempotencyResponseAdvice} 调用：把响应写回 DB。
     *
     * @param compositeKey in-memory 缓存 key（与 preHandle 一致）
     * @param schoolId     学校 ID
     * @param userId       用户 ID
     * @param method       真实 HTTP 方法
     * @param path         真实 HTTP 路径
     * @param key          原始 Idempotency-Key
     * @param status       响应状态码
     * @param body         响应体
     */
    public void recordResponse(String compositeKey, UUID schoolId, UUID userId,
                               String method, String path, String key,
                               int status, String body) {
        CachedResponse cached = inFlight.remove(compositeKey);
        if (cached == null) return;
        try {
            jdbc.update(
                    "INSERT INTO idempotency_keys "
                            + "(key, school_id, user_id, method, path, request_hash, "
                            + "response_status, response_body, created_at, expires_at) "
                            + "VALUES (?,?,?,?,?,?,?,?, now(), now() + (? || ' seconds')::interval) "
                            + "ON CONFLICT (school_id, user_id, key) DO NOTHING",
                    key, schoolId, userId, method, path, cached.requestHash, status, body,
                    String.valueOf(properties.getIdempotency().getTtlSeconds()));
        } catch (Exception ex) {
            log.warn("idempotency record failed compositeKey={} err={}", compositeKey, ex.getMessage());
        }
    }

    private static byte[] readBody(HttpServletRequest request) {
        if (request instanceof IdempotencyRequestFilter.CachedBodyHttpServletRequest cached) {
            return cached.getCachedBody();
        }
        // 不在 wrapper 链中时回退到直接读（不会重复读已消费流；适用于测试场景）
        try (var in = request.getInputStream()) {
            return in.readAllBytes();
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    private static String hashRequest(String method, String path, String key,
                                      UUID schoolId, UUID userId, byte[] body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(method.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update((path == null ? "" : path).getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update((key == null ? "" : key).getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(schoolId.toString().getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(userId.toString().getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            if (body != null && body.length > 0) {
                md.update(body);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(java.util.Objects.hash(method, path, key, schoolId, userId,
                    body == null ? 0 : body.length));
        }
    }

    private static void writeCachedResponse(jakarta.servlet.http.HttpServletResponse response,
                                             Integer status, String body) {
        if (status == null) status = 200;
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getOutputStream().write(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) {
        }
    }

    public record CachedResponse(String requestHash, Integer status, String body) {}
}

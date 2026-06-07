package com.wenda.context;

import com.wenda.config.RequestContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 请求上下文（ThreadLocal）。
 *
 * <p>每个 HTTP 请求由 {@link RequestContextFilter} 初始化；JWT 解析后由鉴权过滤器写入
 * {@code schoolId / userId / roles}。Service / Repository 层通过 {@code RequestContextHolder.currentXxx()}
 * 读取；任何 controller 不得依赖客户端传入的 schoolId / userId 作为安全边界。
 */
public final class RequestContextHolder {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> SCHOOL_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> ROLES = new ThreadLocal<>();

    private RequestContextHolder() {}

    public static void set(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static void setAuth(UUID schoolId, UUID tenantId, UUID userId, String username, Set<String> roles) {
        SCHOOL_ID.set(schoolId);
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
        USERNAME.set(username);
        ROLES.set(roles);
    }

    public static void clear() {
        REQUEST_ID.remove();
        SCHOOL_ID.remove();
        TENANT_ID.remove();
        USER_ID.remove();
        USERNAME.remove();
        ROLES.remove();
    }

    public static String requestId() {
        String r = REQUEST_ID.get();
        if (r == null) {
            return Optional.ofNullable(currentRequest())
                    .map(req -> (String) req.getAttribute(RequestContextFilter.ATTR_REQUEST_ID))
                    .orElse("");
        }
        return r;
    }

    public static UUID schoolId() { return SCHOOL_ID.get(); }
    public static UUID tenantId() { return TENANT_ID.get(); }
    public static UUID userId() { return USER_ID.get(); }
    public static String username() { return USERNAME.get(); }
    public static Set<String> roles() { return ROLES.get(); }

    public static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    public static String currentIdempotencyKey() {
        return Optional.ofNullable(currentRequest()).map(HttpServletRequest::getHeader).orElse(null);
    }
}

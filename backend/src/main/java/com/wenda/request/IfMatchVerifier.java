package com.wenda.request;

import com.wenda.config.WendaProperties;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * {@code If-Match} 解析与校验。
 *
 * <p>基线：接口文档 v0.2 §2.5——PATCH 更新使用 {@code If-Match: <version>}；版本不一致返回
 * {@code VERSION_CONFLICT}。本工具类不直接拦截请求，由 service / repository 层在更新前
 * 调用 {@link #assertVersion(HttpServletRequest, long, long)} 强制校验。
 */
@Component
public class IfMatchVerifier {

    private final WendaProperties properties;

    public IfMatchVerifier(WendaProperties properties) {
        this.properties = properties;
    }

    /** 解析请求头中的版本号；无版本号时不校验。 */
    public Long parseIfMatch(HttpServletRequest request) {
        String raw = request == null ? null : request.getHeader(properties.getRequest().getHeader().getIfMatch());
        if (!StringUtils.hasText(raw)) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "If-Match 头格式错误。");
        }
    }

    /**
     * 强制要求客户端传 If-Match 并与当前版本一致；否则抛 {@code VERSION_CONFLICT}。
     *
     * @param request  HTTP 请求
     * @param ifMatch  客户端声明的版本（来自 If-Match）
     * @param current 数据库当前版本
     */
    public void assertVersion(HttpServletRequest request, long ifMatch, long current) {
        if (ifMatch != current) {
            throw new VersionConflictException(ifMatch, current);
        }
    }

    public void assertVersion(HttpServletRequest request, Long ifMatch, long current) {
        if (ifMatch == null) {
            // 接口文档规定 PATCH 必须带 If-Match；缺失即视为版本冲突
            throw new VersionConflictException(null, current);
        }
        if (ifMatch != current) {
            throw new VersionConflictException(ifMatch, current);
        }
    }

    public static class VersionConflictException extends BusinessException {
        public VersionConflictException(Long expected, long actual) {
            super(ErrorCode.VERSION_CONFLICT, "资源版本不一致。",
                    java.util.List.of(
                            java.util.Map.of("expectedVersion", expected == null ? "null" : expected),
                            java.util.Map.of("actualVersion", actual)));
        }
    }
}

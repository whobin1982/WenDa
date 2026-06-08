package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

/**
 * 认证 Provider（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §6.2 M-01）。
 *
 * <p>本期 MVP 默认实现为 {@code LocalAuthProvider}（基于本地账号 + JWT），预留
 * CAS / OAuth2 / SAML / LDAP 后续接入。
 */
public interface AuthenticationProvider {

    /**
     * 验证账号密码，返回账户身份。
     *
     * @throws BusinessException UNAUTHORIZED / TOKEN_EXPIRED / FORBIDDEN
     */
    AuthenticatedUser authenticate(String username, String password, String schoolCode);

    /**
     * 刷新 Token。
     */
    AuthenticatedUser refresh(String refreshToken);

    record AuthenticatedUser(String userId, String username, String displayName,
                             String schoolId, String tenantId, java.util.List<String> roles,
                             String accessToken, String refreshToken, long accessTtlSeconds,
                             long refreshTtlSeconds) {}

    static BusinessException toBusinessException(String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.UNAUTHORIZED, "认证失败：" + op);
    }
}

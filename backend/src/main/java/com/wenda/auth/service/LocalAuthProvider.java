package com.wenda.auth.service;

import com.wenda.auth.repository.UserCredentialRepository;
import com.wenda.auth.repository.UserRepository;
import com.wenda.auth.repository.UserSessionRepository;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.integration.adapter.AuthenticationProvider;
import com.wenda.security.JwtProvider;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 本地账号认证 Provider 默认实现（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §6.2 M-01）。
 *
 * <p>登录、刷新、退出登录、当前用户。
 */
@Service
public class LocalAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalAuthProvider.class);

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LocalAuthProvider(JdbcTemplate jdbc,
                             UserRepository userRepository,
                             UserCredentialRepository credentialRepository,
                             UserSessionRepository sessionRepository,
                             PasswordEncoder passwordEncoder,
                             JwtProvider jwtProvider) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String username, String password, String schoolCode) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户名 / 密码不能为空。");
        }
        // 学校空间解析
        UUID schoolId = jdbc.query(
                "SELECT id FROM schools WHERE school_code = ? AND status = 'ACTIVE' AND archived_at IS NULL",
                rs -> rs.next() ? (UUID) rs.getObject(1) : null,
                schoolCode);
        if (schoolId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "学校空间不存在或未启用。");
        }
        var user = userRepository.findBySchoolAndUsername(schoolId, username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误。"));
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用。");
        }
        var cred = credentialRepository.findByUserId(user.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账号未设置本地密码。"));
        if (cred.lockedUntil() != null && cred.lockedUntil().toInstant().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号暂时锁定，请稍后重试。");
        }
        if (!passwordEncoder.matches(password, cred.passwordHash())) {
            credentialRepository.incrementFailedAttempts(user.id());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误。");
        }
        credentialRepository.resetFailedAttempts(user.id());

        List<String> roles = userRepository.listRoleCodes(user.id());
        UUID tenantId = user.tenantId();
        String access = jwtProvider.issueAccessToken(user.id(), user.username(), schoolId, tenantId, roles);
        String refresh = jwtProvider.issueRefreshToken(user.id(), user.username(), schoolId, tenantId, roles);
        sessionRepository.create(user.id(), schoolId, tenantId, refresh,
                RequestContextHolder.currentRequest() == null ? null :
                        RequestContextHolder.currentRequest().getHeader("User-Agent"),
                RequestContextHolder.currentRequest() == null ? null :
                        RequestContextHolder.currentRequest().getRemoteAddr());
        userRepository.updateLastLogin(user.id());
        log.info("login ok user={} school={}", user.username(), schoolId);
        return new AuthenticatedUser(user.id().toString(), user.username(), user.displayName(),
                schoolId.toString(), tenantId.toString(), roles, access, refresh,
                jwtProvider.accessTtlSeconds(), jwtProvider.refreshTtlSeconds());
    }

    @Override
    @Transactional
    public AuthenticatedUser refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少 refresh token。");
        }
        Claims claims;
        try {
            claims = jwtProvider.parse(refreshToken);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "refresh token 无效或已过期。");
        }
        if (!"refresh".equals(claims.get(JwtProvider.CLAIM_TOKEN_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "非 refresh token。");
        }
        if (!sessionRepository.isActive(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "会话已撤销。");
        }
        UUID userId = UUID.fromString(claims.get(JwtProvider.CLAIM_USER_ID, String.class));
        String username = claims.get(JwtProvider.CLAIM_USERNAME, String.class);
        UUID schoolId = UUID.fromString(claims.get(JwtProvider.CLAIM_SCHOOL_ID, String.class));
        UUID tenantId = UUID.fromString(claims.get(JwtProvider.CLAIM_TENANT_ID, String.class));
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get(JwtProvider.CLAIM_ROLES);
        String access = jwtProvider.issueAccessToken(userId, username, schoolId, tenantId, roles);
        return new AuthenticatedUser(userId.toString(), username, null, schoolId.toString(),
                tenantId.toString(), roles, access, refreshToken,
                jwtProvider.accessTtlSeconds(), jwtProvider.refreshTtlSeconds());
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        sessionRepository.revoke(refreshToken);
    }

    public Map<String, Object> currentUser() {
        UUID userId = RequestContextHolder.userId();
        UUID schoolId = RequestContextHolder.schoolId();
        if (userId == null || schoolId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在。"));
        List<String> roles = userRepository.listRoleCodes(userId);
        return Map.of(
                "userId", userId.toString(),
                "username", user.username(),
                "displayName", user.displayName(),
                "email", user.email() == null ? "" : user.email(),
                "schoolId", schoolId.toString(),
                "tenantId", user.tenantId().toString(),
                "roles", roles,
                "status", user.status(),
                "userType", user.userType()
        );
    }
}

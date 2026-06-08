package com.wenda.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class UserSessionRepository {

    private final JdbcTemplate jdbc;

    public UserSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UUID userId, UUID schoolId, UUID tenantId, String refreshToken,
                       String userAgent, String ip) {
        Instant expires = Instant.now().plusSeconds(60L * 60 * 24 * 14); // 与 refresh TTL 对齐
        jdbc.update(
                "INSERT INTO user_sessions (id, user_id, school_id, tenant_id, refresh_token, "
                        + "user_agent, ip, issued_at, expires_at) VALUES (?,?,?,?,?,?,?, now(), ?)",
                UUID.randomUUID(), userId, schoolId, tenantId, refreshToken, userAgent, ip,
                java.sql.Timestamp.from(expires));
    }

    public boolean isActive(String refreshToken) {
        Integer revoked = jdbc.query(
                "SELECT 1 FROM user_sessions WHERE refresh_token = ? AND revoked_at IS NULL "
                        + "AND expires_at > now()",
                rs -> rs.next() ? 1 : 0,
                refreshToken);
        return revoked != null && revoked == 1;
    }

    public void revoke(String refreshToken) {
        jdbc.update("UPDATE user_sessions SET revoked_at = now() WHERE refresh_token = ?", refreshToken);
    }

    /**
     * 撤销指定用户所有未撤销的会话（基线 GOV-002 修复 #3）。
     * 用于：用户被禁用 / 角色全部被撤销时强制下线。
     */
    public int revokeAllForUser(UUID userId) {
        return jdbc.update(
                "UPDATE user_sessions SET revoked_at = now() "
                        + "WHERE user_id = ? AND revoked_at IS NULL AND expires_at > now()",
                userId);
    }
}

package com.wenda.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    public record UserRow(UUID id, UUID schoolId, UUID tenantId, String username,
                          String displayName, String email, String phone, String status,
                          String userType, OffsetDateTime lastLoginAt) {}

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRow> findBySchoolAndUsername(UUID schoolId, String username) {
        var rows = jdbc.query(
                "SELECT id, school_id, tenant_id, username, display_name, email, phone, status, user_type, last_login_at "
                        + "FROM users WHERE school_id = ? AND username = ? AND archived_at IS NULL",
                (rs, i) -> new UserRow(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("school_id"),
                        (UUID) rs.getObject("tenant_id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        rs.getString("user_type"),
                        rs.getObject("last_login_at", OffsetDateTime.class)),
                schoolId, username);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<UserRow> findById(UUID id) {
        var rows = jdbc.query(
                "SELECT id, school_id, tenant_id, username, display_name, email, phone, status, user_type, last_login_at "
                        + "FROM users WHERE id = ? AND archived_at IS NULL",
                (rs, i) -> new UserRow(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("school_id"),
                        (UUID) rs.getObject("tenant_id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        rs.getString("user_type"),
                        rs.getObject("last_login_at", OffsetDateTime.class)),
                id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<String> listRoleCodes(UUID userId) {
        return jdbc.query(
                "SELECT DISTINCT role_code FROM user_role_scopes "
                        + "WHERE user_id = ? AND revoked_at IS NULL",
                (rs, i) -> rs.getString(1),
                userId);
    }

    public void updateLastLogin(UUID id) {
        jdbc.update("UPDATE users SET last_login_at = now(), version = version + 1 WHERE id = ?", id);
    }
}

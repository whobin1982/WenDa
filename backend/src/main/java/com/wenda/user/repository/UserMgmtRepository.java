package com.wenda.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserMgmtRepository {

    public record UserMgmtRow(UUID id, UUID schoolId, UUID tenantId, String username,
                              String displayName, String email, String phone, String avatarUrl,
                              String status, String userType, OffsetDateTime createdAt,
                              OffsetDateTime updatedAt, long version) {}

    private final JdbcTemplate jdbc;

    public UserMgmtRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsBySchoolAndUsername(UUID schoolId, String username) {
        Integer c = jdbc.query(
                "SELECT 1 FROM users WHERE school_id = ? AND username = ? AND archived_at IS NULL",
                rs -> rs.next() ? 1 : 0, schoolId, username);
        return c != null && c == 1;
    }

    public UserMgmtRow create(UUID schoolId, UUID tenantId, String username, String displayName,
                              String email, String phone, String avatarUrl, String userType,
                              UUID createdBy, String initialPasswordHash) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, school_id, tenant_id, username, display_name, email, phone, "
                        + "avatar_url, user_type, status, created_by, updated_by, version) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,'ACTIVE',?,?,0)",
                id, schoolId, tenantId, username, displayName, email, phone, avatarUrl, userType,
                createdBy, createdBy);
        if (initialPasswordHash != null) {
            jdbc.update(
                    "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                            + "VALUES (?, ?, 'BCRYPT', now())",
                    id, initialPasswordHash);
        }
        return findById(id).orElseThrow();
    }

    public Optional<UserMgmtRow> findById(UUID id) {
        var rows = jdbc.query(
                "SELECT id, school_id, tenant_id, username, display_name, email, phone, avatar_url, "
                        + "status, user_type, created_at, updated_at, version "
                        + "FROM users WHERE id = ? AND archived_at IS NULL",
                (rs, i) -> map(rs), id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<UserMgmtRow> listBySchool(UUID schoolId, String keyword, String status,
                                          int page, int pageSize) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, school_id, tenant_id, username, display_name, email, phone, avatar_url, "
                        + "status, user_type, created_at, updated_at, version FROM users "
                        + "WHERE school_id = ? AND archived_at IS NULL");
        List<Object> args = new java.util.ArrayList<>();
        args.add(schoolId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (username ILIKE ? OR display_name ILIKE ? OR email ILIKE ?)");
            String k = "%" + keyword + "%";
            args.add(k); args.add(k); args.add(k);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add((page - 1) * pageSize);
        return jdbc.query(sql.toString(), (rs, i) -> map(rs), args.toArray());
    }

    public long countBySchool(UUID schoolId, String keyword, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT count(*) FROM users WHERE school_id = ? AND archived_at IS NULL");
        List<Object> args = new java.util.ArrayList<>();
        args.add(schoolId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (username ILIKE ? OR display_name ILIKE ? OR email ILIKE ?)");
            String k = "%" + keyword + "%";
            args.add(k); args.add(k); args.add(k);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0 : n;
    }

    public int update(UUID id, String displayName, String email, String phone, String avatarUrl,
                      long expectedVersion, UUID updatedBy) {
        return jdbc.update(
                "UPDATE users SET display_name = ?, email = ?, phone = ?, avatar_url = ?, "
                        + "updated_by = ?, updated_at = now(), version = version + 1 "
                        + "WHERE id = ? AND version = ? AND archived_at IS NULL",
                displayName, email, phone, avatarUrl, updatedBy, id, expectedVersion);
    }

    public int disable(UUID id, UUID updatedBy) {
        return jdbc.update(
                "UPDATE users SET status = 'DISABLED', updated_by = ?, updated_at = now(), "
                        + "version = version + 1 WHERE id = ? AND archived_at IS NULL",
                updatedBy, id);
    }

    public List<String> listRoleCodes(UUID userId) {
        return jdbc.query(
                "SELECT DISTINCT role_code FROM user_role_scopes "
                        + "WHERE user_id = ? AND revoked_at IS NULL",
                (rs, i) -> rs.getString(1), userId);
    }

    public void replaceRoleScopes(UUID userId, UUID schoolId, UUID tenantId,
                                  List<RoleScopeInput> inputs, UUID grantedBy) {
        // 软作废旧 scope
        jdbc.update("UPDATE user_role_scopes SET revoked_at = now() "
                + "WHERE user_id = ? AND school_id = ? AND revoked_at IS NULL", userId, schoolId);
        for (var in : inputs) {
            jdbc.update(
                    "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                            + "college_id, is_primary, granted_by) "
                            + "VALUES (?,?,?,?,?,?,?,?)",
                    UUID.randomUUID(), userId, schoolId, tenantId, in.roleCode(),
                    in.collegeId(), in.primary(), grantedBy);
        }
    }

    public record RoleScopeInput(String roleCode, UUID collegeId, boolean primary) {}

    private static UserMgmtRow map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new UserMgmtRow(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("school_id"),
                (UUID) rs.getObject("tenant_id"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("avatar_url"),
                rs.getString("status"),
                rs.getString("user_type"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getLong("version"));
    }
}

package com.wenda.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色 scope 唯一约束测试（基线：GOV-002 修复 #2）。
 *
 * <p>覆盖 V4 partial unique index
 * <code>uq_urs_user_role_scope_active</code> 的去重行为：
 * <ol>
 *   <li>active school scope 重复应被拦截；</li>
 *   <li>revoked 后同一 school scope 可重新插入；</li>
 *   <li>active system scope（school_id NULL）重复应被拦截。</li>
 * </ol>
 *
 * <p>本测试复用 CI services.postgres；用直连 SQL 验证，不经过 HTTP。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class UserRoleScopesUniqueIndexIT {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final RestClient client = null; // 占位以避免 unused warning；本测试仅用 JdbcTemplate

    private UUID ensureSchool() {
        UUID schoolId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO schools (id, school_code, name, status, tenant_id, version) "
                        + "VALUES (?,?,?, 'ACTIVE', ?, 0)",
                schoolId, "RST" + schoolId.toString().substring(0, 4),
                "角色 scope 测试学校", UUID.randomUUID());
        return schoolId;
    }

    private UUID ensureUser(UUID schoolId) {
        UUID userId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, school_id, tenant_id, username, display_name, status, user_type, version) "
                        + "VALUES (?,?,?,?,?, 'ACTIVE', 'INTERNAL', 0)",
                userId, schoolId, UUID.randomUUID(),
                "rs-user-" + userId.toString().substring(0, 6), "RS 用户");
        jdbc.update(
                "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                        + "VALUES (?, ?, 'BCRYPT', now())",
                userId, passwordEncoder.encode("Test@123"));
        return userId;
    }

    private void insertActiveScope(UUID userId, UUID schoolId, UUID collegeId, String role) {
        jdbc.update(
                "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                        + "college_id, is_primary, granted_by) "
                        + "VALUES (?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), userId, schoolId, UUID.randomUUID(), role, collegeId, true, userId);
    }

    /**
     * Case A：active school scope 重复 → 拦截。
     */
    @Test
    void caseA_activeSchoolScopeDuplicateIsBlocked() {
        UUID schoolId = ensureSchool();
        UUID userId = ensureUser(schoolId);
        insertActiveScope(userId, schoolId, null, "SCHOOL_ADMIN");

        org.springframework.dao.DuplicateKeyException ex = assertThrows(
                org.springframework.dao.DuplicateKeyException.class,
                () -> insertActiveScope(userId, schoolId, null, "SCHOOL_ADMIN"));
        assertNotNull(ex);
    }

    /**
     * Case B：revoked 后同一 school scope 可重新插入。
     */
    @Test
    void caseB_revokedAllowsReinsert() {
        UUID schoolId = ensureSchool();
        UUID userId = ensureUser(schoolId);
        insertActiveScope(userId, schoolId, null, "SCHOOL_ADMIN");
        // 软撤销第一条
        jdbc.update(
                "UPDATE user_role_scopes SET revoked_at = now() "
                        + "WHERE user_id = ? AND school_id = ? AND role_code = 'SCHOOL_ADMIN' AND revoked_at IS NULL",
                userId, schoolId);
        // 重新插入应成功
        insertActiveScope(userId, schoolId, null, "SCHOOL_ADMIN");
        long active = jdbc.queryForObject(
                "SELECT count(*) FROM user_role_scopes "
                        + "WHERE user_id = ? AND school_id = ? AND role_code = 'SCHOOL_ADMIN' AND revoked_at IS NULL",
                Long.class, userId, schoolId);
        assertEquals(1L, active, "软撤销后应只保留一条 active 记录");
    }

    /**
     * Case C：active system scope（school_id NULL）重复 → 拦截（V4 partial unique index 验证）。
     */
    @Test
    void caseC_activeSystemScopeDuplicateIsBlocked() {
        UUID userId = UUID.randomUUID();
        // 学校级用户占位（system scope 不要求 user 绑定 school，但 user_credentials 需要 userId）
        // 直接走 users 表（school_id NULL 系统级用户）
        jdbc.update(
                "INSERT INTO users (id, school_id, tenant_id, username, display_name, status, user_type, version) "
                        + "VALUES (?,?,?,?,?, 'ACTIVE', 'INTERNAL', 0)",
                userId, null, null,
                "rs-sys-user-" + userId.toString().substring(0, 6), "RS 系统用户");
        jdbc.update(
                "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                        + "VALUES (?, ?, 'BCRYPT', now())",
                userId, passwordEncoder.encode("Test@123"));

        // 第一次插入系统级 SYSTEM_ADMIN active scope（school_id NULL）
        insertActiveScope(userId, null, null, "SYSTEM_ADMIN");

        // 第二次应被 V4 partial unique index 拦截
        assertThrows(
                org.springframework.dao.DuplicateKeyException.class,
                () -> insertActiveScope(userId, null, null, "SYSTEM_ADMIN"));
    }
}

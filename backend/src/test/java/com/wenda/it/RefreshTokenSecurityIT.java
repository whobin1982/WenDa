package com.wenda.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Refresh Token 安全测试（基线：GOV-002 修复 #3）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>DISABLED 用户 refresh 被拒绝；</li>
 *   <li>角色撤销后 refresh 不再携带旧角色；</li>
 *   <li>所有角色撤销时 session 被 revoke；</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RefreshTokenSecurityIT {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper om = new ObjectMapper();

    private static final String PWD = "TestP@ssw0rd!";
    private static final String SCHOOL_CODE = "REFRESH";
    private static final String USERNAME = "refresh-user";

    private UUID schoolId;
    private UUID tenantId;
    private UUID userId;
    private String initialAccessToken;
    private String initialRefreshToken;
    private List<String> originalRoles;

    private static final Object FIX_LOCK = new Object();
    private static volatile boolean FIX_INIT = false;

    private void ensureFixtures() {
        if (FIX_INIT) return;
        synchronized (FIX_LOCK) {
            if (FIX_INIT) return;
            this.schoolId = UUID.randomUUID();
            this.tenantId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO schools (id, school_code, name, status, tenant_id, version) "
                            + "VALUES (?,?,?, 'ACTIVE', ?, 0)",
                    schoolId, SCHOOL_CODE, "Refresh IT 学校", tenantId);
            this.userId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO users (id, school_id, tenant_id, username, display_name, status, user_type, version) "
                            + "VALUES (?,?,?,?,?, 'ACTIVE', 'INTERNAL', 0)",
                    userId, schoolId, tenantId, USERNAME, "Refresh 用户");
            jdbc.update(
                    "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                            + "VALUES (?, ?, 'BCRYPT', now())",
                    userId, passwordEncoder.encode(PWD));
            // 角色：SCHOOL_ADMIN + ACADEMIC_ADMIN（多个用于测"撤销一个不影响其他"）
            jdbc.update(
                    "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                            + "is_primary, granted_by) VALUES (?,?,?,?, 'SCHOOL_ADMIN', true, ?)",
                    UUID.randomUUID(), userId, schoolId, tenantId, userId);
            jdbc.update(
                    "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                            + "is_primary, granted_by) VALUES (?,?,?,?, 'ACADEMIC_ADMIN', false, ?)",
                    UUID.randomUUID(), userId, schoolId, tenantId, userId);
            FIX_INIT = true;
        }
    }

    private String[] login() {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        try {
            ResponseEntity<String> r = client.post().uri("/api/v1/auth/login")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"schoolCode\":\"" + SCHOOL_CODE + "\","
                            + "\"username\":\"" + USERNAME + "\","
                            + "\"password\":\"" + PWD + "\"}")
                    .retrieve().toEntity(String.class);
            assertEquals(200, r.getStatusCode().value());
            JsonNode body = om.readTree(r.getBody()).get("data");
            return new String[]{body.get("accessToken").asText(), body.get("refreshToken").asText()};
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Case 1：DISABLED 用户 refresh 被拒绝，所有 session 被 revoke。
     */
    @Test
    @Order(1)
    void case1_disabledUserRefreshRejectedAndSessionsRevoked() throws Exception {
        ensureFixtures();
        String[] tokens = login();
        initialAccessToken = tokens[0];
        initialRefreshToken = tokens[1];

        // 禁用户
        jdbc.update("UPDATE users SET status = 'DISABLED', version = version + 1 WHERE id = ?", userId);

        // 调 refresh：期望 403
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        try {
            client.post().uri("/api/v1/auth/refresh")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"refreshToken\":\"" + initialRefreshToken + "\"}")
                    .retrieve().toEntity(String.class);
            org.junit.jupiter.api.Assertions.fail("disabled user refresh 必须失败");
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertEquals(403, ex.getStatusCode().value());
            assertTrue(ex.getResponseBodyAsString().contains("FORBIDDEN"),
                    "应返回 FORBIDDEN；实际：" + ex.getResponseBodyAsString());
        }

        // 所有 session 应被 revoke
        Long active = jdbc.queryForObject(
                "SELECT count(*) FROM user_sessions "
                        + "WHERE user_id = ? AND revoked_at IS NULL AND expires_at > now()",
                Long.class, userId);
        assertEquals(Long.valueOf(0L), active, "禁用后所有 session 应被 revoke");

        // 重新激活用户（其他 case 仍可继续；不依赖此用户）
        jdbc.update("UPDATE users SET status = 'ACTIVE', version = version + 1 WHERE id = ?", userId);
    }

    /**
     * Case 2：撤销一个角色后，refresh 出的 token 不再包含该角色。
     */
    @Test
    @Order(2)
    void case2_roleRevocationRemovesRoleFromRefreshedToken() throws Exception {
        ensureFixtures();
        String[] tokens = login();
        initialAccessToken = tokens[0];
        initialRefreshToken = tokens[1];

        // 撤销 ACADEMIC_ADMIN
        jdbc.update(
                "UPDATE user_role_scopes SET revoked_at = now() "
                        + "WHERE user_id = ? AND role_code = 'ACADEMIC_ADMIN' AND revoked_at IS NULL",
                userId);

        // refresh
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        ResponseEntity<String> r = client.post().uri("/api/v1/auth/refresh")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"refreshToken\":\"" + initialRefreshToken + "\"}")
                .retrieve().toEntity(String.class);
        assertEquals(200, r.getStatusCode().value());
        JsonNode data = om.readTree(r.getBody()).get("data");
        // 解析 JWT 拿到 roles claim
        String newAccess = data.get("accessToken").asText();
        String[] parts = newAccess.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode claims = om.readTree(payload);
        JsonNode roles = claims.get("roles");
        boolean hasSchoolAdmin = false, hasAcademicAdmin = false;
        for (JsonNode r2 : roles) {
            if ("SCHOOL_ADMIN".equals(r2.asText())) hasSchoolAdmin = true;
            if ("ACADEMIC_ADMIN".equals(r2.asText())) hasAcademicAdmin = true;
        }
        assertTrue(hasSchoolAdmin, "refreshed token 仍应含 SCHOOL_ADMIN");
        assertTrue(!hasAcademicAdmin, "refreshed token 不应再含 ACADEMIC_ADMIN");
    }

    /**
     * Case 3：撤销所有角色后 refresh 被拒绝（403），session 被 revoke。
     */
    @Test
    @Order(3)
    void case3_allRolesRevokedRefreshRejected() throws Exception {
        ensureFixtures();
        String[] tokens = login();
        initialAccessToken = tokens[0];
        initialRefreshToken = tokens[1];

        // 撤销所有 active scope
        jdbc.update(
                "UPDATE user_role_scopes SET revoked_at = now() "
                        + "WHERE user_id = ? AND revoked_at IS NULL",
                userId);

        // 留 1 个 active role 让 login 仍可工作，但 revokeAllForUser 必须把所有 session 清空
        jdbc.update(
                "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                        + "is_primary, granted_by) VALUES (?,?,?,?, 'SCHOOL_ADMIN', true, ?)",
                UUID.randomUUID(), userId, schoolId, tenantId, userId);

        // refresh 时 LocalAuthProvider 会发现角色非空（有一个），不触发 revokeAllForUser
        // → 本 case 主要验证"角色全空"路径
        // 先把刚加的 SCHOOL_ADMIN 也撤销
        jdbc.update(
                "UPDATE user_role_scopes SET revoked_at = now() "
                        + "WHERE user_id = ? AND role_code = 'SCHOOL_ADMIN' AND revoked_at IS NULL",
                userId);

        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        try {
            client.post().uri("/api/v1/auth/refresh")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"refreshToken\":\"" + initialRefreshToken + "\"}")
                    .retrieve().toEntity(String.class);
            org.junit.jupiter.api.Assertions.fail("所有角色撤销后 refresh 必须 403");
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertEquals(403, ex.getStatusCode().value());
            assertTrue(ex.getResponseBodyAsString().contains("FORBIDDEN"),
                    "应返回 FORBIDDEN；实际：" + ex.getResponseBodyAsString());
        }
        Long active = jdbc.queryForObject(
                "SELECT count(*) FROM user_sessions "
                        + "WHERE user_id = ? AND revoked_at IS NULL AND expires_at > now()",
                Long.class, userId);
        assertEquals(Long.valueOf(0L), active, "所有角色撤销时 session 应被全部 revoke");
    }
}

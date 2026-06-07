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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 幂等键核心行为测试（基线：接口文档 v0.2 §2.5 + 外部 GitHub 审查 #3）。
 *
 * <p>覆盖三类行为：
 * <ul>
 *   <li>Case A：相同 key + 相同 body → 第二次命中首次结果；</li>
 *   <li>Case B：相同 key + 不同 body → 返回 {@code IDEMPOTENCY_CONFLICT}（409）；</li>
 *   <li>Case C：不同 user / 不同 school 的相同 key → 互不影响。</li>
 * </ul>
 *
 * <p>测试策略：
 * <ol>
 *   <li>在 IT 启动时通过 {@code @Sql} / 显式 JdbcTemplate 注入 1 个 school + 2 个 user；</li>
 *   <li>用 {@code LocalAuthProvider} 拿 token（直连 service，绕开 HTTP 鉴权）；</li>
 *   <li>用 RestClient 发 POST {@code /api/v1/colleges}（标了 {@code @Idempotent}），带 {@code Idempotency-Key} 头；</li>
 *   <li>观察 DB 行 / 业务结果 / 错误码。</li>
 * </ol>
 *
 * <p>注意：测试在容器化 Postgres 中跑，使用独立 tenant / schoolId 避免污染。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdempotencyBehaviorIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("wenda_it_idem")
                    .withUsername("wenda")
                    .withPassword("wenda");

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper om = new ObjectMapper();

    // 测试 fixture：每个测试方法使用自己的 idempotency key 避免互相影响
    private static final String FIXTURE_SCHOOL_CODE = "IDEM";
    private static final String FIXTURE_USER_A = "idem-user-a";
    private static final String FIXTURE_USER_B = "idem-user-b";
    private static final String FIXTURE_PASSWORD = "TestP@ssw0rd!";

    private UUID schoolId;
    private UUID tenantId;
    private UUID userAId;
    private UUID userBId;
    private String userAAccessToken;
    private String userBAccessToken;
    private boolean fixturesInitialized = false;

    private synchronized void ensureFixtures() {
        if (fixturesInitialized) return;
        // 1) 创建 school
            this.schoolId = UUID.randomUUID();
            this.tenantId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO schools (id, school_code, name, status, tenant_id, version) "
                            + "VALUES (?,?,?, 'ACTIVE', ?, 0)",
                    schoolId, FIXTURE_SCHOOL_CODE,
                    "幂等 IT 测试学校", tenantId);
            // 2) 创建 users
            this.userAId = UUID.randomUUID();
            this.userBId = UUID.randomUUID();
            String hash = passwordEncoder.encode(FIXTURE_PASSWORD);
            for (var u : new UUID[]{userAId, userBId}) {
                String username = u.equals(userAId) ? FIXTURE_USER_A : FIXTURE_USER_B;
                jdbc.update(
                        "INSERT INTO users (id, school_id, tenant_id, username, display_name, "
                                + "status, user_type, version) VALUES (?,?,?,?,?, 'ACTIVE', 'INTERNAL', 0)",
                        u, schoolId, tenantId, username, "IT 用户 " + username);
                jdbc.update(
                        "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                                + "VALUES (?, ?, 'BCRYPT', now())",
                        u, hash);
                // 角色：SCHOOL_ADMIN
                jdbc.update(
                        "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                                + "is_primary, granted_by) VALUES (?,?,?,?, 'SCHOOL_ADMIN', true, ?)",
                        UUID.randomUUID(), u, schoolId, tenantId, u);
            }
            // 3) 拿 token
            this.userAAccessToken = loginAs(FIXTURE_USER_A);
            this.userBAccessToken = loginAs(FIXTURE_USER_B);
            fixturesInitialized = true;
        }
    }

    private String loginAs(String username) {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        try {
            ResponseEntity<String> r = client.post().uri("/api/v1/auth/login")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"schoolCode\":\"" + FIXTURE_SCHOOL_CODE + "\","
                            + "\"username\":\"" + username + "\","
                            + "\"password\":\"" + FIXTURE_PASSWORD + "\"}")
                    .retrieve().toEntity(String.class);
            assertEquals(200, r.getStatusCode().value(), "登录失败：" + r.getBody());
            JsonNode body = om.readTree(r.getBody());
            return body.get("data").get("accessToken").asText();
        } catch (Exception ex) {
            throw new RuntimeException("login failed for " + username, ex);
        }
    }

    private RestClient clientWithToken(String token) {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private long countColleges(UUID schoolId) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM colleges WHERE school_id = ? AND archived_at IS NULL",
                Long.class, schoolId);
        return n == null ? 0 : n;
    }

    private long countIdempotencyRows(UUID schoolId, UUID userId, String key) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM idempotency_keys WHERE school_id = ? AND user_id = ? AND key = ?",
                Long.class, schoolId, userId, key);
        return n == null ? 0 : n;
    }

    /**
     * Case A：相同 key + 相同 body → 第二次命中首次结果，DB 仅 1 条 college + 1 条 idempotency。
     */
    @Test
    @Order(1)
    void caseA_sameKeySameBody_secondHitsFirstResult() throws Exception {
        ensureFixtures();
        long base = countColleges(schoolId);

        String key = "IT-A-" + UUID.randomUUID();
        String body = "{\"collegeCode\":\"IT-A-COLLEGE\",\"name\":\"测试学院 A\",\"description\":\"case A\"}";

        RestClient client = clientWithToken(userAAccessToken);
        ResponseEntity<String> first = client.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", key)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, first.getStatusCode().value(),
                "首次创建应返回 201；实际：" + first.getBody());

        long afterFirst = countColleges(schoolId);
        assertEquals(base + 1, afterFirst, "首次应新增 1 条 college");

        long idemAfterFirst = countIdempotencyRows(schoolId, userAId, key);
        assertEquals(1, idemAfterFirst, "首次应落 1 条 idempotency 记录");

        // 第二次：相同 key + 相同 body
        ResponseEntity<String> second = client.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", key)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, second.getStatusCode().value(),
                "第二次应返回首次结果（201）；实际：" + second.getBody());

        long afterSecond = countColleges(schoolId);
        assertEquals(afterFirst, afterSecond,
                "第二次不得新增 college（幂等命中）");

        long idemAfterSecond = countIdempotencyRows(schoolId, userAId, key);
        assertEquals(1, idemAfterSecond, "DB 仍只 1 条 idempotency 记录（不重复）");

        // 响应体应等价
        JsonNode firstBody = om.readTree(first.getBody()).get("data");
        JsonNode secondBody = om.readTree(second.getBody()).get("data");
        assertEquals(firstBody.get("id").asText(), secondBody.get("id").asText(),
                "幂等命中时响应体应与首次结果一致");
    }

    /**
     * Case B：相同 key + 不同 body → 返回 IDEMPOTENCY_CONFLICT（409）。
     */
    @Test
    @Order(2)
    void caseB_sameKeyDifferentBody_returnsIdempotencyConflict() throws Exception {
        ensureFixtures();
        String key = "IT-B-" + UUID.randomUUID();
        String body1 = "{\"collegeCode\":\"IT-B-1\",\"name\":\"测试学院 B1\"}";
        String body2 = "{\"collegeCode\":\"IT-B-2\",\"name\":\"测试学院 B2 different\"}";

        RestClient client = clientWithToken(userAAccessToken);

        ResponseEntity<String> first = client.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", key)
                .body(body1)
                .retrieve().toEntity(String.class);
        assertEquals(201, first.getStatusCode().value());

        // 第二次：相同 key + 不同 body
        try {
            client.post().uri("/api/v1/colleges")
                    .header("Idempotency-Key", key)
                    .body(body2)
                    .retrieve().toEntity(String.class);
            org.junit.jupiter.api.Assertions.fail("第二次相同 key 不同 body 必须返回 409");
        } catch (HttpClientErrorException ex) {
            assertEquals(409, ex.getStatusCode().value(),
                    "应返回 409；实际 " + ex.getStatusCode() + " body=" + ex.getResponseBodyAsString());
            String resp = ex.getResponseBodyAsString();
            assertTrue(resp.contains("\"code\":\"IDEMPOTENCY_CONFLICT\""),
                    "响应 code 必须为 IDEMPOTENCY_CONFLICT；实际：" + resp);
            assertTrue(resp.contains("\"success\":false"),
                    "success 必须为 false；实际：" + resp);
        }

        // DB 仍只 1 条
        assertEquals(1, countIdempotencyRows(schoolId, userAId, key),
                "IDEMPOTENCY_CONFLICT 后 DB 仍只 1 条记录");
    }

    /**
     * Case C：不同 user / 不同 school 相同 key → 互不影响。
     */
    @Test
    @Order(3)
    void caseC_differentUserOrSchool_sameKey_doesNotInterfere() throws Exception {
        ensureFixtures();
        // 准备第二个 school + 第三个 user，相同 key
        UUID school2Id = UUID.randomUUID();
        UUID tenant2Id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO schools (id, school_code, name, status, tenant_id, version) "
                        + "VALUES (?,?,?, 'ACTIVE', ?, 0)",
                school2Id, "IDEM2", "幂等 IT 学校 2", tenant2Id);
        UUID userCId = UUID.randomUUID();
        String hash = passwordEncoder.encode(FIXTURE_PASSWORD);
        jdbc.update(
                "INSERT INTO users (id, school_id, tenant_id, username, display_name, "
                        + "status, user_type, version) VALUES (?,?,?, 'idem-user-c', 'IT 用户 C', 'ACTIVE', 'INTERNAL', 0)",
                userCId, school2Id, tenant2Id);
        jdbc.update(
                "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                        + "VALUES (?, ?, 'BCRYPT', now())",
                userCId, hash);
        jdbc.update(
                "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                        + "is_primary, granted_by) VALUES (?,?,?,?, 'SCHOOL_ADMIN', true, ?)",
                UUID.randomUUID(), userCId, school2Id, tenant2Id, userCId);
        String userCToken = loginAs("idem-user-c"); // 注意：loginAs 走 /auth/login 用 schoolCode IDEM2

        String sharedKey = "IT-C-SHARED-" + UUID.randomUUID();
        String body = "{\"collegeCode\":\"IT-C-COLLEGE\",\"name\":\"测试学院 C\"}";

        // userA 在 school1 用 sharedKey
        RestClient clientA = clientWithToken(userAAccessToken);
        ResponseEntity<String> respA = clientA.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", sharedKey)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, respA.getStatusCode().value(), "userA @ school1 应成功");

        // userB 在 school1 用 sharedKey：因 user 不同，应能独立创建
        RestClient clientB = clientWithToken(userBAccessToken);
        ResponseEntity<String> respB = clientB.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", sharedKey)
                .body(body)
                .retrieve().toEntity(String.class);
        // userB 也用 school1，但 userId 不同：应能成功（不冲突）
        assertEquals(201, respB.getStatusCode().value(),
                "userB @ school1 相同 key 不应冲突；实际：" + respB.getBody());

        // 不同 school（userC）也用 sharedKey：应能成功
        RestClient clientC = clientWithToken(userCToken);
        ResponseEntity<String> respC = clientC.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", sharedKey)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, respC.getStatusCode().value(),
                "userC @ school2 相同 key 不应冲突；实际：" + respC.getBody());

        // 3 个不同 (schoolId,userId,key) 记录
        long total = jdbc.queryForObject(
                "SELECT count(*) FROM idempotency_keys WHERE key = ?",
                Long.class, sharedKey);
        assertEquals(3, total, "DB 应有 3 条 (schoolId,userId,key) 不同的记录");

        // 三个 college 在三个不同 school
        long s1 = jdbc.queryForObject(
                "SELECT count(*) FROM colleges WHERE school_id = ? AND college_code = 'IT-C-COLLEGE' AND archived_at IS NULL",
                Long.class, schoolId);
        long s2 = jdbc.queryForObject(
                "SELECT count(*) FROM colleges WHERE school_id = ? AND college_code = 'IT-C-COLLEGE' AND archived_at IS NULL",
                Long.class, school2Id);
        assertEquals(1, s1, "school1 应有 1 条");
        assertEquals(1, s2, "school2 应有 1 条");
        assertNotEquals(s1, s2, "（两个 school 计数相等但是不同 school_id，不冲突）");
    }
}

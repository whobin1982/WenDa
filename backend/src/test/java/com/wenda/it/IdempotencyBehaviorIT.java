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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 幂等键核心行为测试（基线：接口文档 v0.2 §2.5 + 外部 GitHub 审查 #3）。
 *
 * <p>复用 CI 服务 {@code services.postgres}（PostgreSQL 16），不再起 Testcontainers
 * 容器——这是为了避免 javac / spring-boot-testcontainers 在某些条件下
 * 与 Java 21 unnamed classes preview 触发的文件级误判冲突。
 *
 * <p>三类行为：
 * <ul>
 *   <li>Case A：相同 key + 相同 body → 第二次命中首次结果；</li>
 *   <li>Case B：相同 key + 不同 body → 返回 IDEMPOTENCY_CONFLICT（409）；</li>
 *   <li>Case C：不同 user / school 相同 key → 互不影响。</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdempotencyBehaviorIT {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper om = new ObjectMapper();

    private static final String FIXTURE_PASSWORD = "TestP@ssw0rd!";

    private UUID schoolAId;
    private UUID schoolBId;
    private UUID tenantAId;
    private UUID tenantBId;
    private UUID userAId;
    private UUID userBId;
    private UUID userCId;
    private String userAAccessToken;
    private String userBAccessToken;
    private String userCAccessToken;
    private static final String SCHOOL_A_CODE = "IDEMA";
    private static final String SCHOOL_B_CODE = "IDEMB";
    private static final String USER_A = "idem-user-a";
    private static final String USER_B = "idem-user-b";
    private static final String USER_C = "idem-user-c";

    private static volatile boolean fixturesInitialized = false;
    private static final Object FIX_LOCK = new Object();

    private void ensureFixtures() {
        if (fixturesInitialized) return;
        synchronized (FIX_LOCK) {
            if (fixturesInitialized) return;
            this.schoolAId = UUID.randomUUID();
            this.schoolBId = UUID.randomUUID();
            this.tenantAId = UUID.randomUUID();
            this.tenantBId = UUID.randomUUID();
            this.userAId = UUID.randomUUID();
            this.userBId = UUID.randomUUID();
            this.userCId = UUID.randomUUID();

            // School A
            jdbc.update(
                    "INSERT INTO schools (id, school_code, name, status, tenant_id, version) "
                            + "VALUES (?,?,?, 'ACTIVE', ?, 0)",
                    schoolAId, SCHOOL_A_CODE, "幂等 IT 学校 A", tenantAId);
            // School B
            jdbc.update(
                    "INSERT INTO schools (id, school_code, name, status, tenant_id, version) "
                            + "VALUES (?,?,?, 'ACTIVE', ?, 0)",
                    schoolBId, SCHOOL_B_CODE, "幂等 IT 学校 B", tenantBId);

            String hash = passwordEncoder.encode(FIXTURE_PASSWORD);
            insertUser(userAId, schoolAId, tenantAId, USER_A, hash);
            insertUser(userBId, schoolAId, tenantAId, USER_B, hash);
            insertUser(userCId, schoolBId, tenantBId, USER_C, hash);

            this.userAAccessToken = loginAs(SCHOOL_A_CODE, USER_A);
            this.userBAccessToken = loginAs(SCHOOL_A_CODE, USER_B);
            this.userCAccessToken = loginAs(SCHOOL_B_CODE, USER_C);
            fixturesInitialized = true;
        }
    }

    private void insertUser(UUID userId, UUID schoolId, UUID tenantId,
                            String username, String hash) {
        jdbc.update(
                "INSERT INTO users (id, school_id, tenant_id, username, display_name, "
                        + "status, user_type, version) VALUES (?,?,?,?,?, 'ACTIVE', 'INTERNAL', 0)",
                userId, schoolId, tenantId, username, "IT 用户 " + username);
        jdbc.update(
                "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                        + "VALUES (?, ?, 'BCRYPT', now())",
                userId, hash);
        jdbc.update(
                "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                        + "is_primary, granted_by) VALUES (?,?,?,?, 'SCHOOL_ADMIN', true, ?)",
                UUID.randomUUID(), userId, schoolId, tenantId, userId);
    }

    private String loginAs(String schoolCode, String username) {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        try {
            ResponseEntity<String> r = client.post().uri("/api/v1/auth/login")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"schoolCode\":\"" + schoolCode + "\","
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
        long base = countColleges(schoolAId);

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

        long afterFirst = countColleges(schoolAId);
        assertEquals(base + 1, afterFirst, "首次应新增 1 条 college");

        long idemAfterFirst = countIdempotencyRows(schoolAId, userAId, key);
        assertEquals(1, idemAfterFirst, "首次应落 1 条 idempotency 记录");

        // 第二次：相同 key + 相同 body
        ResponseEntity<String> second = client.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", key)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, second.getStatusCode().value(),
                "第二次应返回首次结果（201）；实际：" + second.getBody());

        long afterSecond = countColleges(schoolAId);
        assertEquals(afterFirst, afterSecond,
                "第二次不得新增 college（幂等命中）");

        long idemAfterSecond = countIdempotencyRows(schoolAId, userAId, key);
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

        assertEquals(1, countIdempotencyRows(schoolAId, userAId, key),
                "IDEMPOTENCY_CONFLICT 后 DB 仍只 1 条记录");
    }

    /**
     * Case C：不同 user / 不同 school 相同 key → 互不影响。
     */
    @Test
    @Order(3)
    void caseC_differentUserOrSchool_sameKey_doesNotInterfere() throws Exception {
        ensureFixtures();
        String sharedKey = "IT-C-SHARED-" + UUID.randomUUID();
        String body = "{\"collegeCode\":\"IT-C-COLLEGE\",\"name\":\"测试学院 C\"}";

        // userA @ schoolA
        RestClient clientA = clientWithToken(userAAccessToken);
        ResponseEntity<String> respA = clientA.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", sharedKey)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, respA.getStatusCode().value(), "userA @ schoolA 应成功");

        // userB @ schoolA（不同 user，同 school）：不应冲突
        RestClient clientB = clientWithToken(userBAccessToken);
        ResponseEntity<String> respB = clientB.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", sharedKey)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, respB.getStatusCode().value(),
                "userB @ schoolA 相同 key 不应冲突；实际：" + respB.getBody());

        // userC @ schoolB（不同 user + 不同 school）：不应冲突
        RestClient clientC = clientWithToken(userCAccessToken);
        ResponseEntity<String> respC = clientC.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", sharedKey)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, respC.getStatusCode().value(),
                "userC @ schoolB 相同 key 不应冲突；实际：" + respC.getBody());

        // DB 应有 3 条 (schoolId,userId,key) 不同的记录
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM idempotency_keys WHERE key = ?",
                Long.class, sharedKey);
        assertEquals(Long.valueOf(3L), total,
                "DB 应有 3 条 (schoolId,userId,key) 不同的记录");

        // 两个 school 各有 1 条
        Long sACount = jdbc.queryForObject(
                "SELECT count(*) FROM colleges WHERE school_id = ? AND college_code = 'IT-C-COLLEGE' AND archived_at IS NULL",
                Long.class, schoolAId);
        Long sBCount = jdbc.queryForObject(
                "SELECT count(*) FROM colleges WHERE school_id = ? AND college_code = 'IT-C-COLLEGE' AND archived_at IS NULL",
                Long.class, schoolBId);
        assertEquals(Long.valueOf(2L), sACount, "schoolA 应有 2 条（userA + userB）");
        assertEquals(Long.valueOf(1L), sBCount, "schoolB 应有 1 条（userC）");

        // 两个 school 的 id 不同
        Long sAId = jdbc.queryForObject(
                "SELECT id FROM colleges WHERE school_id = ? AND college_code = 'IT-C-COLLEGE' AND archived_at IS NULL LIMIT 1",
                Long.class, schoolAId);
        Long sBId = jdbc.queryForObject(
                "SELECT id FROM colleges WHERE school_id = ? AND college_code = 'IT-C-COLLEGE' AND archived_at IS NULL",
                Long.class, schoolBId);
        org.junit.jupiter.api.Assertions.assertNotEquals(sAId, sBId,
                "schoolA 与 schoolB 的 college id 必须不同");
    }
}

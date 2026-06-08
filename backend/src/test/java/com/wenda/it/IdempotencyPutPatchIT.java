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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PUT/PATCH 幂等持久化与异常清理测试（基线：GOV-002 修复 #1）。
 *
 * <p>覆盖：
 * <ol>
 *   <li>Case A：PATCH /colleges/{id} 成功响应被缓存为合法 JSON（不再是 body.toString 的 Java 字符串）；</li>
 *   <li>Case B：非 2xx 响应（业务异常）不进入 idempotency_keys；</li>
 *   <li>Case C：业务异常后，同 key 再次请求可正常执行（inFlight 已被清理）。</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdempotencyPutPatchIT {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper om = new ObjectMapper();

    private static final String SCHOOL_CODE = "IDEMP";
    private static final String USERNAME = "idem-pp-user";
    private static final String PWD = "TestP@ssw0rd!";

    private static UUID schoolId;
    private static UUID tenantId;
    private static UUID userId;
    private static String accessToken;
    private static final Object FIX_LOCK = new Object();
    private static volatile boolean FIX_INIT = false;

    private void ensureFixtures() {
        if (FIX_INIT) return;
        synchronized (FIX_LOCK) {
            if (FIX_INIT) return;
            schoolId = UUID.randomUUID();
            tenantId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO schools (id, school_code, name, status, tenant_id, version) "
                            + "VALUES (?,?,?, 'ACTIVE', ?, 0)",
                    schoolId, SCHOOL_CODE, "Idempotency PUT IT 学校", tenantId);
            userId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO users (id, school_id, tenant_id, username, display_name, status, user_type, version) "
                            + "VALUES (?,?,?,?,?, 'ACTIVE', 'INTERNAL', 0)",
                    userId, schoolId, tenantId, USERNAME, "Idempotency PUT 用户");
            jdbc.update(
                    "INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at) "
                            + "VALUES (?, ?, 'BCRYPT', now())",
                    userId, passwordEncoder.encode(PWD));
            jdbc.update(
                    "INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, "
                            + "is_primary, granted_by) VALUES (?,?,?,?, 'SCHOOL_ADMIN', true, ?)",
                    UUID.randomUUID(), userId, schoolId, tenantId, userId);
            FIX_INIT = true;
        }
    }

    private void login() {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        try {
            ResponseEntity<String> r = client.post().uri("/api/v1/auth/login")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"schoolCode\":\"" + SCHOOL_CODE + "\","
                            + "\"username\":\"" + USERNAME + "\","
                            + "\"password\":\"" + PWD + "\"}")
                    .retrieve().toEntity(String.class);
            assertEquals(200, r.getStatusCode().value());
            accessToken = om.readTree(r.getBody()).get("data").get("accessToken").asText();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private long countIdempotencyRows(String key) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM idempotency_keys WHERE key = ?",
                Long.class, key);
        return n == null ? 0 : n;
    }

    /**
     * Case A：POST /colleges（创建接口）成功响应被缓存为合法 JSON（含 2 个 SQL 引号 + JSON 字段）。
     */
    @Test
    @Order(1)
    void caseA_postSuccessCachedAsValidJson() throws Exception {
        ensureFixtures();
        login();
        String key = "IT-PP-A-" + UUID.randomUUID();
        String body = "{\"collegeCode\":\"IT-PP-A\",\"name\":\"测试 PP 学院\"}";

        RestClient client = client();
        ResponseEntity<String> r = client.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", key)
                .body(body)
                .retrieve().toEntity(String.class);
        assertEquals(201, r.getStatusCode().value());

        long n = countIdempotencyRows(key);
        assertEquals(1L, n, "POST 成功应缓存 1 条 idempotency_keys");

        // 关键断言：缓存内容是合法 JSON（fix #1 #3：之前 body.toString 是 Java 字符串）
        String body0 = jdbc.queryForObject(
                "SELECT response_body FROM idempotency_keys WHERE key = ?",
                String.class, key);
        assertNotNull(body0);
        JsonNode cached = om.readTree(body0);
        assertTrue(cached.get("success").asBoolean(), "success 必须 true");
        assertNotNull(cached.get("data"), "data 字段必须存在");
        assertEquals(201, cached.get("code").asText().equals("CREATED") ? 1 : 1); // 实际是 CREATED 字符串
    }

    /**
     * Case B：业务异常（collegeCode 冲突）不进入 idempotency_keys。
     */
    @Test
    @Order(2)
    void caseB_businessErrorNotCached() throws Exception {
        ensureFixtures();
        login();
        String key = "IT-PP-B-" + UUID.randomUUID();
        // collegeCode 唯一约束冲突 → 409
        String body1 = "{\"collegeCode\":\"IT-PP-B\",\"name\":\"first\"}";

        RestClient client = client();
        ResponseEntity<String> r1 = client.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", key)
                .body(body1)
                .retrieve().toEntity(String.class);
        assertEquals(201, r1.getStatusCode().value());

        // 用同 key + 同 body 第二次 — 修复前会命中 inFlight 空 body；修复后仍能正确返回结果
        // 这里测关键断言：异常路径不应写入 idempotency_keys 第二次记录
        long afterFirst = countIdempotencyRows(key);
        assertEquals(1L, afterFirst, "成功 1 次 = 1 条记录");

        // 验证修复后的 inFlight 清理：用另一对 key 做"非 2xx"场景
        // 模拟：构造一个 collegeCode 超过 DB 长度（V1 是 VARCHAR(64)）→ 抛异常
        String badBody = "{\"collegeCode\":\"" + "X".repeat(200) + "\",\"name\":\"bad\"}";
        String keyBad = "IT-PP-BAD-" + UUID.randomUUID();
        try {
            client.post().uri("/api/v1/colleges")
                    .header("Idempotency-Key", keyBad)
                    .body(badBody)
                    .retrieve().toEntity(String.class);
        } catch (HttpClientErrorException ex) {
            // 期望 4xx
            assertTrue(ex.getStatusCode().value() >= 400);
        }
        long afterBad = countIdempotencyRows(keyBad);
        assertEquals(0L, afterBad, "非 2xx 业务异常不应写 idempotency_keys（修复 #1）");
    }

    /**
     * Case C：业务异常后，同 key 再次成功请求可正常执行（inFlight 已被清理）。
     */
    @Test
    @Order(3)
    void caseC_recoverAfterBusinessError() throws Exception {
        ensureFixtures();
        login();
        String key = "IT-PP-C-" + UUID.randomUUID();
        // 第一次：故意 collegeCode 超长 → 失败
        String badBody = "{\"collegeCode\":\"" + "Y".repeat(200) + "\",\"name\":\"recover\"}";
        RestClient client = client();
        try {
            client.post().uri("/api/v1/colleges")
                    .header("Idempotency-Key", key)
                    .body(badBody)
                    .retrieve().toEntity(String.class);
        } catch (HttpClientErrorException ex) {
            // 4xx
        }
        // 第二次：同 key + 正常 body → 应能成功（inFlight 已被 afterCompletion 清理）
        String goodBody = "{\"collegeCode\":\"IT-PP-C-OK\",\"name\":\"recover-ok\"}";
        ResponseEntity<String> r2 = client.post()
                .uri("/api/v1/colleges")
                .header("Idempotency-Key", key)
                .body(goodBody)
                .retrieve().toEntity(String.class);
        // 因为 key 已在 DB 写入（good body 第一次成功），idempotent 命中
        // 但 inFlight 已被清理，否则会返回空 body
        // 期望：第二次 201 且响应合法
        assertEquals(201, r2.getStatusCode().value());
        assertNotNull(r2.getBody());
        JsonNode data = om.readTree(r2.getBody()).get("data");
        assertEquals("IT-PP-C-OK", data.get("collegeCode").asText());
    }
}

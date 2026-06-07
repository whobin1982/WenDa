package com.wenda.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实集成测试（基线：修复 #3）。
 *
 * <p>使用 Testcontainers 启动 PostgreSQL 16 service；启动 Spring Boot test context；
 * 验证 Flyway V1 迁移已应用 + 关键表存在；调真实 HTTP 端点
 * {@code /actuator/health} 和 {@code /api/v1/roles}。
 *
 * <p>CI 由 maven-failsafe-plugin 在 {@code verify} 阶段执行（基线 backend-ci.yml）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class FlywayAndApiSmokeIT {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void contextLoadsAndFlywayV1Applied() {
        // 1) 关键核心表存在
        Integer schools = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'schools'",
                Integer.class);
        assertNotNull(schools);
        assertTrue(schools >= 1, "Flyway V1 未创建 schools 表");

        Integer users = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'",
                Integer.class);
        assertNotNull(users);
        assertTrue(users >= 1, "Flyway V1 未创建 users 表");

        Integer audit = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'audit_logs'",
                Integer.class);
        assertNotNull(audit);
        assertTrue(audit >= 1, "Flyway V1 未创建 audit_logs 表");

        Integer roles = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'roles'",
                Integer.class);
        assertNotNull(roles);
        assertTrue(roles >= 1, "Flyway V1 未创建 roles 表");

        Integer idem = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'idempotency_keys'",
                Integer.class);
        assertNotNull(idem);
        assertTrue(idem >= 1, "Flyway V1 未创建 idempotency_keys 表");

        // 2) Flyway schema_history 至少 1 行
        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true",
                Integer.class);
        assertNotNull(applied);
        assertTrue(applied >= 1, "Flyway 未记录任何成功迁移");
    }

    @Test
    void seedRolesInsertedByV1() {
        // V1 末尾的种子数据：10 条内置角色
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM roles WHERE is_system = true",
                Integer.class);
        assertNotNull(n);
        assertEquals(10, n.intValue(), "V1 应插入 10 条系统角色");
    }

    @Test
    void healthEndpointIsUp() {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        ResponseEntity<String> r = client.get().uri("/actuator/health").retrieve().toEntity(String.class);
        assertEquals(200, r.getStatusCode().value(), "actuator/health 必须 200");
        assertNotNull(r.getBody());
        assertTrue(r.getBody().contains("\"status\":\"UP\""),
                "actuator/health 响应应包含 status=UP，实际：" + r.getBody());
    }

    @Test
    void rolesEndpointIsAuthProtected() {
        // /api/v1/roles 是受保护端点；未登录返回 401（基线 SecurityConfig）。
        // 真正的角色元数据访问需 JWT；本期 IT 范围只验证鉴权边界。
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        try {
            client.get().uri("/api/v1/roles").retrieve().toEntity(String.class);
            org.junit.jupiter.api.Assertions.fail("/api/v1/roles 在未登录时必须返回 401");
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertEquals(401, ex.getStatusCode().value());
            String body = ex.getResponseBodyAsString();
            assertTrue(body.contains("\"code\":\"UNAUTHORIZED\""),
                    "应返回 UNAUTHORIZED；实际：" + body);
        }
    }

    @Test
    void loginRequestBodyIsNotConsumedByIdempotencyFilter() {
        // 修复 #1 验证：POST /api/v1/auth/login 即使带 Idempotency-Key，
        // Controller 也能正常解析 @RequestBody（基线 401 因为缺 school，但 body 已到）。
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "it-key-001");
        String json = "{\"schoolCode\":\"NOEXIST\",\"username\":\"u\",\"password\":\"p\"}";
        HttpEntity<String> req = new HttpEntity<>(json, headers);
        try {
            client.post().uri("/api/v1/auth/login").headers(h -> h.addAll(headers)).body(json).retrieve().toEntity(String.class);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            // 期望 401（UNAUTHORIZED）；证明 body 已被 Spring 解析
            assertEquals(401, ex.getStatusCode().value());
            // 错误码必须是字典中的 UNAUTHORIZED
            String body = ex.getResponseBodyAsString();
            assertTrue(body.contains("\"code\":\"UNAUTHORIZED\""),
                    "应返回 UNAUTHORIZED；实际：" + body);
        }
    }
}

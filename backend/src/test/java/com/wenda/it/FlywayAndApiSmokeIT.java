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
        // 修复 GOV-002 #3 方案 A：login 不再标 @Idempotent。
        // 断言：login 带 Idempotency-Key 仍能解析 JSON body 并返回 401（解析失败/认证失败）；
        // 不进入幂等拦截器持久化逻辑。
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "it-key-001");
        String json = "{\"schoolCode\":\"NOEXIST\",\"username\":\"u\",\"password\":\"p\"}";
        try {
            client.post().uri("/api/v1/auth/login").headers(h -> h.addAll(headers)).body(json).retrieve().toEntity(String.class);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            // 期望 401（UNAUTHORIZED）；证明 body 已被 Spring 解析（不是被幂等过滤器消费了）
            assertEquals(401, ex.getStatusCode().value());
            String body = ex.getResponseBodyAsString();
            assertTrue(body.contains("\"code\":\"UNAUTHORIZED\""),
                    "应返回 UNAUTHORIZED；实际：" + body);
        }
        // 关键断言：login 不应在 idempotency_keys 表中留任何记录
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM idempotency_keys WHERE path = '/api/v1/auth/login'",
                Long.class);
        assertEquals(Long.valueOf(0L), n,
                "login 端点不应向 idempotency_keys 写入任何记录（@Idempotent 已移除）");
    }
}

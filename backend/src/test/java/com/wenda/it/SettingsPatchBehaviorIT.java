package com.wenda.it;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Settings PATCH containsKey 行为测试（基线：GOV-002 修复 #5）。
 *
 * <p>断言：
 * <ul>
 *   <li>不传 Boolean 字段保留原值（修复前的"重置"bug）；</li>
 *   <li>传 false 正确写 false；</li>
 *   <li>AI enabled 的审批约束（无 approvalRecordId 不可启用）仍生效。</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SettingsPatchBehaviorIT {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper om = new ObjectMapper();

    private static final String SCHOOL_CODE = "SETP";
    private static final String USERNAME = "setp-user";
    private static final String PWD = "TestP@ssw0rd!";

    private UUID schoolId;
    private UUID tenantId;
    private UUID userId;
    private String accessToken;
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
                    schoolId, SCHOOL_CODE, "Settings PATCH IT 学校", tenantId);
            this.userId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO users (id, school_id, tenant_id, username, display_name, status, user_type, version) "
                            + "VALUES (?,?,?,?,?, 'ACTIVE', 'INTERNAL', 0)",
                    userId, schoolId, tenantId, USERNAME, "Settings PATCH 用户");
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

    private void loginAndBind() {
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

    private long currentCourseCodePolicyVersion() {
        Long v = jdbc.queryForObject(
                "SELECT version FROM course_code_policy WHERE school_id = ?",
                Long.class, schoolId);
        return v == null ? 0L : v;
    }

    private long currentAISettingsVersion() {
        Long v = jdbc.queryForObject(
                "SELECT version FROM school_ai_settings WHERE school_id = ?",
                Long.class, schoolId);
        return v == null ? 0L : v;
    }

    private long currentWarningRulesVersion() {
        Long v = jdbc.queryForObject(
                "SELECT version FROM growth_warning_rules WHERE school_id = ?",
                Long.class, schoolId);
        return v == null ? 0L : v;
    }

    private long currentQualityRulesVersion() {
        Long v = jdbc.queryForObject(
                "SELECT version FROM school_quality_rules WHERE school_id = ?",
                Long.class, schoolId);
        return v == null ? 0L : v;
    }

    /**
     * Case 1：course-code-policy PATCH 不传 allowTempCode 保留原值。
     */
    @Test
    @Order(1)
    void case1_courseCodePolicy_unsetBool保留原值() {
        ensureFixtures();
        loginAndBind();
        // 初始化：allow_temp_code = true
        jdbc.update(
                "INSERT INTO course_code_policy (school_id, allow_temp_code, temp_code_prefix, "
                        + "temp_code_ttl_days, version) VALUES (?, TRUE, 'T-', 180, 0) "
                        + "ON CONFLICT (school_id) DO UPDATE SET allow_temp_code = TRUE, "
                        + "temp_code_prefix = 'T-', temp_code_ttl_days = 180, version = 0",
                schoolId);
        long beforeVersion = currentCourseCodePolicyVersion();

        // PATCH：只传 tempCodePrefix，不传 allowTempCode
        Map<String, Object> body = new HashMap<>();
        body.put("tempCodePrefix", "X-");
        long ifMatch = beforeVersion;
        RestClient client = client();
        try {
            client.patch().uri("/api/v1/school-settings/course-code-policy")
                    .header("If-Match", String.valueOf(ifMatch))
                    .body(body)
                    .retrieve().toEntity(String.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        Boolean allowTempCode = jdbc.queryForObject(
                "SELECT allow_temp_code FROM course_code_policy WHERE school_id = ?",
                Boolean.class, schoolId);
        assertEquals(Boolean.TRUE, allowTempCode, "未传 allowTempCode 应保留原值 true");
        String prefix = jdbc.queryForObject(
                "SELECT temp_code_prefix FROM course_code_policy WHERE school_id = ?",
                String.class, schoolId);
        assertEquals("X-", prefix, "tempCodePrefix 应被新值覆盖");
    }

    /**
     * Case 2：AI settings PATCH 不传 externalEnabled 保留原值。
     */
    @Test
    @Order(2)
    void case2_aiSettings_unsetBool保留原值() {
        ensureFixtures();
        loginAndBind();
        // 初始化：external_enabled = true（带审批记录）
        jdbc.update(
                "INSERT INTO school_ai_settings (school_id, external_provider_code, external_enabled, "
                        + "student_data_outbound, prompt_version, schema_version, version) "
                        + "VALUES (?, 'external-validated', TRUE, FALSE, 'v1', 'v1', 0) "
                        + "ON CONFLICT (school_id) DO UPDATE SET external_provider_code = 'external-validated', "
                        + "external_enabled = TRUE, student_data_outbound = FALSE, "
                        + "prompt_version = 'v1', schema_version = 'v1', version = 0",
                schoolId);
        long beforeVersion = currentAISettingsVersion();

        // PATCH：只传 promptVersion，不传 externalEnabled / studentDataOutbound
        Map<String, Object> body = new HashMap<>();
        body.put("promptVersion", "v2");
        RestClient client = client();
        try {
            client.patch().uri("/api/v1/school-settings/ai")
                    .header("If-Match", String.valueOf(beforeVersion))
                    .body(body)
                    .retrieve().toEntity(String.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        Boolean externalEnabled = jdbc.queryForObject(
                "SELECT external_enabled FROM school_ai_settings WHERE school_id = ?",
                Boolean.class, schoolId);
        assertEquals(Boolean.TRUE, externalEnabled, "未传 externalEnabled 应保留原值 true");
        String prompt = jdbc.queryForObject(
                "SELECT prompt_version FROM school_ai_settings WHERE school_id = ?",
                String.class, schoolId);
        assertEquals("v2", prompt, "promptVersion 应被新值覆盖");
    }

    /**
     * Case 3：AI settings PATCH 显式传 false 正确写 false。
     */
    @Test
    @Order(3)
    void case3_aiSettings_explicitFalse正确写入() {
        ensureFixtures();
        loginAndBind();
        jdbc.update(
                "INSERT INTO school_ai_settings (school_id, external_provider_code, external_enabled, "
                        + "student_data_outbound, prompt_version, schema_version, version) "
                        + "VALUES (?, 'external-validated', TRUE, FALSE, 'v1', 'v1', 0) "
                        + "ON CONFLICT (school_id) DO UPDATE SET external_provider_code = 'external-validated', "
                        + "external_enabled = TRUE, student_data_outbound = FALSE, "
                        + "prompt_version = 'v1', schema_version = 'v1', version = 0",
                schoolId);
        long beforeVersion = currentAISettingsVersion();

        // PATCH：显式 externalEnabled=false
        Map<String, Object> body = new HashMap<>();
        body.put("externalEnabled", false);
        RestClient client = client();
        try {
            client.patch().uri("/api/v1/school-settings/ai")
                    .header("If-Match", String.valueOf(beforeVersion))
                    .body(body)
                    .retrieve().toEntity(String.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        Boolean externalEnabled = jdbc.queryForObject(
                "SELECT external_enabled FROM school_ai_settings WHERE school_id = ?",
                Boolean.class, schoolId);
        assertEquals(Boolean.FALSE, externalEnabled, "显式传 false 应写 false");
    }

    /**
     * Case 4：AI 启用约束（无 approvalRecordId 不可启用）仍生效。
     */
    @Test
    @Order(4)
    void case4_aiEnableRequiresApproval() {
        ensureFixtures();
        loginAndBind();
        // 初始化：external_enabled = false（无审批记录）
        jdbc.update(
                "INSERT INTO school_ai_settings (school_id, external_provider_code, external_enabled, "
                        + "student_data_outbound, prompt_version, schema_version, version) "
                        + "VALUES (?, 'disabled', FALSE, FALSE, 'v1', 'v1', 0) "
                        + "ON CONFLICT (school_id) DO UPDATE SET external_provider_code = 'disabled', "
                        + "external_enabled = FALSE, student_data_outbound = FALSE, "
                        + "prompt_version = 'v1', schema_version = 'v1', version = 0",
                schoolId);
        long beforeVersion = currentAISettingsVersion();

        // 尝试启用：externalEnabled=true 但无 approvalRecordId
        Map<String, Object> body = new HashMap<>();
        body.put("externalEnabled", true);
        // body 中没有 approvalRecordId
        RestClient client = client();
        try {
            client.patch().uri("/api/v1/school-settings/ai")
                    .header("If-Match", String.valueOf(beforeVersion))
                    .body(body)
                    .retrieve().toEntity(String.class);
            org.junit.jupiter.api.Assertions.fail("无 approvalRecordId 启用 AI 必须 400");
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertEquals(400, ex.getStatusCode().value());
            assertTrue(ex.getResponseBodyAsString().contains("approvalRecordId"),
                    "应提示缺 approvalRecordId；实际：" + ex.getResponseBodyAsString());
        }
    }

    /**
     * Case 5：warning-rules PATCH 不传 notificationEmail 保留原值。
     */
    @Test
    @Order(5)
    void case5_warningRules_unsetBool保留原值() {
        ensureFixtures();
        loginAndBind();
        jdbc.update(
                "INSERT INTO growth_warning_rules (school_id, rules_json, notification_email, version) "
                        + "VALUES (?, '{}', TRUE, 0) "
                        + "ON CONFLICT (school_id) DO UPDATE SET rules_json = '{}', "
                        + "notification_email = TRUE, version = 0",
                schoolId);
        long beforeVersion = currentWarningRulesVersion();

        // PATCH：只传 rulesJson
        Map<String, Object> body = new HashMap<>();
        body.put("rulesJson", "{\"key\":\"val\"}");
        RestClient client = client();
        try {
            client.patch().uri("/api/v1/school-settings/growth-warning-rules")
                    .header("If-Match", String.valueOf(beforeVersion))
                    .body(body)
                    .retrieve().toEntity(String.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        Boolean ne = jdbc.queryForObject(
                "SELECT notification_email FROM growth_warning_rules WHERE school_id = ?",
                Boolean.class, schoolId);
        assertEquals(Boolean.TRUE, ne, "未传 notificationEmail 应保留原值 true");
    }
}

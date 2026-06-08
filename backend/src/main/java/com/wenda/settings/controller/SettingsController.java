package com.wenda.settings.controller;

import com.wenda.context.RequestContextHolder;
import com.wenda.idempotency.Idempotent;
import com.wenda.request.IfMatchVerifier;
import com.wenda.response.ApiResponse;
import com.wenda.settings.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 学校级配置 Controller（API-CFG-001～API-CFG-012）。
 */
@RestController
@RequestMapping("/api/v1")
public class SettingsController {

    private final SettingsService settingsService;
    private final IfMatchVerifier ifMatchVerifier;

    public SettingsController(SettingsService settingsService, IfMatchVerifier ifMatchVerifier) {
        this.settingsService = settingsService;
        this.ifMatchVerifier = ifMatchVerifier;
    }

    private long ifMatchOrZero(HttpServletRequest request) {
        Long v = ifMatchVerifier.parseIfMatch(request);
        return v == null ? 0L : v;
    }

    // ===== API-CFG-001 / 002 =====
    @GetMapping("/school-settings/quality-rules")
    public ApiResponse<Object> getQuality() {
        return ApiResponse.ok(settingsService.getQualityRules(), RequestContextHolder.requestId());
    }

    @Idempotent
    @PatchMapping("/school-settings/quality-rules")
    public ResponseEntity<ApiResponse<Object>> updateQuality(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        long v = ifMatchOrZero(request);
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updateQualityRules(
                        asInt(body.get("minCredits")),
                        asInt(body.get("maxCredits")),
                        asDecimal(body.get("minPracticeRatio")),
                        asInt(body.get("maxCoursePerTerm")),
                        asString(body.get("minSupportDegree")),
                        asString(body.get("thresholdsJson")),
                        v),
                RequestContextHolder.requestId()));
    }

    // ===== API-CFG-003 / 004 =====
    @GetMapping("/school-settings/ai")
    public ApiResponse<Object> getAI() {
        return ApiResponse.ok(settingsService.getAISettings(), RequestContextHolder.requestId());
    }

    @Idempotent
    @PatchMapping("/school-settings/ai")
    public ResponseEntity<ApiResponse<Object>> updateAI(@RequestBody Map<String, Object> body,
                                                        HttpServletRequest request) {
        long v = ifMatchOrZero(request);
        // 修复 #7：把整个 body map 直接传给 service，service 用 containsKey 区分
        // "未传" 与 "传了 false"，避免局部更新意外重置布尔配置。
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updateAISettingsRaw(body, v),
                RequestContextHolder.requestId()));
    }

    // ===== API-CFG-005 / 006 =====
    @GetMapping("/school-settings/ability-levels")
    public ApiResponse<Object> getLevels() {
        return ApiResponse.ok(settingsService.getAbilityLevels(), RequestContextHolder.requestId());
    }

    @Idempotent
    @PutMapping("/school-settings/ability-levels")
    public ResponseEntity<ApiResponse<Object>> putLevels(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        long v = ifMatchOrZero(request);
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updateAbilityLevels(asString(body.get("levelsJson")), v),
                RequestContextHolder.requestId()));
    }

    // ===== API-CFG-007 / 008 =====
    @GetMapping("/school-settings/growth-warning-rules")
    public ApiResponse<Object> getWarning() {
        return ApiResponse.ok(settingsService.getWarningRules(), RequestContextHolder.requestId());
    }

    @Idempotent
    @PatchMapping("/school-settings/growth-warning-rules")
    public ResponseEntity<ApiResponse<Object>> updateWarning(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        long v = ifMatchOrZero(request);
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updateWarningRulesRaw(body, v),
                RequestContextHolder.requestId()));
    }

    // ===== API-CFG-009 / 010 =====
    @GetMapping("/school-settings/course-code-policy")
    public ApiResponse<Object> getCourseCode() {
        return ApiResponse.ok(settingsService.getCourseCodePolicy(), RequestContextHolder.requestId());
    }

    @Idempotent
    @PatchMapping("/school-settings/course-code-policy")
    public ResponseEntity<ApiResponse<Object>> updateCourseCode(@RequestBody Map<String, Object> body,
                                                                 HttpServletRequest request) {
        long v = ifMatchOrZero(request);
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updateCourseCodePolicyRaw(body, v),
                RequestContextHolder.requestId()));
    }

    // ===== API-CFG-011 / 012 =====
    @GetMapping("/school/ai-policy")
    public ApiResponse<Map<String, Object>> getAIPolicy() {
        return ApiResponse.ok(settingsService.snapshotAIPolicy(), RequestContextHolder.requestId());
    }

    @Idempotent
    @PutMapping("/school/ai-policy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> putAIPolicy(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updateAIPolicyRaw(body),
                RequestContextHolder.requestId()));
    }

    // ===== helpers =====
    private static Integer asInt(Object v) { return v == null ? null : (v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString())); }
    private static BigDecimal asDecimal(Object v) { return v == null ? null : new BigDecimal(v.toString()); }
    private static String asString(Object v) { return v == null ? null : v.toString(); }
    private static Boolean asBool(Object v) { return v == null ? null : Boolean.valueOf(v.toString()); }
}

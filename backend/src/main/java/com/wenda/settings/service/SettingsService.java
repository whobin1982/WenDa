package com.wenda.settings.service;

import com.wenda.audit.Audited;
import com.wenda.auth.permission.PermissionService;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.request.IfMatchVerifier;
import com.wenda.settings.repository.SettingsRepository;
import com.wenda.settings.repository.SettingsRepository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 学校级配置中心（基线：API-CFG-001～012 + 权限判定矩阵 v1.0 PERM-SCHOOL-002）。
 */
@Service
public class SettingsService {

    private final SettingsRepository repo;
    private final PermissionService permissionService;
    private final IfMatchVerifier ifMatchVerifier;

    public SettingsService(SettingsRepository repo, PermissionService permissionService,
                           IfMatchVerifier ifMatchVerifier) {
        this.repo = repo;
        this.permissionService = permissionService;
        this.ifMatchVerifier = ifMatchVerifier;
    }

    public QualityRulesRow getQualityRules() {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        return repo.getQualityRules(schoolId).orElseGet(() -> defaultQuality(schoolId));
    }

    @Audited(action = "UPDATE_QUALITY_RULES", resourceType = "settings-quality-rules",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public QualityRulesRow updateQualityRules(Integer minCredits, Integer maxCredits,
                                               BigDecimal minPracticeRatio, Integer maxCoursePerTerm,
                                               String minSupportDegree, String thresholdsJson,
                                               long ifMatch) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = getQualityRules();
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        var updated = repo.upsertQualityRules(new QualityRulesRow(
                schoolId,
                minCredits == null ? current.minCredits() : minCredits,
                maxCredits == null ? current.maxCredits() : maxCredits,
                minPracticeRatio == null ? current.minPracticeRatio() : minPracticeRatio,
                maxCoursePerTerm == null ? current.maxCoursePerTerm() : maxCoursePerTerm,
                minSupportDegree == null ? current.minSupportDegree() : minSupportDegree,
                thresholdsJson == null ? current.thresholdsJson() : thresholdsJson,
                current.version() + 1));
        return updated;
    }

    private QualityRulesRow defaultQuality(UUID schoolId) {
        return new QualityRulesRow(schoolId, 160, 200, new BigDecimal("0.15"), 12, "LOW",
                "{}", 0L);
    }

    public CourseCodePolicyRow getCourseCodePolicy() {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        return repo.getCourseCodePolicy(schoolId).orElseGet(() ->
                new CourseCodePolicyRow(schoolId, false, "T-", 180, 0L));
    }

    @Audited(action = "UPDATE_COURSE_CODE_POLICY", resourceType = "settings-course-code-policy",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public CourseCodePolicyRow updateCourseCodePolicyRaw(java.util.Map<String, Object> body, long ifMatch) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = getCourseCodePolicy();
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        // 修复 #7：用 body.containsKey 区分"未传"和"传了 false"。
        Boolean allowTempCode = body.containsKey("allowTempCode")
                ? asBool(body.get("allowTempCode"))
                : null;
        String tempCodePrefix = body.containsKey("tempCodePrefix")
                ? asString(body.get("tempCodePrefix"))
                : null;
        Integer tempCodeTtlDays = body.containsKey("tempCodeTtlDays")
                ? asInt(body.get("tempCodeTtlDays"))
                : null;
        return repo.upsertCourseCodePolicy(new CourseCodePolicyRow(schoolId,
                allowTempCode == null ? current.allowTempCode() : allowTempCode,
                tempCodePrefix == null ? current.tempCodePrefix() : tempCodePrefix,
                tempCodeTtlDays == null ? current.tempCodeTtlDays() : tempCodeTtlDays,
                current.version() + 1));
    }

    public AISettingsRow getAISettings() {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        return repo.getAISettings(schoolId).orElseGet(() ->
                new AISettingsRow(schoolId, "disabled", null, false, false, "v1", "v1", 0, null, 0L));
    }

    @Audited(action = "UPDATE_AI_SETTINGS", resourceType = "settings-ai",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public AISettingsRow updateAISettingsRaw(java.util.Map<String, Object> body, long ifMatch) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = getAISettings();
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        // 修复 #7：用 body.containsKey 区分"未传"和"传了 false"。
        Boolean externalEnabled = body.containsKey("externalEnabled")
                ? asBool(body.get("externalEnabled"))
                : null;
        Boolean studentDataOutbound = body.containsKey("studentDataOutbound")
                ? asBool(body.get("studentDataOutbound"))
                : null;
        String externalProviderCode = body.containsKey("externalProviderCode")
                ? asString(body.get("externalProviderCode"))
                : null;
        String externalModelId = body.containsKey("externalModelId")
                ? asString(body.get("externalModelId"))
                : null;
        String promptVersion = body.containsKey("promptVersion")
                ? asString(body.get("promptVersion"))
                : null;
        String schemaVersion = body.containsKey("schemaVersion")
                ? asString(body.get("schemaVersion"))
                : null;
        Integer quotaPerDay = body.containsKey("quotaPerDay")
                ? asInt(body.get("quotaPerDay"))
                : null;
        String approvalRecordId = body.containsKey("approvalRecordId")
                ? asString(body.get("approvalRecordId"))
                : null;
        // 硬性约束：学生数据禁出域 + 外部 Provider 启用必须带审批记录（基线 PERM-AI-001）
        boolean effectiveEnabled = externalEnabled == null ? current.externalEnabled() : externalEnabled;
        if (effectiveEnabled) {
            String effectiveApproval = approvalRecordId == null ? current.approvalRecordId() : approvalRecordId;
            if (effectiveApproval == null || effectiveApproval.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "启用外部 AI Provider 必须提供 approvalRecordId（基线 NFR-016 / RG-OSG-009）。");
            }
            boolean effectiveOutbound = studentDataOutbound == null
                    ? current.studentDataOutbound() : studentDataOutbound;
            if (effectiveOutbound) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "学生数据默认禁止出域（基线 NFR-010）。");
            }
        }
        return repo.upsertAISettings(new AISettingsRow(
                schoolId,
                externalProviderCode == null ? current.externalProviderCode() : externalProviderCode,
                externalModelId == null ? current.externalModelId() : externalModelId,
                effectiveEnabled,
                studentDataOutbound == null ? current.studentDataOutbound() : studentDataOutbound,
                promptVersion == null ? current.promptVersion() : promptVersion,
                schemaVersion == null ? current.schemaVersion() : schemaVersion,
                quotaPerDay == null ? current.quotaPerDay() : quotaPerDay,
                approvalRecordId == null ? current.approvalRecordId() : approvalRecordId,
                current.version() + 1));
    }

    public AbilityLevelsRow getAbilityLevels() {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        return repo.getAbilityLevels(schoolId).orElseGet(() ->
                new AbilityLevelsRow(schoolId, "[]", 0L));
    }

    @Audited(action = "UPDATE_ABILITY_LEVELS", resourceType = "settings-ability-levels",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public AbilityLevelsRow updateAbilityLevels(String levelsJson, long ifMatch) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = getAbilityLevels();
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        if (levelsJson == null || levelsJson.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "levelsJson 不能为空。");
        }
        return repo.upsertAbilityLevels(new AbilityLevelsRow(schoolId, levelsJson, current.version() + 1));
    }

    public WarningRulesRow getWarningRules() {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        return repo.getWarningRules(schoolId).orElseGet(() ->
                new WarningRulesRow(schoolId, "{}", false, 0L));
    }

    @Audited(action = "UPDATE_WARNING_RULES", resourceType = "settings-warning-rules",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public WarningRulesRow updateWarningRulesRaw(java.util.Map<String, Object> body, long ifMatch) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = getWarningRules();
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        // 修复 #7：用 body.containsKey 区分"未传"和"传了 false"。
        String rulesJson = body.containsKey("rulesJson") ? asString(body.get("rulesJson")) : null;
        Boolean notificationEmail = body.containsKey("notificationEmail")
                ? asBool(body.get("notificationEmail"))
                : null;
        return repo.upsertWarningRules(new WarningRulesRow(schoolId,
                rulesJson == null ? current.rulesJson() : rulesJson,
                notificationEmail == null ? current.notificationEmail() : notificationEmail,
                current.version() + 1));
    }

    public Map<String, Object> snapshotAIPolicy() {
        var ai = getAISettings();
        return Map.of(
                "externalProviderCode", ai.externalProviderCode(),
                "externalEnabled", ai.externalEnabled(),
                "studentDataOutbound", ai.studentDataOutbound(),
                "promptVersion", ai.promptVersion(),
                "schemaVersion", ai.schemaVersion(),
                "quotaPerDay", ai.quotaPerDay(),
                "approvalRecordId", ai.approvalRecordId() == null ? "" : ai.approvalRecordId(),
                "version", ai.version());
    }

    @Audited(action = "UPDATE_AI_POLICY", resourceType = "settings-ai-policy",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public Map<String, Object> updateAIPolicyRaw(java.util.Map<String, Object> body) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = getAISettings();
        // 修复 #7：把 body 透传给 AISettingsRow 内部处理；不传 = 保留 current
        AISettingsRow updated = updateAISettingsRaw(body, current.version());
        return Map.of(
                "externalProviderCode", updated.externalProviderCode(),
                "externalEnabled", updated.externalEnabled(),
                "studentDataOutbound", updated.studentDataOutbound(),
                "promptVersion", updated.promptVersion(),
                "schemaVersion", updated.schemaVersion(),
                "quotaPerDay", updated.quotaPerDay(),
                "approvalRecordId", updated.approvalRecordId() == null ? "" : updated.approvalRecordId(),
                "version", updated.version());
    }

    // ===== helpers（修复 #5：让 Raw 方法从 body 取值时做类型转换）=====
    private static Boolean asBool(Object v) {
        return v == null ? null : (v instanceof Boolean b ? b : Boolean.valueOf(v.toString()));
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }
}

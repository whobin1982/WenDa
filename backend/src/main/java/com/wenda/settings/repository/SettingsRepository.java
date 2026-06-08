package com.wenda.settings.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SettingsRepository {

    private final JdbcTemplate jdbc;

    public SettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ===== school_quality_rules =====
    public record QualityRulesRow(UUID schoolId, int minCredits, int maxCredits,
                                  BigDecimal minPracticeRatio, int maxCoursePerTerm,
                                  String minSupportDegree, String thresholdsJson, long version) {}

    public Optional<QualityRulesRow> getQualityRules(UUID schoolId) {
        try {
            QualityRulesRow row = jdbc.queryForObject(
                    "SELECT school_id, min_credits, max_credits, min_practice_ratio, "
                            + "max_course_per_term, min_support_degree, thresholds_json, version "
                            + "FROM school_quality_rules WHERE school_id = ?",
                    new RowMapper<QualityRulesRow>() {
                        @Override
                        public QualityRulesRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return new QualityRulesRow(
                                    (UUID) rs.getObject("school_id"),
                                    rs.getInt("min_credits"),
                                    rs.getInt("max_credits"),
                                    rs.getBigDecimal("min_practice_ratio"),
                                    rs.getInt("max_course_per_term"),
                                    rs.getString("min_support_degree"),
                                    rs.getString("thresholds_json"),
                                    rs.getLong("version"));
                        }
                    },
                    schoolId);
            return Optional.ofNullable(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public QualityRulesRow upsertQualityRules(QualityRulesRow row) {
        jdbc.update(
                "INSERT INTO school_quality_rules (school_id, min_credits, max_credits, "
                        + "min_practice_ratio, max_course_per_term, min_support_degree, thresholds_json, "
                        + "updated_by, version) VALUES (?,?,?,?,?,?,?::jsonb,?,?) "
                        + "ON CONFLICT (school_id) DO UPDATE SET "
                        + "min_credits = EXCLUDED.min_credits, max_credits = EXCLUDED.max_credits, "
                        + "min_practice_ratio = EXCLUDED.min_practice_ratio, "
                        + "max_course_per_term = EXCLUDED.max_course_per_term, "
                        + "min_support_degree = EXCLUDED.min_support_degree, "
                        + "thresholds_json = EXCLUDED.thresholds_json, "
                        + "updated_by = EXCLUDED.updated_by, updated_at = now(), "
                        + "version = school_quality_rules.version + 1",
                row.schoolId(), row.minCredits(), row.maxCredits(), row.minPracticeRatio(),
                row.maxCoursePerTerm(), row.minSupportDegree(), row.thresholdsJson(),
                null, row.version());
        return getQualityRules(row.schoolId()).orElseThrow();
    }

    // ===== course_code_policy =====
    public record CourseCodePolicyRow(UUID schoolId, boolean allowTempCode, String tempCodePrefix,
                                      int tempCodeTtlDays, long version) {}

    public Optional<CourseCodePolicyRow> getCourseCodePolicy(UUID schoolId) {
        try {
            CourseCodePolicyRow row = jdbc.queryForObject(
                    "SELECT school_id, allow_temp_code, temp_code_prefix, temp_code_ttl_days, version "
                            + "FROM course_code_policy WHERE school_id = ?",
                    (rs, i) -> new CourseCodePolicyRow(
                            (UUID) rs.getObject("school_id"),
                            rs.getBoolean("allow_temp_code"),
                            rs.getString("temp_code_prefix"),
                            rs.getInt("temp_code_ttl_days"),
                            rs.getLong("version")),
                    schoolId);
            return Optional.ofNullable(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public CourseCodePolicyRow upsertCourseCodePolicy(CourseCodePolicyRow row) {
        jdbc.update(
                "INSERT INTO course_code_policy (school_id, allow_temp_code, temp_code_prefix, "
                        + "temp_code_ttl_days, updated_by, version) VALUES (?,?,?,?,?,?) "
                        + "ON CONFLICT (school_id) DO UPDATE SET "
                        + "allow_temp_code = EXCLUDED.allow_temp_code, "
                        + "temp_code_prefix = EXCLUDED.temp_code_prefix, "
                        + "temp_code_ttl_days = EXCLUDED.temp_code_ttl_days, "
                        + "updated_by = EXCLUDED.updated_by, updated_at = now(), "
                        + "version = course_code_policy.version + 1",
                row.schoolId(), row.allowTempCode(), row.tempCodePrefix(), row.tempCodeTtlDays(),
                null, row.version());
        return getCourseCodePolicy(row.schoolId()).orElseThrow();
    }

    // ===== school_ai_settings =====
    public record AISettingsRow(UUID schoolId, String externalProviderCode, String externalModelId,
                                boolean externalEnabled, boolean studentDataOutbound,
                                String promptVersion, String schemaVersion, int quotaPerDay,
                                String approvalRecordId, long version) {}

    public Optional<AISettingsRow> getAISettings(UUID schoolId) {
        try {
            AISettingsRow row = jdbc.queryForObject(
                    "SELECT school_id, external_provider_code, external_model_id, external_enabled, "
                            + "student_data_outbound, prompt_version, schema_version, quota_per_day, "
                            + "approval_record_id, version "
                            + "FROM school_ai_settings WHERE school_id = ?",
                    (rs, i) -> new AISettingsRow(
                            (UUID) rs.getObject("school_id"),
                            rs.getString("external_provider_code"),
                            rs.getString("external_model_id"),
                            rs.getBoolean("external_enabled"),
                            rs.getBoolean("student_data_outbound"),
                            rs.getString("prompt_version"),
                            rs.getString("schema_version"),
                            rs.getInt("quota_per_day"),
                            rs.getString("approval_record_id"),
                            rs.getLong("version")),
                    schoolId);
            return Optional.ofNullable(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public AISettingsRow upsertAISettings(AISettingsRow row) {
        jdbc.update(
                "INSERT INTO school_ai_settings (school_id, external_provider_code, external_model_id, "
                        + "external_enabled, student_data_outbound, prompt_version, schema_version, "
                        + "quota_per_day, approval_record_id, updated_by, version) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?) "
                        + "ON CONFLICT (school_id) DO UPDATE SET "
                        + "external_provider_code = EXCLUDED.external_provider_code, "
                        + "external_model_id = EXCLUDED.external_model_id, "
                        + "external_enabled = EXCLUDED.external_enabled, "
                        + "student_data_outbound = EXCLUDED.student_data_outbound, "
                        + "prompt_version = EXCLUDED.prompt_version, "
                        + "schema_version = EXCLUDED.schema_version, "
                        + "quota_per_day = EXCLUDED.quota_per_day, "
                        + "approval_record_id = EXCLUDED.approval_record_id, "
                        + "updated_by = EXCLUDED.updated_by, updated_at = now(), "
                        + "version = school_ai_settings.version + 1",
                row.schoolId(), row.externalProviderCode(), row.externalModelId(),
                row.externalEnabled(), row.studentDataOutbound(), row.promptVersion(),
                row.schemaVersion(), row.quotaPerDay(), row.approvalRecordId(),
                null, row.version());
        return getAISettings(row.schoolId()).orElseThrow();
    }

    // ===== school_ability_level_settings =====
    public record AbilityLevelsRow(UUID schoolId, String levelsJson, long version) {}

    public Optional<AbilityLevelsRow> getAbilityLevels(UUID schoolId) {
        try {
            AbilityLevelsRow row = jdbc.queryForObject(
                    "SELECT school_id, levels_json, version "
                            + "FROM school_ability_level_settings WHERE school_id = ?",
                    (rs, i) -> new AbilityLevelsRow(
                            (UUID) rs.getObject("school_id"),
                            rs.getString("levels_json"),
                            rs.getLong("version")),
                    schoolId);
            return Optional.ofNullable(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public AbilityLevelsRow upsertAbilityLevels(AbilityLevelsRow row) {
        jdbc.update(
                "INSERT INTO school_ability_level_settings (school_id, levels_json, updated_by, version) "
                        + "VALUES (?,?::jsonb,?,?) "
                        + "ON CONFLICT (school_id) DO UPDATE SET "
                        + "levels_json = EXCLUDED.levels_json, "
                        + "updated_by = EXCLUDED.updated_by, updated_at = now(), "
                        + "version = school_ability_level_settings.version + 1",
                row.schoolId(), row.levelsJson(), null, row.version());
        return getAbilityLevels(row.schoolId()).orElseThrow();
    }

    // ===== growth_warning_rules =====
    public record WarningRulesRow(UUID schoolId, String rulesJson, boolean notificationEmail,
                                  long version) {}

    public Optional<WarningRulesRow> getWarningRules(UUID schoolId) {
        try {
            WarningRulesRow row = jdbc.queryForObject(
                    "SELECT school_id, rules_json, notification_email, version "
                            + "FROM growth_warning_rules WHERE school_id = ?",
                    (rs, i) -> new WarningRulesRow(
                            (UUID) rs.getObject("school_id"),
                            rs.getString("rules_json"),
                            rs.getBoolean("notification_email"),
                            rs.getLong("version")),
                    schoolId);
            return Optional.ofNullable(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public WarningRulesRow upsertWarningRules(WarningRulesRow row) {
        jdbc.update(
                "INSERT INTO growth_warning_rules (school_id, rules_json, notification_email, "
                        + "updated_by, version) VALUES (?,?::jsonb,?,?,?) "
                        + "ON CONFLICT (school_id) DO UPDATE SET "
                        + "rules_json = EXCLUDED.rules_json, "
                        + "notification_email = EXCLUDED.notification_email, "
                        + "updated_by = EXCLUDED.updated_by, updated_at = now(), "
                        + "version = growth_warning_rules.version + 1",
                row.schoolId(), row.rulesJson(), row.notificationEmail(), null, row.version());
        return getWarningRules(row.schoolId()).orElseThrow();
    }
}

package com.wenda.dashboard.controller;

import com.wenda.auth.permission.PermissionService;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.response.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 看板骨架 Controller（API-DSH-001 / DSH-002 / DSH-003 + 架构 v0.3 §6.2 M-04）。
 *
 * <p>本期仅返回基础计数与待办摘要；复杂指标（成熟度、完整度、风险）随对应业务模块
 * （M-05 / M-07）就绪后接入。
 */
@RestController
@RequestMapping("/api/v1/dashboards")
public class DashboardController {

    private final JdbcTemplate jdbc;
    private final PermissionService permissionService;

    public DashboardController(JdbcTemplate jdbc, PermissionService permissionService) {
        this.jdbc = jdbc;
        this.permissionService = permissionService;
    }

    @GetMapping("/school")
    public ApiResponse<Map<String, Object>> school() {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        long colleges = countColleges(schoolId);
        long majors = countMajors(schoolId);
        long activeUsers = countActiveUsers(schoolId);
        long pendingReviews = countPendingReviews(schoolId);
        return ApiResponse.ok(Map.of(
                "scope", "SCHOOL",
                "scopeId", schoolId.toString(),
                "metrics", Map.of(
                        "colleges", colleges,
                        "majors", majors,
                        "activeUsers", activeUsers,
                        "pendingReviews", pendingReviews
                ),
                "todoSummary", Map.of(
                        "pendingReview", pendingReviews
                )
        ), RequestContextHolder.requestId());
    }

    @GetMapping("/colleges/{collegeId}")
    public ApiResponse<Map<String, Object>> college(@PathVariable UUID collegeId) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        ensureCollegeInSchool(collegeId, schoolId);
        long majors = countMajorsByCollege(schoolId, collegeId);
        long pendingReviews = countPendingReviewsByCollege(schoolId, collegeId);
        return ApiResponse.ok(Map.of(
                "scope", "COLLEGE",
                "scopeId", collegeId.toString(),
                "metrics", Map.of(
                        "majors", majors,
                        "pendingReviews", pendingReviews
                ),
                "todoSummary", Map.of(
                        "pendingReview", pendingReviews
                )
        ), RequestContextHolder.requestId());
    }

    @GetMapping("/majors/{majorId}")
    public ApiResponse<Map<String, Object>> major(@PathVariable UUID majorId) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        // MVP-1 阶段 majors 表暂未创建；返回 0 计数 + 提示
        return ApiResponse.ok(Map.of(
                "scope", "MAJOR",
                "scopeId", majorId.toString(),
                "metrics", Map.of(
                        "curriculumVersions", 0,
                        "obeIndicators", 0,
                        "supportMatrixItems", 0,
                        "pendingReviews", 0
                ),
                "todoSummary", Map.of(
                        "pendingReview", 0
                ),
                "note", "MVP-1 看板骨架；专业级指标在 DEV-010～DEV-022 业务模块就绪后接入。"
        ), RequestContextHolder.requestId());
    }

    // ===== helpers =====
    private long countColleges(UUID schoolId) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM colleges WHERE school_id = ? AND archived_at IS NULL",
                Long.class, schoolId);
        return n == null ? 0 : n;
    }

    private long countMajors(UUID schoolId) {
        try {
            Long n = jdbc.queryForObject(
                    "SELECT count(*) FROM majors WHERE school_id = ? AND archived_at IS NULL",
                    Long.class, schoolId);
            return n == null ? 0 : n;
        } catch (Exception ex) {
            return 0L; // majors 表尚未迁移（MVP-1 后续 PR）
        }
    }

    private long countMajorsByCollege(UUID schoolId, UUID collegeId) {
        try {
            Long n = jdbc.queryForObject(
                    "SELECT count(*) FROM majors WHERE school_id = ? AND college_id = ? AND archived_at IS NULL",
                    Long.class, schoolId, collegeId);
            return n == null ? 0 : n;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private long countActiveUsers(UUID schoolId) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE school_id = ? AND status = 'ACTIVE' AND archived_at IS NULL",
                Long.class, schoolId);
        return n == null ? 0 : n;
    }

    private long countPendingReviews(UUID schoolId) {
        try {
            Long n = jdbc.queryForObject(
                    "SELECT count(*) FROM human_reviews WHERE school_id = ? AND status = 'PENDING'",
                    Long.class, schoolId);
            return n == null ? 0 : n;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private long countPendingReviewsByCollege(UUID schoolId, UUID collegeId) {
        try {
            Long n = jdbc.queryForObject(
                    "SELECT count(*) FROM human_reviews WHERE school_id = ? AND status = 'PENDING' AND college_id = ?",
                    Long.class, schoolId, collegeId);
            return n == null ? 0 : n;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private void ensureCollegeInSchool(UUID collegeId, UUID schoolId) {
        Integer c = jdbc.query(
                "SELECT 1 FROM colleges WHERE id = ? AND school_id = ? AND archived_at IS NULL",
                rs -> rs.next() ? 1 : 0, collegeId, schoolId);
        if (c == null || c == 0) {
            throw new BusinessException(ErrorCode.SCOPE_FORBIDDEN);
        }
    }
}

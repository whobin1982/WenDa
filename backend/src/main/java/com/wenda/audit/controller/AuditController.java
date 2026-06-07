package com.wenda.audit.controller;

import com.wenda.audit.AuditService;
import com.wenda.auth.permission.PermissionService;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志查询（API-AUD-001 + 权限判定矩阵 v1.0 PERM-SCHOOL-003）。
 *
 * <p>只读，按 schoolId 隔离；越权返回 SCOPE_FORBIDDEN；不返回敏感字段（基线 NFR-012）。
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditService auditService;
    private final PermissionService permissionService;

    public AuditController(AuditService auditService, PermissionService permissionService) {
        this.auditService = auditService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String startAt,
            @RequestParam(required = false) String endAt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        // 学校空间隔离：系统管理员可显式传 schoolId；其他角色固定为当前 school
        UUID effectiveSchoolId = schoolId;
        if (effectiveSchoolId == null) {
            effectiveSchoolId = RequestContextHolder.schoolId();
        }
        if (effectiveSchoolId == null) {
            throw new BusinessException(ErrorCode.SCHOOL_SCOPE_REQUIRED);
        }
        permissionService.requireSameSchoolRead(effectiveSchoolId);
        // 仅 SCHOOL_ADMIN / SYSTEM_ADMIN 可查全校审计；其他角色仅查自己相关
        if (!permissionService.hasAnyRole(PermissionService.SCHOOL_ADMIN, PermissionService.SYS_ADMIN)
                && userId == null) {
            userId = RequestContextHolder.userId();
        }
        OffsetDateTime from = startAt == null ? null : OffsetDateTime.parse(startAt);
        OffsetDateTime to = endAt == null ? null : OffsetDateTime.parse(endAt);
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;

        long total = auditService.count(effectiveSchoolId, action, resourceType, userId, from, to);
        var items = auditService.search(effectiveSchoolId, action, resourceType, userId, from, to,
                page, pageSize);
        return ApiResponse.ok(Map.of(
                "items", items,
                "page", page,
                "pageSize", pageSize,
                "total", total,
                "totalPages", (total + pageSize - 1) / pageSize,
                "sort", "createdAt",
                "order", "desc"), RequestContextHolder.requestId());
    }
}

package com.wenda.audit;

import com.wenda.config.WendaProperties;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 业务审计日志服务（基线：架构 v0.3 §6.2 M-23 + 接口文档 v0.2 §2.3 写操作必须记录）。
 *
 * <p>追加式 PostgreSQL 表 + 分区；普通业务事件用 {@code NORMAL} 风险，授权 / 隐私相关
 * 用 {@code SENSITIVE}，登录失败 / 越权尝试用 {@code SECURITY}。
 */
@Service
public class AuditService {

    private final JdbcTemplate jdbc;
    private final WendaProperties properties;

    public AuditService(JdbcTemplate jdbc, WendaProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public void record(String action, String resourceType, String resourceId,
                       String method, String path, int statusCode,
                       Map<String, Object> details, Audited.Risk risk) {
        if (!properties.getAudit().isEnabled()) return;
        UUID schoolId = RequestContextHolder.schoolId();
        UUID tenantId = RequestContextHolder.tenantId();
        UUID userId = RequestContextHolder.userId();
        String username = RequestContextHolder.username();
        var request = RequestContextHolder.currentRequest();
        String ip = request == null ? null : request.getRemoteAddr();
        String ua = request == null ? null : request.getHeader("User-Agent");
        String requestId = RequestContextHolder.requestId();
        try {
            jdbc.update(
                    "INSERT INTO audit_logs ("
                            + "id, school_id, tenant_id, user_id, user_name, action, resource_type, resource_id, "
                            + "method, path, status_code, request_id, ip, user_agent, details, risk_level, created_at"
                            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?)",
                    UUID.randomUUID(), schoolId, tenantId, userId, username,
                    action, resourceType, resourceId,
                    method, path, statusCode, requestId, ip, ua,
                    details == null ? "{}" : toJson(details),
                    risk == null ? "NORMAL" : risk.name(),
                    Instant.now());
        } catch (Exception ex) {
            // 审计失败不能阻塞业务；记录到运行日志
            org.slf4j.LoggerFactory.getLogger(AuditService.class)
                    .error("audit log insert failed action={} resourceType={} err={}",
                            action, resourceType, ex.getMessage());
        }
    }

    private static String toJson(Map<String, Object> m) {
        // 极简 JSON 序列化（避免引入 Jackson 依赖到审计模块），实际生产可替换
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);
        } catch (Exception ex) {
            return "{}";
        }
    }

    /**
     * 查询审计日志（基线：API-AUD-001 接口实现 + 权限判定矩阵 v1.0 PERM-SCHOOL-003）。
     *
     * <p>本方法不做鉴权；由 controller 在调用前完成 {@code PermissionService.assertCanReadAudit()}。
     */
    public List<Map<String, Object>> search(UUID schoolId, String action, String resourceType,
                                            UUID userId, java.time.OffsetDateTime from,
                                            java.time.OffsetDateTime to, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, school_id, user_id, user_name, action, resource_type, resource_id, "
                        + "method, path, status_code, request_id, ip, details, risk_level, created_at "
                        + "FROM audit_logs WHERE 1=1");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (schoolId != null) { sql.append(" AND school_id = ?"); args.add(schoolId); }
        if (action != null && !action.isBlank()) { sql.append(" AND action = ?"); args.add(action); }
        if (resourceType != null && !resourceType.isBlank()) { sql.append(" AND resource_type = ?"); args.add(resourceType); }
        if (userId != null) { sql.append(" AND user_id = ?"); args.add(userId); }
        if (from != null) { sql.append(" AND created_at >= ?"); args.add(from); }
        if (to != null) { sql.append(" AND created_at < ?"); args.add(to); }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add((page - 1) * pageSize);
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public long count(UUID schoolId, String action, String resourceType, UUID userId,
                      java.time.OffsetDateTime from, java.time.OffsetDateTime to) {
        StringBuilder sql = new StringBuilder("SELECT count(*) FROM audit_logs WHERE 1=1");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (schoolId != null) { sql.append(" AND school_id = ?"); args.add(schoolId); }
        if (action != null && !action.isBlank()) { sql.append(" AND action = ?"); args.add(action); }
        if (resourceType != null && !resourceType.isBlank()) { sql.append(" AND resource_type = ?"); args.add(resourceType); }
        if (userId != null) { sql.append(" AND user_id = ?"); args.add(userId); }
        if (from != null) { sql.append(" AND created_at >= ?"); args.add(from); }
        if (to != null) { sql.append(" AND created_at < ?"); args.add(to); }
        Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0 : n;
    }

    public boolean isAuditedAction(ErrorCode code) {
        return code != null && code.audited();
    }
}

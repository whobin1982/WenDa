package com.wenda.auth.permission;

import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * 权限判定服务（基线：权限判定矩阵 v1.0 §1 RBAC+Scope+ABAC + §3 权限规则编号）。
 *
 * <p>本期 MVP-1 落地：
 * <ul>
 *   <li>PERM-SYS-001 系统管理员创建学校空间</li>
 *   <li>PERM-SCHOOL-001/002/003 学校空间内组织/用户/配置/审计</li>
 * </ul>
 *
 * <p>实现原则：
 * <ol>
 *   <li>鉴权边界在后端；前端菜单只用于体验优化。</li>
 *   <li>任何业务资源必须先检查 {@code schoolId} 与当前用户所属 school 一致，否则返回 {@code SCOPE_FORBIDDEN}。</li>
 *   <li>写操作失败与越权访问必须写安全审计（{@code SECURITY} 风险等级）。</li>
 * </ul>
 */
@Service
public class PermissionService {

    public static final String SYS_ADMIN = "SYSTEM_ADMIN";
    public static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";
    public static final String COLLEGE_MANAGER = "COLLEGE_MANAGER";
    public static final String MAJOR_OWNER = "MAJOR_OWNER";
    public static final String ACADEMIC_ADMIN = "ACADEMIC_ADMIN";
    public static final String TEACHER = "TEACHER";
    public static final String MENTOR = "MENTOR";
    public static final String STUDENT = "STUDENT";
    public static final String EMPLOYER_MENTOR = "EMPLOYER_MENTOR";
    public static final String KNOWLEDGE_ADMIN = "KNOWLEDGE_ADMIN";

    public Set<String> currentRoles() {
        Set<String> r = RequestContextHolder.roles();
        return r == null ? Set.of() : r;
    }

    public boolean hasRole(String role) {
        return currentRoles().contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        for (String r : roles) if (hasRole(r)) return true;
        return false;
    }

    public void requireLogin() {
        if (RequestContextHolder.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public void requireRole(String role) {
        requireLogin();
        if (!hasRole(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "无权限访问资源。",
                    java.util.List.of(java.util.Map.of("requiredRole", role)));
        }
    }

    public void requireAnyRole(String... roles) {
        requireLogin();
        if (!hasAnyRole(roles)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public void requireSameSchool(UUID schoolId) {
        if (schoolId == null) {
            throw new BusinessException(ErrorCode.SCHOOL_SCOPE_REQUIRED);
        }
        UUID mine = RequestContextHolder.schoolId();
        // 系统管理员可跨学校（仅运维场景）
        if (hasRole(SYS_ADMIN)) return;
        if (mine == null || !mine.equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCOPE_FORBIDDEN,
                    "无权访问该组织或资源范围。",
                    java.util.List.of(java.util.Map.of("schoolId", schoolId.toString())));
        }
    }

    /**
     * 系统管理员创建学校空间（PERM-SYS-001）：仅允许 SYSTEM_ADMIN 且必须未持有任何 schoolId。
     */
    public void requireSystemAdminBootstrap() {
        if (!hasRole(SYS_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "仅系统管理员可创建学校空间。",
                    java.util.List.of(java.util.Map.of("requiredRole", SYS_ADMIN)));
        }
    }

    /**
     * 学校级写操作（PERM-SCHOOL-001 / 002）：仅 SCHOOL_ADMIN 角色，且必须在同一 schoolId 范围。
     */
    public void requireSchoolAdmin(UUID schoolId) {
        requireSameSchool(schoolId);
        if (!hasRole(SCHOOL_ADMIN) && !hasRole(SYS_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "无权限访问资源。",
                    java.util.List.of(java.util.Map.of("requiredRole", SCHOOL_ADMIN)));
        }
    }

    /** 学校级读操作：同 school 即可，无需 ADMIN。 */
    public void requireSameSchoolRead(UUID schoolId) {
        requireSameSchool(schoolId);
    }
}

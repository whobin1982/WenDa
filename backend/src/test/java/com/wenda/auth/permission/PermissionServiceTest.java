package com.wenda.auth.permission;

import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PermissionService 单元测试（基线：权限判定矩阵 v1.0 §3 核心 P0 规则）。
 *
 * <p>正向：合法角色 / 同 school 写 / 系统管理员创建学校空间。
 * <p>反向：跨 school 越权 / 缺失角色 / 未登录。
 */
class PermissionServiceTest {

    private final PermissionService svc = new PermissionService();
    private final UUID schoolA = UUID.randomUUID();
    private final UUID schoolB = UUID.randomUUID();
    private final UUID tenantA = UUID.randomUUID();

    @BeforeEach
    void login() {
        RequestContextHolder.setAuth(schoolA, tenantA, UUID.randomUUID(), "u1",
                Set.of(PermissionService.SCHOOL_ADMIN));
    }

    @AfterEach
    void logout() {
        RequestContextHolder.clear();
    }

    @Test
    void requireSameSchoolPassesForSame() {
        assertDoesNotThrow(() -> svc.requireSameSchool(schoolA));
    }

    @Test
    void requireSameSchoolFailsForDifferent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.requireSameSchool(schoolB));
        assertEquals(ErrorCode.SCOPE_FORBIDDEN, ex.errorCode());
    }

    @Test
    void requireSameSchoolPassesForSystemAdmin() {
        RequestContextHolder.setAuth(schoolA, tenantA, UUID.randomUUID(), "root",
                Set.of(PermissionService.SYS_ADMIN));
        // 跨 school 仍允许
        assertDoesNotThrow(() -> svc.requireSameSchool(schoolB));
    }

    @Test
    void requireSchoolAdminPassesForSchoolAdmin() {
        assertDoesNotThrow(() -> svc.requireSchoolAdmin(schoolA));
    }

    @Test
    void requireSchoolAdminFailsForTeacher() {
        RequestContextHolder.setAuth(schoolA, tenantA, UUID.randomUUID(), "t1",
                Set.of(PermissionService.TEACHER));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.requireSchoolAdmin(schoolA));
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode());
    }

    @Test
    void requireSystemAdminBootstrapFailsForSchoolAdmin() {
        BusinessException ex = assertThrows(BusinessException.class,
                svc::requireSystemAdminBootstrap);
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode());
    }

    @Test
    void requireSystemAdminBootstrapPassesForSystemAdmin() {
        RequestContextHolder.setAuth(schoolA, tenantA, UUID.randomUUID(), "root",
                Set.of(PermissionService.SYS_ADMIN));
        assertDoesNotThrow(svc::requireSystemAdminBootstrap);
    }

    @Test
    void requireLoginFailsWhenNoUser() {
        RequestContextHolder.clear();
        BusinessException ex = assertThrows(BusinessException.class, svc::requireLogin);
        assertEquals(ErrorCode.UNAUTHORIZED, ex.errorCode());
    }

    @Test
    void hasAnyRoleWorks() {
        assertTrue(svc.hasAnyRole(PermissionService.SCHOOL_ADMIN, PermissionService.SYS_ADMIN));
        assertFalse(svc.hasAnyRole(PermissionService.STUDENT));
    }
}

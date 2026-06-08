package com.wenda.user.service;

import com.wenda.audit.Audited;
import com.wenda.auth.permission.PermissionService;
import com.wenda.auth.repository.UserCredentialRepository;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.request.IfMatchVerifier;
import com.wenda.user.repository.UserMgmtRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户管理服务（API-USER-001～006 + 权限判定矩阵 v1.0 PERM-SCHOOL-001）。
 */
@Service
public class UserMgmtService {

    private final UserMgmtRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final IfMatchVerifier ifMatchVerifier;

    public UserMgmtService(UserMgmtRepository userRepository,
                           UserCredentialRepository credentialRepository,
                           PasswordEncoder passwordEncoder,
                           PermissionService permissionService,
                           IfMatchVerifier ifMatchVerifier) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
        this.ifMatchVerifier = ifMatchVerifier;
    }

    @Audited(action = "CREATE_USER", resourceType = "user", risk = Audited.Risk.SENSITIVE)
    @Transactional
    public UserMgmtRepository.UserMgmtRow createUser(String username, String displayName,
                                                     String email, String phone, String avatarUrl,
                                                     String userType, String initialPassword) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "username 不能为空。");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "displayName 不能为空。");
        }
        if (userRepository.existsBySchoolAndUsername(schoolId, username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "username 在该学校空间已存在。",
                    List.of(Map.of("conflictField", "username")));
        }
        UUID tenantId = RequestContextHolder.tenantId();
        String hash = initialPassword == null || initialPassword.isBlank()
                ? null : passwordEncoder.encode(initialPassword);
        return userRepository.create(schoolId, tenantId, username, displayName, email, phone,
                avatarUrl, userType == null ? "INTERNAL" : userType,
                RequestContextHolder.userId(), hash);
    }

    public Map<String, Object> listUsers(String keyword, String status, int page, int pageSize) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        long total = userRepository.countBySchool(schoolId, keyword, status);
        var items = userRepository.listBySchool(schoolId, keyword, status, page, pageSize);
        return Map.of(
                "items", items,
                "page", page,
                "pageSize", pageSize,
                "total", total,
                "totalPages", (total + pageSize - 1) / pageSize,
                "sort", "createdAt",
                "order", "desc");
    }

    public UserMgmtRepository.UserMgmtRow getUser(UUID id) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在。"));
        if (!user.schoolId().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCOPE_FORBIDDEN);
        }
        return user;
    }

    @Audited(action = "UPDATE_USER", resourceType = "user", resourceId = "#{#id}",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public UserMgmtRepository.UserMgmtRow updateUser(UUID id, String displayName, String email,
                                                     String phone, String avatarUrl, long ifMatch) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = getUser(id);
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        int n = userRepository.update(id, displayName, email, phone, avatarUrl, current.version(),
                RequestContextHolder.userId());
        if (n == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return userRepository.findById(id).orElseThrow();
    }

    @Audited(action = "BIND_USER_ROLES", resourceType = "user-role-scope", resourceId = "#{#userId}",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public void bindRolesAndScopes(UUID userId, List<UserMgmtRepository.RoleScopeInput> inputs) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var user = getUser(userId);
        userRepository.replaceRoleScopes(userId, schoolId, user.tenantId(), inputs,
                RequestContextHolder.userId());
    }

    @Audited(action = "DISABLE_USER", resourceType = "user", resourceId = "#{#id}",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public void disableUser(UUID id) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var user = getUser(id);
        int n = userRepository.disable(id, RequestContextHolder.userId());
        if (n == 0) throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    @Audited(action = "RESET_USER_PASSWORD", resourceType = "user", resourceId = "#{#id}",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var user = getUser(id);
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "新密码长度不能小于 8。");
        }
        credentialRepository.updatePasswordHash(id, passwordEncoder.encode(newPassword));
    }
}

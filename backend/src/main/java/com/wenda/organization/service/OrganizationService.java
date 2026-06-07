package com.wenda.organization.service;

import com.wenda.audit.Audited;
import com.wenda.auth.permission.PermissionService;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.organization.repository.CollegeRepository;
import com.wenda.organization.repository.SchoolRepository;
import com.wenda.request.IfMatchVerifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 组织服务（基线：API-ORG-001/002/003/004/005/006 + 权限判定矩阵 v1.0
 * PERM-SYS-001 / PERM-SCHOOL-001）。
 */
@Service
public class OrganizationService {

    private final SchoolRepository schoolRepository;
    private final CollegeRepository collegeRepository;
    private final PermissionService permissionService;
    private final IfMatchVerifier ifMatchVerifier;

    public OrganizationService(SchoolRepository schoolRepository,
                               CollegeRepository collegeRepository,
                               PermissionService permissionService,
                               IfMatchVerifier ifMatchVerifier) {
        this.schoolRepository = schoolRepository;
        this.collegeRepository = collegeRepository;
        this.permissionService = permissionService;
        this.ifMatchVerifier = ifMatchVerifier;
    }

    @Audited(action = "CREATE_SCHOOL", resourceType = "school", risk = Audited.Risk.SENSITIVE)
    @Transactional
    public SchoolRepository.SchoolRow createSchool(String code, String name, String shortName,
                                                   String contactEmail, String contactPhone,
                                                   String address, String description) {
        permissionService.requireSystemAdminBootstrap();
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schoolCode 不能为空。");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "name 不能为空。");
        }
        if (schoolRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "schoolCode 已存在。",
                    List.of(Map.of("conflictField", "schoolCode")));
        }
        UUID tenantId = RequestContextHolder.tenantId() == null
                ? UUID.randomUUID() : RequestContextHolder.tenantId();
        return schoolRepository.create(code, name, shortName, contactEmail, contactPhone, address,
                description, tenantId, RequestContextHolder.userId());
    }

    public SchoolRepository.SchoolRow getCurrentSchool() {
        UUID schoolId = RequestContextHolder.schoolId();
        if (schoolId == null) {
            throw new BusinessException(ErrorCode.SCHOOL_SCOPE_REQUIRED);
        }
        permissionService.requireSameSchoolRead(schoolId);
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学校空间不存在。"));
    }

    @Audited(action = "UPDATE_SCHOOL", resourceType = "school", resourceId = "#{#id}",
            risk = Audited.Risk.SENSITIVE)
    @Transactional
    public SchoolRepository.SchoolRow updateSchool(UUID id, String name, String shortName,
                                                   String contactEmail, String contactPhone,
                                                   String address, String description,
                                                   long ifMatch) {
        permissionService.requireSchoolAdmin(id);
        var current = schoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学校空间不存在。"));
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        int n = schoolRepository.update(id, name, shortName, contactEmail, contactPhone, address,
                description, current.version(), RequestContextHolder.userId());
        if (n == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return schoolRepository.findById(id).orElseThrow();
    }

    @Audited(action = "CREATE_COLLEGE", resourceType = "college", risk = Audited.Risk.NORMAL)
    @Transactional
    public CollegeRepository.CollegeRow createCollege(String code, String name, String shortName,
                                                      String description) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "code / name 不能为空。");
        }
        UUID tenantId = RequestContextHolder.tenantId();
        return collegeRepository.create(schoolId, tenantId, code, name, shortName, description,
                RequestContextHolder.userId());
    }

    public Map<String, Object> listColleges(int page, int pageSize) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSameSchoolRead(schoolId);
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        long total = collegeRepository.countBySchool(schoolId);
        var items = collegeRepository.listBySchool(schoolId, page, pageSize);
        return Map.of(
                "items", items,
                "page", page,
                "pageSize", pageSize,
                "total", total,
                "totalPages", (total + pageSize - 1) / pageSize,
                "sort", "createdAt",
                "order", "desc");
    }

    @Audited(action = "UPDATE_COLLEGE", resourceType = "college", resourceId = "#{#id}",
            risk = Audited.Risk.NORMAL)
    @Transactional
    public CollegeRepository.CollegeRow updateCollege(UUID id, String name, String shortName,
                                                      String description, long ifMatch) {
        UUID schoolId = RequestContextHolder.schoolId();
        permissionService.requireSchoolAdmin(schoolId);
        var current = collegeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学院不存在。"));
        if (!current.schoolId().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCOPE_FORBIDDEN);
        }
        ifMatchVerifier.assertVersion(RequestContextHolder.currentRequest(), ifMatch, current.version());
        int n = collegeRepository.update(id, name, shortName, description, current.version(),
                RequestContextHolder.userId());
        if (n == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return collegeRepository.findById(id).orElseThrow();
    }
}

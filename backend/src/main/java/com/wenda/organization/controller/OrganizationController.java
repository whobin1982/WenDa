package com.wenda.organization.controller;

import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.idempotency.Idempotent;
import com.wenda.organization.repository.CollegeRepository;
import com.wenda.organization.repository.SchoolRepository;
import com.wenda.organization.service.OrganizationService;
import com.wenda.request.IfMatchVerifier;
import com.wenda.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 组织 Controller（API-ORG-001～API-ORG-006）。
 */
@RestController
@RequestMapping("/api/v1")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final IfMatchVerifier ifMatchVerifier;

    public OrganizationController(OrganizationService organizationService,
                                  IfMatchVerifier ifMatchVerifier) {
        this.organizationService = organizationService;
        this.ifMatchVerifier = ifMatchVerifier;
    }

    public record CreateSchoolRequest(@NotBlank String schoolCode, @NotBlank String name,
                                      String shortName, String contactEmail, String contactPhone,
                                      String address, String description) {}

    public record UpdateSchoolRequest(String name, String shortName, String contactEmail,
                                      String contactPhone, String address, String description) {}

    public record CreateCollegeRequest(@NotBlank String collegeCode, @NotBlank String name,
                                      String shortName, String description) {}

    public record UpdateCollegeRequest(String name, String shortName, String description) {}

    @Idempotent
    @PostMapping("/schools")
    public ResponseEntity<ApiResponse<SchoolRepository.SchoolRow>> createSchool(
            @Valid @RequestBody CreateSchoolRequest req) {
        var row = organizationService.createSchool(req.schoolCode(), req.name(), req.shortName(),
                req.contactEmail(), req.contactPhone(), req.address(), req.description());
        return ResponseEntity.status(201).body(
                ApiResponse.created(row, RequestContextHolder.requestId()));
    }

    @GetMapping("/schools/current")
    public ResponseEntity<ApiResponse<SchoolRepository.SchoolRow>> currentSchool() {
        return ResponseEntity.ok(
                ApiResponse.ok(organizationService.getCurrentSchool(), RequestContextHolder.requestId()));
    }

    @PatchMapping("/schools/{schoolId}")
    public ResponseEntity<ApiResponse<SchoolRepository.SchoolRow>> updateSchool(
            @PathVariable UUID schoolId, @RequestBody UpdateSchoolRequest req,
            HttpServletRequest request) {
        Long ifMatch = ifMatchVerifier.parseIfMatch(request);
        long v = ifMatch == null ? -1L : ifMatch;
        var row = organizationService.updateSchool(schoolId, req.name(), req.shortName(),
                req.contactEmail(), req.contactPhone(), req.address(), req.description(), v);
        return ResponseEntity.ok(ApiResponse.ok(row, RequestContextHolder.requestId()));
    }

    @Idempotent
    @PostMapping("/colleges")
    public ResponseEntity<ApiResponse<CollegeRepository.CollegeRow>> createCollege(
            @Valid @RequestBody CreateCollegeRequest req) {
        var row = organizationService.createCollege(req.collegeCode(), req.name(), req.shortName(),
                req.description());
        return ResponseEntity.status(201).body(
                ApiResponse.created(row, RequestContextHolder.requestId()));
    }

    @GetMapping("/colleges")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listColleges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(
                ApiResponse.ok(organizationService.listColleges(page, pageSize),
                        RequestContextHolder.requestId()));
    }

    @PatchMapping("/colleges/{collegeId}")
    public ResponseEntity<ApiResponse<CollegeRepository.CollegeRow>> updateCollege(
            @PathVariable UUID collegeId, @RequestBody UpdateCollegeRequest req,
            HttpServletRequest request) {
        Long ifMatch = ifMatchVerifier.parseIfMatch(request);
        long v = ifMatch == null ? -1L : ifMatch;
        var row = organizationService.updateCollege(collegeId, req.name(), req.shortName(),
                req.description(), v);
        return ResponseEntity.ok(ApiResponse.ok(row, RequestContextHolder.requestId()));
    }

    // 兼容 /schools 列表端点空实现
    @GetMapping("/schools")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listSchools(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // MVP-1 学校空间通常只有一个（按 schoolId 区分），此处返回空列表
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "items", List.of(),
                "page", page,
                "pageSize", pageSize,
                "total", 0,
                "totalPages", 0,
                "sort", "createdAt",
                "order", "desc"), RequestContextHolder.requestId()));
    }
}

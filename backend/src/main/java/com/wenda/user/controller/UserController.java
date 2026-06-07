package com.wenda.user.controller;

import com.wenda.context.RequestContextHolder;
import com.wenda.idempotency.Idempotent;
import com.wenda.request.IfMatchVerifier;
import com.wenda.response.ApiResponse;
import com.wenda.user.repository.UserMgmtRepository;
import com.wenda.user.service.UserMgmtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户管理 Controller（API-USER-001～006）。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserMgmtService userService;
    private final IfMatchVerifier ifMatchVerifier;

    public UserController(UserMgmtService userService, IfMatchVerifier ifMatchVerifier) {
        this.userService = userService;
        this.ifMatchVerifier = ifMatchVerifier;
    }

    public record CreateUserRequest(@NotBlank String username, @NotBlank String displayName,
                                    String email, String phone, String avatarUrl, String userType,
                                    String initialPassword) {}

    public record UpdateUserRequest(String displayName, String email, String phone, String avatarUrl) {}

    public record RoleScopeRequest(@NotBlank String roleCode, UUID collegeId, Boolean isPrimary) {}

    public record BindRolesRequest(@NotBlank @Valid List<RoleScopeRequest> roles) {}

    public record ResetPasswordRequest(@NotBlank String newPassword) {}

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<UserMgmtRepository.UserMgmtRow>> create(
            @Valid @RequestBody CreateUserRequest req) {
        var row = userService.createUser(req.username(), req.displayName(), req.email(), req.phone(),
                req.avatarUrl(), req.userType(), req.initialPassword());
        return ResponseEntity.status(201)
                .body(ApiResponse.created(row, RequestContextHolder.requestId()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.listUsers(keyword, status, page, pageSize),
                RequestContextHolder.requestId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserMgmtRepository.UserMgmtRow>> get(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUser(userId),
                RequestContextHolder.requestId()));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserMgmtRepository.UserMgmtRow>> update(
            @PathVariable UUID userId, @RequestBody UpdateUserRequest req,
            HttpServletRequest request) {
        Long ifMatch = ifMatchVerifier.parseIfMatch(request);
        long v = ifMatch == null ? -1L : ifMatch;
        return ResponseEntity.ok(ApiResponse.ok(
                userService.updateUser(userId, req.displayName(), req.email(), req.phone(),
                        req.avatarUrl(), v), RequestContextHolder.requestId()));
    }

    @Idempotent
    @PutMapping("/{userId}/roles-scopes")
    public ResponseEntity<ApiResponse<Void>> bindRoles(@PathVariable UUID userId,
                                                       @RequestBody BindRolesRequest req) {
        var inputs = req.roles().stream()
                .map(r -> new UserMgmtRepository.RoleScopeInput(r.roleCode(), r.collegeId(),
                        Boolean.TRUE.equals(r.isPrimary())))
                .toList();
        userService.bindRolesAndScopes(userId, inputs);
        return ResponseEntity.ok(ApiResponse.ok(null, RequestContextHolder.requestId()));
    }

    @Idempotent
    @PostMapping("/{userId}/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable UUID userId) {
        userService.disableUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, RequestContextHolder.requestId()));
    }

    @Idempotent
    @PostMapping("/{userId}/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable UUID userId,
                                                           @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(userId, req.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null, RequestContextHolder.requestId()));
    }
}

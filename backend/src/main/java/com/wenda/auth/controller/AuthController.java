package com.wenda.auth.controller;

import com.wenda.auth.service.LocalAuthProvider;
import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.idempotency.Idempotent;
import com.wenda.integration.adapter.AuthenticationProvider;
import com.wenda.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller（基线：接口文档 v0.2 / 任务-接口映射表 v1.0 MAP-001～MAP-004）。
 *
 * <ul>
 *   <li>POST /api/v1/auth/login   API-AUTH-001</li>
 *   <li>POST /api/v1/auth/logout  API-AUTH-002</li>
 *   <li>POST /api/v1/auth/refresh API-AUTH-003</li>
 *   <li>GET  /api/v1/auth/me      API-AUTH-004</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LocalAuthProvider localAuthProvider;
    private final AuthenticationProvider authProvider;

    public AuthController(LocalAuthProvider localAuthProvider, AuthenticationProvider authProvider) {
        this.localAuthProvider = localAuthProvider;
        this.authProvider = authProvider;
    }

    public record LoginRequest(@NotBlank String schoolCode, @NotBlank String username,
                               @NotBlank String password) {}

    public record LogoutRequest(String refreshToken) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    // 修复 GOV-002 #3 方案 A：登录不使用幂等。
    // 原因：登录 token 场景不适合缓存首次响应（重复登录返回新 token 是可接受行为）；
    // 缓存登录 token 还会引入安全与会话语义问题（refresh 撤销、token 轮换、IP 校验等
    // 会与缓存冲突）。@Idempotency-Key 留给创建类业务接口使用。
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationProvider.AuthenticatedUser>> login(
            @Valid @RequestBody LoginRequest req) {
        var u = localAuthProvider.authenticate(req.username(), req.password(), req.schoolCode());
        return ResponseEntity.ok(ApiResponse.ok(u, RequestContextHolder.requestId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) LogoutRequest req) {
        if (req == null || req.refreshToken() == null) {
            // 即使没传 token 也视为幂等成功；前端清空本地态即可
            return ResponseEntity.ok(ApiResponse.ok(null, RequestContextHolder.requestId()));
        }
        localAuthProvider.logout(req.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null, RequestContextHolder.requestId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationProvider.AuthenticatedUser>> refresh(
            @Valid @RequestBody RefreshRequest req) {
        var u = authProvider.refresh(req.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(u, RequestContextHolder.requestId()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> me() {
        if (RequestContextHolder.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(localAuthProvider.currentUser(),
                RequestContextHolder.requestId()));
    }
}

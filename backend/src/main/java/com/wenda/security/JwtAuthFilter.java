package com.wenda.security;

import com.wenda.config.WendaProperties;
import com.wenda.context.RequestContextHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT 鉴权过滤器。基线：接口文档 v0.2 §2.1 / §2.2；权限判定矩阵 v1.0 §1（RBAC+Scope+ABAC）。
 *
 * <p>仅做"是谁"——具体"能做什么"由 service 层 {@code PermissionService} 完成。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtProvider jwtProvider;
    private final WendaProperties properties;

    public JwtAuthFilter(JwtProvider jwtProvider, WendaProperties properties) {
        this.jwtProvider = jwtProvider;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 公开端点：登录、刷新、企业邀请 / 公开能力地图、Swagger、Actuator
        return path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/public/")
                || path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring("Bearer ".length()).trim();
        try {
            Claims claims = jwtProvider.parse(token);
            UUID userId = UUID.fromString(claims.get(JwtProvider.CLAIM_USER_ID, String.class));
            String username = claims.get(JwtProvider.CLAIM_USERNAME, String.class);
            UUID schoolId = UUID.fromString(claims.get(JwtProvider.CLAIM_SCHOOL_ID, String.class));
            UUID tenantId = UUID.fromString(claims.get(JwtProvider.CLAIM_TENANT_ID, String.class));
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get(JwtProvider.CLAIM_ROLES);
            Set<String> roleSet = roles == null ? Set.of() : Set.copyOf(roles);

            RequestContextHolder.setAuth(schoolId, tenantId, userId, username, roleSet);

            var auth = new UsernamePasswordAuthenticationToken(
                    username, null,
                    roleSet.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList()));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ExpiredJwtException ex) {
            log.debug("jwt expired: {}", ex.getMessage());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"TOKEN_EXPIRED\",\"message\":\"登录已过期，请重新登录。\""
                            + ",\"requestId\":\"" + RequestContextHolder.requestId() + "\"}");
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("jwt invalid: {}", ex.getMessage());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"未登录或 Token 无效。\""
                            + ",\"requestId\":\"" + RequestContextHolder.requestId() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

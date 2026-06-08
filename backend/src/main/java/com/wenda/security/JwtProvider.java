package com.wenda.security;

import com.wenda.config.WendaProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Provider（基线：LocalAuthProvider 默认实现，架构 v0.3 §6.2 M-01）。
 *
 * <p>密钥长度硬性 ≥ 32 字节；HS256；issuer / TTL 由 {@link WendaProperties.Jwt} 控制。
 * 严禁把密钥硬编码到代码 / 日志 / 仓库。
 */
@Component
public class JwtProvider {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_USERNAME = "uname";
    public static final String CLAIM_SCHOOL_ID = "sid";
    public static final String CLAIM_TENANT_ID = "tid";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TOKEN_TYPE = "typ"; // access | refresh

    private final WendaProperties properties;
    private final SecretKey signingKey;

    public JwtProvider(WendaProperties properties) {
        this.properties = properties;
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("wenda.jwt.secret 长度必须 ≥ 32 字节；当前 " + secret.length + " 字节。");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public String issueAccessToken(UUID userId, String username, UUID schoolId, UUID tenantId, List<String> roles) {
        return issue(userId, username, schoolId, tenantId, roles, "access",
                properties.getJwt().getAccessTtlSeconds());
    }

    public String issueRefreshToken(UUID userId, String username, UUID schoolId, UUID tenantId, List<String> roles) {
        return issue(userId, username, schoolId, tenantId, roles, "refresh",
                properties.getJwt().getRefreshTtlSeconds());
    }

    private String issue(UUID userId, String username, UUID schoolId, UUID tenantId,
                         List<String> roles, String type, long ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .id(UUID.randomUUID().toString())
                .claims(Map.of(
                        CLAIM_USER_ID, userId.toString(),
                        CLAIM_USERNAME, username,
                        CLAIM_SCHOOL_ID, schoolId.toString(),
                        CLAIM_TENANT_ID, tenantId.toString(),
                        CLAIM_ROLES, roles,
                        CLAIM_TOKEN_TYPE, type))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .requireIssuer(properties.getJwt().getIssuer())
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long accessTtlSeconds() {
        return properties.getJwt().getAccessTtlSeconds();
    }

    public long refreshTtlSeconds() {
        return properties.getJwt().getRefreshTtlSeconds();
    }
}

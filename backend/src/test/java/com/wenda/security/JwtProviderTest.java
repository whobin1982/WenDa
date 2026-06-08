package com.wenda.security;

import com.wenda.config.WendaProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT Provider 单元测试（基线：架构 v0.3 §6.2 M-01 + 接口文档 v0.2 §2.1）。
 */
class JwtProviderTest {

    private WendaProperties properties() {
        WendaProperties p = new WendaProperties();
        p.getJwt().setSecret("a_very_long_test_secret_for_unit_tests_only_32_bytes_xx");
        p.getJwt().setIssuer("wenda.test");
        p.getJwt().setAccessTtlSeconds(60);
        p.getJwt().setRefreshTtlSeconds(120);
        return p;
    }

    @Test
    void issueAndParseAccessToken() {
        JwtProvider provider = new JwtProvider(properties());
        UUID uid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        UUID tid = UUID.randomUUID();
        String token = provider.issueAccessToken(uid, "alice", sid, tid, List.of("SCHOOL_ADMIN"));
        assertNotNull(token);
        Claims c = provider.parse(token);
        assertEquals(uid.toString(), c.get(JwtProvider.CLAIM_USER_ID, String.class));
        assertEquals("alice", c.get(JwtProvider.CLAIM_USERNAME, String.class));
        assertEquals(sid.toString(), c.get(JwtProvider.CLAIM_SCHOOL_ID, String.class));
        assertEquals(tid.toString(), c.get(JwtProvider.CLAIM_TENANT_ID, String.class));
        assertEquals("access", c.get(JwtProvider.CLAIM_TOKEN_TYPE, String.class));
        assertTrue(c.get(JwtProvider.CLAIM_ROLES, List.class).contains("SCHOOL_ADMIN"));
    }

    @Test
    void issueAndParseRefreshToken() {
        JwtProvider provider = new JwtProvider(properties());
        String token = provider.issueRefreshToken(UUID.randomUUID(), "u", UUID.randomUUID(),
                UUID.randomUUID(), List.of("STUDENT"));
        Claims c = provider.parse(token);
        assertEquals("refresh", c.get(JwtProvider.CLAIM_TOKEN_TYPE, String.class));
    }

    @Test
    void shortSecretRejectedAtStartup() {
        WendaProperties p = properties();
        p.getJwt().setSecret("short");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtProvider(p));
        assertTrue(ex.getMessage().contains("32 字节"));
    }
}

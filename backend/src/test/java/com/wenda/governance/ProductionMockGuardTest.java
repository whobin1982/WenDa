package com.wenda.governance;

import com.wenda.config.WendaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ProductionMockGuard 启动校验测试（基线：技术方案 v0.4 §10.4 + RG-OSG-007）。
 */
class ProductionMockGuardTest {

    @Test
    void nonProdProfileSkips() {
        WendaProperties p = propsWithMocks();
        var env = new MockEnvironment();
        env.setActiveProfiles("dev");
        var guard = new com.wenda.config.ProductionMockGuard(p, env);
        assertDoesNotThrow(guard::validate);
    }

    @Test
    void prodProfileWithMockStorageBlocked() {
        WendaProperties p = propsWithMocks();
        p.getAdapter().setStorage("mockStorage");
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var guard = new com.wenda.config.ProductionMockGuard(p, env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);
        assertTrue(ex.getMessage().contains("storage"));
    }

    @Test
    void prodProfileWithMockAiBlocked() {
        WendaProperties p = propsWithMocks();
        p.getAdapter().setAiProvider("MockAI");
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var guard = new com.wenda.config.ProductionMockGuard(p, env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);
        assertTrue(ex.getMessage().contains("ai-provider"));
    }

    @Test
    void prodProfileWithOpenSourceAdaptersPasses() {
        WendaProperties p = new WendaProperties();
        p.getAdapter().setAiProvider("external-validated");
        p.getAdapter().setStorage("cloud-or-minio");
        p.getAdapter().setScanner("clamav");
        p.getAdapter().setRenderer("opensource");
        p.getAdapter().setEmail("smtp");
        p.getSecurity().setProdMockDisabled(true);
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var guard = new com.wenda.config.ProductionMockGuard(p, env);
        assertDoesNotThrow(guard::validate);
    }

    @Test
    void prodProfileWithMockDisabledFlagFalseBlocked() {
        WendaProperties p = propsWithMocks();
        p.getSecurity().setProdMockDisabled(false);
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var guard = new com.wenda.config.ProductionMockGuard(p, env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);
        assertTrue(ex.getMessage().contains("prod-mock-disabled"));
    }

    private static WendaProperties propsWithMocks() {
        WendaProperties p = new WendaProperties();
        p.getSecurity().setProdMockDisabled(true);
        return p;
    }
}

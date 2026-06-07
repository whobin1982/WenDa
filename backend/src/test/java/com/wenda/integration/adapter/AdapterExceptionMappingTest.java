package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Adapter 错误码转换测试（基线：技术方案 v0.4 §9.2 第 4 条）。
 */
class AdapterExceptionMappingTest {

    @Test
    void aiProviderAdapterMapsToDependencyUnavailable() {
        var ex = AIProviderAdapter.toBusinessException("external", "generate",
                new RuntimeException("upstream timeout"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void storageAdapterMapsToDependencyUnavailable() {
        var ex = FileStorageAdapter.toBusinessException("upload", new RuntimeException("503"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void scannerAdapterMapsToDependencyUnavailable() {
        var ex = FileSecurityScannerAdapter.toBusinessException("scan", new RuntimeException("boom"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void rendererAdapterMapsToDependencyUnavailable() {
        var ex = ReportRendererAdapter.toBusinessException("render", new RuntimeException("oom"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void knowledgeSearchMapsToDependencyUnavailable() {
        var ex = KnowledgeSearchAdapter.toBusinessException("search", new RuntimeException("err"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void emailAdapterMapsToDependencyUnavailable() {
        var ex = EmailNotificationAdapter.toBusinessException("send", new RuntimeException("smtp"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void taskQueueAdapterMapsToDependencyUnavailable() {
        var ex = TaskQueueAdapter.toBusinessException("enqueue", new RuntimeException("pg"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void authProviderMapsToUnauthorized() {
        var ex = AuthenticationProvider.toBusinessException("login", new RuntimeException("bad creds"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.errorCode());
    }

    @Test
    void passingBusinessExceptionIsReused() {
        BusinessException original = new BusinessException(ErrorCode.AI_PROVIDER_DISABLED);
        BusinessException mapped = AIProviderAdapter.toBusinessException("p", "g", original);
        assertSame(original, mapped);
    }
}

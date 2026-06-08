package com.wenda.integration.adapter;

import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;

import java.util.Map;

/**
 * AI Provider Adapter（基线：技术方案 v0.4 §9.1 + 架构 v0.3 §4.2 ARCH-DEC-008）。
 *
 * <p>业务侧只能依赖本接口；具体厂商 SDK 隐藏在 {@code infrastructure} 包下。默认实现由
 * Spring Profile 注入；生产禁用 Mock（基线 RG-OSG-007）。
 *
 * <p>实现约束（ArchUnit 测试断言）：
 * <ol>
 *   <li>所有实现类必须位于 {@code com.wenda.integration.adapter.ai} 子包；</li>
 *   <li>业务模块（{@code com.wenda.auth/organization/user/role/settings/...}）不得依赖
 *       具体实现类，只能依赖本接口；</li>
 *   <li>Mock 实现类名必须包含 {@code Mock}（用于生产环境禁用扫描）。</li>
 * </ol>
 */
public interface AIProviderAdapter {

    /**
     * 调用 AI 模型生成结果。
     *
     * @param request 业务侧统一请求体（Prompt / Schema / Provider / Model 标识）
     * @return AI 返回的原始输出；调用方负责 Schema 校验
     * @throws BusinessException AI_PROVIDER_DISABLED / AI_OUTPUT_SCHEMA_INVALID / AI_TASK_FAILED /
     *                           DEPENDENCY_UNAVAILABLE
     */
    String generate(AIRequest request);

    record AIRequest(
            String providerCode,
            String modelId,
            String prompt,
            String schemaName,
            String schemaVersion,
            Map<String, Object> variables,
            boolean allowStudentDataOutbound) {}

    record AIResponse(
            String providerCode,
            String modelId,
            String content,
            String promptVersion,
            String schemaVersion,
            long latencyMs,
            String requestId) {}

    /** Adapter 失败必须转换为统一错误码，不向上泄露厂商异常。 */
    static BusinessException toBusinessException(String providerCode, String op, Throwable t) {
        if (t instanceof BusinessException be) return be;
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "AI Provider " + providerCode + " " + op + " 失败。");
    }
}

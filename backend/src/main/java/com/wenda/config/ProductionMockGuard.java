package com.wenda.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 生产 Mock 禁用启动校验（基线：技术方案 v0.4 §10.4 + RG-OSG-007）。
 *
 * <p>在 {@code prod} profile 下：
 * <ol>
 *   <li>{@code wenda.security.prod-mock-disabled} 必须为 true；</li>
 *   <li>{@code wenda.adapter.ai-provider} / {@code scanner} / {@code renderer} /
 *       {@code storage} 不得是 {@code mock*} / {@code disabled-with-mock}；</li>
 *   <li>{@code wenda.adapter.email} 不得是 {@code mock}。</li>
 * </ol>
 *
 * <p>违反任一条件直接阻断 Spring 启动。
 */
@Component
public class ProductionMockGuard {

    private static final Logger log = LoggerFactory.getLogger(ProductionMockGuard.class);

    private final WendaProperties properties;
    private final Environment env;

    public ProductionMockGuard(WendaProperties properties, Environment env) {
        this.properties = properties;
        this.env = env;
    }

    @PostConstruct
    public void validate() {
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (!isProd) {
            log.info("ProductionMockGuard 跳过（非 prod profile：{}）",
                    Arrays.toString(env.getActiveProfiles()));
            return;
        }
        if (!properties.getSecurity().isProdMockDisabled()) {
            throw new IllegalStateException("生产环境禁止关闭 prod-mock-disabled 开关（基线 RG-OSG-007）。");
        }
        validateAdapter("ai-provider", properties.getAdapter().getAiProvider());
        validateAdapter("scanner", properties.getAdapter().getScanner());
        validateAdapter("renderer", properties.getAdapter().getRenderer());
        validateAdapter("storage", properties.getAdapter().getStorage());
        validateAdapter("email", properties.getAdapter().getEmail());
        log.info("ProductionMockGuard 校验通过：prod profile 下所有 Adapter 均未启用 Mock。");
    }

    private static void validateAdapter(String name, String value) {
        if (value == null) return;
        String v = value.toLowerCase();
        if (v.startsWith("mock") || v.contains("simplemock") || v.endsWith("-mock")) {
            throw new IllegalStateException(
                    "生产环境禁止启用 Mock Adapter：wenda.adapter." + name + "=" + value
                            + "（基线 RG-OSG-007）。");
        }
    }
}

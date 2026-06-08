package com.wenda.idempotency;

import com.wenda.config.WendaProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 {@link IdempotencyInterceptor}（基线：接口文档 v0.2 §2.5）。
 */
@Configuration
public class IdempotencyWebConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor interceptor;
    private final WendaProperties properties;

    public IdempotencyWebConfig(IdempotencyInterceptor interceptor, WendaProperties properties) {
        this.interceptor = interceptor;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (properties.getIdempotency().isEnabled()) {
            registry.addInterceptor(interceptor);
        }
    }
}

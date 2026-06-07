package com.wenda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一应用配置（基线：基线索引 v1.0 §6 + 接口文档 v0.2 §2.1 / §2.2 / §2.5）。
 *
 * <p>包含请求头、JWT、审计、幂等、Adapter 默认实现、生产 Mock 禁用等开关。
 */
@ConfigurationProperties(prefix = "wenda")
public class WendaProperties {

    private Request request = new Request();
    private Jwt jwt = new Jwt();
    private Audit audit = new Audit();
    private Idempotency idempotency = new Idempotency();
    private Adapter adapter = new Adapter();
    private Security security = new Security();

    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

    public Audit getAudit() { return audit; }
    public void setAudit(Audit audit) { this.audit = audit; }

    public Idempotency getIdempotency() { return idempotency; }
    public void setIdempotency(Idempotency idempotency) { this.idempotency = idempotency; }

    public Adapter getAdapter() { return adapter; }
    public void setAdapter(Adapter adapter) { this.adapter = adapter; }

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Request {
        private Header header = new Header();
        private int requestIdMaxLen = 64;
        public Header getHeader() { return header; }
        public void setHeader(Header header) { this.header = header; }
        public int getRequestIdMaxLen() { return requestIdMaxLen; }
        public void setRequestIdMaxLen(int v) { this.requestIdMaxLen = v; }
    }

    public static class Header {
        private String requestId = "X-Request-Id";
        private String idempotencyKey = "Idempotency-Key";
        private String ifMatch = "If-Match";
        private String acceptLanguage = "Accept-Language";
        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String v) { this.idempotencyKey = v; }
        public String getIfMatch() { return ifMatch; }
        public void setIfMatch(String v) { this.ifMatch = v; }
        public String getAcceptLanguage() { return acceptLanguage; }
        public void setAcceptLanguage(String v) { this.acceptLanguage = v; }
    }

    public static class Jwt {
        private String issuer = "wenda.local";
        private String secret = "change_me_to_a_32_byte_random_string_at_least_please_xx";
        private long accessTtlSeconds = 3600;
        private long refreshTtlSeconds = 1209600;
        public String getIssuer() { return issuer; }
        public void setIssuer(String v) { this.issuer = v; }
        public String getSecret() { return secret; }
        public void setSecret(String v) { this.secret = v; }
        public long getAccessTtlSeconds() { return accessTtlSeconds; }
        public void setAccessTtlSeconds(long v) { this.accessTtlSeconds = v; }
        public long getRefreshTtlSeconds() { return refreshTtlSeconds; }
        public void setRefreshTtlSeconds(long v) { this.refreshTtlSeconds = v; }
    }

    public static class Audit {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
    }

    public static class Idempotency {
        private boolean enabled = true;
        private long ttlSeconds = 86400;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long v) { this.ttlSeconds = v; }
    }

    public static class Adapter {
        private String aiProvider = "disabled";
        private String storage = "local";
        private String scanner = "disabled";
        private String renderer = "disabled";
        private String email = "disabled";
        public String getAiProvider() { return aiProvider; }
        public void setAiProvider(String v) { this.aiProvider = v; }
        public String getStorage() { return storage; }
        public void setStorage(String v) { this.storage = v; }
        public String getScanner() { return scanner; }
        public void setScanner(String v) { this.scanner = v; }
        public String getRenderer() { return renderer; }
        public void setRenderer(String v) { this.renderer = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { this.email = v; }
    }

    public static class Security {
        private boolean prodMockDisabled = true;
        public boolean isProdMockDisabled() { return prodMockDisabled; }
        public void setProdMockDisabled(boolean v) { this.prodMockDisabled = v; }
    }
}

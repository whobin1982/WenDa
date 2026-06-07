package com.wenda.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * 在 Servlet 层把请求 body 一次性缓存为 byte[] 并包装为
 * {@link CachedBodyHttpServletRequest}，使下游的 {@code IdempotencyInterceptor}
 * 与 Controller（{@code @RequestBody} 解析器）都能独立读取 body。
 *
 * <p>基线：接口文档 v0.2 §2.5 通用幂等规则 + §2.2 {@code Idempotency-Key} 头。
 *
 * <p>实现说明：
 * <ul>
 *   <li>优先级仅次于 {@code RequestContextFilter}（{@code HIGHEST_PRECEDENCE + 5}）；</li>
 *   <li>对 multipart/form-data 不做缓存（由 Spring 单独处理；后续如需支持幂等，
 *       在该分支用 part count + part name 替代 body hash）；</li>
 *   <li>对非 multipart 请求一次性 readAllBytes，缓存到 byte[]；</li>
 *   <li>所有下游调用 {@code getInputStream()} / {@code getReader()} 都从缓存返回，
 *       不会触发原始流"被消费"问题。</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class IdempotencyRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
        if (isMultipart) {
            chain.doFilter(request, response);
            return;
        }
        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);
        chain.doFilter(cached, response);
    }

    /**
     * 请求体一次性缓存的 HttpServletRequest 包装。
     */
    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            try (var in = request.getInputStream()) {
                this.cachedBody = in.readAllBytes();
            } catch (IOException ex) {
                throw ex;
            }
        }

        public byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return byteStream.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener readListener) { /* no-op */ }
                @Override
                public int read() { return byteStream.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            Charset cs = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(getInputStream(), cs));
        }
    }
}

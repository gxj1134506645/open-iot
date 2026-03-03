package com.openiot.common.observability.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Trace ID 过滤器
 *
 * <p>为每个 HTTP 请求生成或传播 Trace ID，并注入到 SLF4J MDC 中。
 * 这样所有日志都会自动包含 Trace ID，便于日志关联和问题排查。
 *
 * <h3>Trace ID 来源优先级：</h3>
 * <ol>
 *   <li>请求头 X-Trace-Id（从上游服务传播）</li>
 *   <li>请求头 traceparent（W3C Trace Context 标准）</li>
 *   <li>自动生成新的 Trace ID（UUID 格式）</li>
 * </ol>
 *
 * <p><b>注意：</b>此类仅在 Servlet 环境下加载，WebFlux 环境下不会加载。
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Component
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 自定义 Trace ID 请求头
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * W3C Trace Context 请求头
     */
    public static final String TRACE_PARENT_HEADER = "traceparent";

    /**
     * MDC 中的 Trace ID 键名
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = extractOrGenerateTraceId(request);

        // 将 Trace ID 放入 MDC
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        // 将 Trace ID 添加到响应头，便于客户端关联
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC，防止线程池复用导致的污染
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 从请求中提取 Trace ID，如果不存在则生成新的
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        // 1. 优先从自定义请求头获取
        String traceId = request.getHeader(TRACE_ID_HEADER);

        // 2. 尝试从 W3C Trace Context 获取
        if (traceId == null || traceId.isBlank()) {
            traceId = extractFromTraceParent(request.getHeader(TRACE_PARENT_HEADER));
        }

        // 3. 生成新的 Trace ID
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        return traceId;
    }

    /**
     * 从 W3C traceparent 头中提取 Trace ID
     *
     * <p>格式：version-traceid-parentid-flags
     * <p>示例：00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
     */
    private String extractFromTraceParent(String traceParent) {
        if (traceParent == null || traceParent.isBlank()) {
            return null;
        }

        String[] parts = traceParent.split("-");
        if (parts.length >= 2 && "00".equals(parts[0])) {
            return parts[1]; // 返回 trace-id 部分
        }

        return null;
    }

    /**
     * 生成新的 Trace ID
     *
     * <p>使用 UUID 格式（32 位小写十六进制）
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}

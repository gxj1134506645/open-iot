package com.openiot.common.observability.filter;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.openiot.common.security.context.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Tenant ID MDC 过滤器
 *
 * <p>将当前请求的租户 ID 注入到 SLF4J MDC 中，以便日志可以按租户进行查询和过滤。
 *
 * <h3>租户 ID 来源优先级：</h3>
 * <ol>
 *   <li>TenantContext（已解析的租户上下文）</li>
 *   <li>请求头 X-Tenant-Id（Gateway 注入）</li>
 * </ol>
 *
 * <p><b>注意：</b>此类仅在 Servlet 环境下加载，WebFlux 环境下不会加载。
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantIdMdcFilter extends OncePerRequestFilter {

    /**
     * 租户 ID 请求头
     */
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    /**
     * MDC 中的租户 ID 键名
     */
    public static final String TENANT_ID_MDC_KEY = "tenantId";

    /**
     * MDC 中的用户 ID 键名
     */
    public static final String USER_ID_MDC_KEY = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 注入租户 ID
            String tenantId = extractTenantId(request);
            if (tenantId != null) {
                MDC.put(TENANT_ID_MDC_KEY, tenantId);
            }

            // 注入用户 ID（如果可用）
            String userId = extractUserId(request);
            if (userId != null) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }

            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC
            MDC.remove(TENANT_ID_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }

    /**
     * 提取租户 ID
     */
    private String extractTenantId(HttpServletRequest request) {
        // 1. 优先从 TenantContext 获取（已通过 Gateway 认证）
        String tenantId = TenantContext.getTenantId();

        // 2. 从请求头获取（备用）
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getHeader(TENANT_ID_HEADER);
        }

        return tenantId;
    }

    /**
     * 提取用户 ID
     */
    private String extractUserId(HttpServletRequest request) {
        // 从 TenantContext 获取用户 ID
        // 注意：这里假设 TenantContext 有 getUserId 方法
        // 如果没有，可以从 Sa-Token 的 StpUtil 获取
        try {
            Object userId = request.getAttribute("userId");
            return userId != null ? userId.toString() : null;
        } catch (Exception e) {
            log.debug("Failed to extract user id: {}", e.getMessage());
            return null;
        }
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

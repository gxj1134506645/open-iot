package com.openiot.common.security.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.openiot.common.security.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 租户上下文过滤器
 * 从请求头或 Sa-Token Session 中提取租户信息并设置到上下文
 * 优先级：HTTP Headers > Sa-Token Session
 */
@Slf4j
@Component
@Order(1)
public class TenantContextFilter implements Filter {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 1. 优先从请求头提取租户信息（由网关注入）
            String tenantId = httpRequest.getHeader(TENANT_ID_HEADER);
            String userId = httpRequest.getHeader(USER_ID_HEADER);
            String role = httpRequest.getHeader(USER_ROLE_HEADER);

            // 2. 如果 headers 为空，尝试从 Sa-Token Session 读取（直接调用服务的情况）
            if (tenantId == null && userId == null && StpUtil.isLogin()) {
                try {
                    // 从 Session 中获取租户信息
                    Object sessionTenantId = StpUtil.getSession().get("tenantId");
                    Object sessionUserId = StpUtil.getSession().get("userId");
                    Object sessionRole = StpUtil.getSession().get("roles");

                    if (sessionTenantId != null) {
                        tenantId = String.valueOf(sessionTenantId);
                    }
                    if (sessionUserId != null) {
                        userId = String.valueOf(sessionUserId);
                    }
                    // roles 可能是 List 或 String
                    if (sessionRole != null) {
                        if (sessionRole instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) sessionRole;
                            role = roles.isEmpty() ? null : roles.get(0);
                        } else {
                            role = String.valueOf(sessionRole);
                        }
                    }

                    log.debug("从 Sa-Token Session 读取租户上下文: tenantId={}, userId={}, role={}",
                            tenantId, userId, role);
                } catch (Exception e) {
                    log.warn("读取 Sa-Token Session 失败: {}", e.getMessage());
                }
            }

            // 3. 设置租户上下文
            if (tenantId != null || userId != null) {
                TenantContext.TenantInfo info = new TenantContext.TenantInfo();
                info.setTenantId(tenantId);
                info.setUserId(userId);
                info.setRole(role);
                TenantContext.setTenant(info);

                log.debug("设置租户上下文: tenantId={}, userId={}, role={}", tenantId, userId, role);
            }

            chain.doFilter(request, response);
        } finally {
            // 清除上下文
            TenantContext.clear();
        }
    }
}

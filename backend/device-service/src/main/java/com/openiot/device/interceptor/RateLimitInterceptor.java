package com.openiot.device.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.config.RateLimitConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API 限流拦截器
 * <p>
 * 基于 IP 地址的限流实现，使用 Guava RateLimiter 令牌桶算法。
 * 当请求超过配置的阈值时，返回 HTTP 429 状态码。
 * </p>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *     <li>从请求中获取客户端 IP 地址</li>
 *     <li>根据 IP 获取或创建对应的 RateLimiter</li>
 *     <li>尝试获取令牌，成功则放行，失败则返回 429</li>
 * </ol>
 *
 * <h3>IP 获取顺序：</h3>
 * <ol>
 *     <li>X-Forwarded-For 头（第一个 IP）</li>
 *     <li>X-Real-IP 头</li>
 *     <li>Proxy-Client-IP 头</li>
 *     <li>WL-Proxy-Client-IP 头</li>
 *     <li>HTTP_CLIENT_IP 头</li>
 *     <li>HTTP_X_FORWARDED_FOR 头</li>
 *     <li>request.getRemoteAddr()</li>
 * </ol>
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    /**
     * 限流状态码
     */
    private static final int TOO_MANY_REQUESTS = 429;

    /**
     * 限流错误消息
     */
    private static final String RATE_LIMIT_MESSAGE = "请求过于频繁，请稍后重试";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取客户端 IP
        String clientIp = getClientIp(request);

        // 尝试获取限流许可
        boolean acquired = rateLimitConfig.tryAcquire(clientIp);

        if (acquired) {
            // 获取成功，继续处理请求
            return true;
        }

        // 获取失败，触发限流
        handleRateLimitExceeded(request, response, clientIp);
        return false;
    }

    /**
     * 处理限流超限情况
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param clientIp 客户端 IP
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, String clientIp) throws IOException {
        String requestUri = request.getRequestURI();

        log.warn("API 限流触发: ip={}, uri={}, permitsPerSecond={}",
                clientIp, requestUri, rateLimitConfig.getPermitsPerSecond());

        // 设置响应头
        response.setStatus(TOO_MANY_REQUESTS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 添加限流相关响应头
        response.setHeader("X-RateLimit-Limit", String.valueOf((int) rateLimitConfig.getPermitsPerSecond()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("Retry-After", "1");  // 建议 1 秒后重试

        // 构建错误响应
        ApiResponse<Void> apiResponse = ApiResponse.error(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                RATE_LIMIT_MESSAGE
        );

        // 写入响应
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    /**
     * 获取客户端真实 IP 地址
     * 支持代理服务器场景
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = null;

        // 1. 检查 X-Forwarded-For 头（可能包含多个 IP，取第一个）
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (isValidIp(xForwardedFor)) {
            // X-Forwarded-For 格式: client, proxy1, proxy2
            ip = xForwardedFor.split(",")[0].trim();
            return ip;
        }

        // 2. 检查其他常见代理头
        String[] headerNames = {
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            if (isValidIp(headerValue)) {
                return headerValue.trim();
            }
        }

        // 3. 最后使用 RemoteAddr
        ip = request.getRemoteAddr();

        // 处理 IPv6 本地地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * 检查 IP 地址是否有效
     *
     * @param ip IP 地址字符串
     * @return true 表示有效
     */
    private boolean isValidIp(String ip) {
        return ip != null
                && !ip.isEmpty()
                && !"unknown".equalsIgnoreCase(ip)
                && !"null".equalsIgnoreCase(ip);
    }
}

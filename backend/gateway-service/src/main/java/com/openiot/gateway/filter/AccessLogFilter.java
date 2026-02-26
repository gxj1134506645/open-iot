package com.openiot.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 访问日志过滤器
 * 记录所有请求的访问日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";
    private static final String TRACE_ID = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 生成链路追踪 ID
        String traceId = UUID.randomUUID().toString().replace("-", "");

        // 记录开始时间
        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());
        exchange.getAttributes().put(TRACE_ID, traceId);

        // 获取请求信息
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        String userId = request.getHeaders().getFirst("X-User-Id");
        String clientIp = getClientIp(request);

        // 记录请求日志
        log.info("[{}] 请求开始: {} {} - tenant={}, user={}, ip={}",
                traceId, method, path, tenantId, userId, clientIp);

        // 添加 traceId 到请求头
        ServerHttpRequest newRequest = request.mutate()
                .header("X-Trace-Id", traceId)
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build())
                .then(Mono.fromRunnable(() -> {
                    // 计算耗时
                    Long startTime = exchange.getAttribute(START_TIME);
                    if (startTime != null) {
                        long duration = System.currentTimeMillis() - startTime;
                        int statusCode = exchange.getResponse().getStatusCode() != null ?
                                exchange.getResponse().getStatusCode().value() : 0;

                        log.info("[{}] 请求完成: {} {} - status={}, duration={}ms",
                                traceId, method, path, statusCode, duration);
                    }
                }));
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        // 处理多个 IP 的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public int getOrder() {
        return -200; // 在认证过滤器之前执行
    }
}

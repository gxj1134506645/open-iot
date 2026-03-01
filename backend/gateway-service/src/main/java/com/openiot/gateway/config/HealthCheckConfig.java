package com.openiot.gateway.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gateway 服务健康检查配置
 *
 * <p>提供详细的健康检查指标：
 * <ul>
 *   <li>Redis 连接状态</li>
 *   <li>路由配置状态</li>
 *   <li>下游服务可达性</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class HealthCheckConfig {

    /**
     * Redis 健康检查指示器
     */
    @Component
    @RequiredArgsConstructor
    public static class RedisHealthIndicator implements HealthIndicator {

        private final RedisConnectionFactory redisConnectionFactory;

        @Override
        public Health health() {
            try {
                // 尝试获取 Redis 连接并执行 PING 命令
                var connection = redisConnectionFactory.getConnection();
                String pong = connection.ping();
                connection.close();

                if ("PONG".equalsIgnoreCase(pong)) {
                    return Health.up()
                            .withDetail("connection", "active")
                            .withDetail("response", pong)
                            .build();
                } else {
                    return Health.down()
                            .withDetail("error", "Unexpected response: " + pong)
                            .build();
                }
            } catch (Exception e) {
                log.error("Redis health check failed", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * 路由健康检查指示器
     */
    @Component
    @RequiredArgsConstructor
    public static class RouteHealthIndicator implements HealthIndicator {

        private final RouteLocator routeLocator;

        @Override
        public Health health() {
            try {
                // 检查路由配置
                var routes = routeLocator.getRoutes().collectList().block();
                int routeCount = routes != null ? routes.size() : 0;

                if (routeCount > 0) {
                    return Health.up()
                            .withDetail("routeCount", routeCount)
                            .withDetail("status", "configured")
                            .build();
                } else {
                    return Health.status(Status.UNKNOWN)
                            .withDetail("routeCount", 0)
                            .withDetail("warning", "No routes configured")
                            .build();
                }
            } catch (Exception e) {
                log.error("Route health check failed", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * JVM 健康检查指示器
     */
    @Component
    public static class JvmHealthIndicator implements HealthIndicator {

        private static final long MAX_HEAP_USAGE_PERCENT = 90;

        @Override
        public Health health() {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double heapUsagePercent = (double) usedMemory / maxMemory * 100;

            Health.Builder builder;
            if (heapUsagePercent > MAX_HEAP_USAGE_PERCENT) {
                builder = Health.status(new Status("WARNING", "High heap usage"));
            } else {
                builder = Health.up();
            }

            return builder
                    .withDetail("maxMemory", formatBytes(maxMemory))
                    .withDetail("totalMemory", formatBytes(totalMemory))
                    .withDetail("freeMemory", formatBytes(freeMemory))
                    .withDetail("usedMemory", formatBytes(usedMemory))
                    .withDetail("heapUsagePercent", String.format("%.2f%%", heapUsagePercent))
                    .withDetail("availableProcessors", runtime.availableProcessors())
                    .build();
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "B";
            return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
        }
    }
}

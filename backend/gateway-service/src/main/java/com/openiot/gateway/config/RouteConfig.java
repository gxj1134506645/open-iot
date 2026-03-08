package com.openiot.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 路由配置类
 * <p>
 * 配置网关路由规则，支持动态路由刷新
 * </p>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Configuration
public class RouteConfig {

    /**
     * 配置路由规则
     * <p>
     * 路由规则说明：
     * 1. tenant-service: 租户管理、用户管理、认证服务
     * 2. device-service: 设备管理、设备数据上报
     * 3. data-service: 数据处理、实时推送、历史重放
     * </p>
     *
     * @param builder 路由构建器
     * @return 路由定位器
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 租户服务路由
                .route("tenant-service", r -> r
                        .path("/api/v1/tenants/**", "/api/v1/users/**", "/api/v1/auth/**")
                        .filters(f -> f
                                // 添加请求头，传递租户信息
                                .addRequestHeader("X-Gateway-Time", String.valueOf(System.currentTimeMillis()))
                                // 添加响应头，标识网关处理
                                .addResponseHeader("X-Gateway-Name", "openiot-gateway")
                        )
                        .uri("lb://tenant-service")
                )
                // 设备服务路由（包含设备管理、产品管理、告警管理）
                .route("device-service", r -> r
                        .path("/api/v1/devices/**", "/api/products/**", "/api/v1/products/**", "/api/v1/alerts/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway-Time", String.valueOf(System.currentTimeMillis()))
                                .addResponseHeader("X-Gateway-Name", "openiot-gateway")
                        )
                        .uri("lb://device-service")
                )
                // 数据服务路由
                .route("data-service", r -> r
                        .path("/api/v1/data/**", "/api/v1/replay/**", "/api/v1/sse/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway-Time", String.valueOf(System.currentTimeMillis()))
                                .addResponseHeader("X-Gateway-Name", "openiot-gateway")
                        )
                        .uri("lb://data-service")
                )
                .build();
    }
}

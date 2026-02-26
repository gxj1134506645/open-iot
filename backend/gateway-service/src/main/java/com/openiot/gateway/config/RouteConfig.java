package com.openiot.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置类
 * 可通过代码方式定义路由规则（与 application.yml 中的配置互为补充）
 */
@Configuration
public class RouteConfig {

    /**
     * 自定义路由配置
     * 注意：此配置与 application.yml 中的路由配置同时生效
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 设备服务路由
                .route("device-service", r -> r
                        .path("/api/v1/devices/**", "/api/v1/auth/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Time", String.valueOf(System.currentTimeMillis())))
                        .uri("lb://device-service"))
                // 租户服务路由
                .route("tenant-service", r -> r
                        .path("/api/v1/tenants/**", "/api/v1/users/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tenant-service"))
                // 数据服务路由
                .route("data-service", r -> r
                        .path("/api/v1/data/**", "/api/v1/replay/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://data-service"))
                .build();
    }
}

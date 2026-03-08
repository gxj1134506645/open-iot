package com.openiot.device.config;

import com.openiot.device.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 配置拦截器、跨域、静态资源等
 * </p>
 *
 * @author open-iot
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 是否启用限流
     */
    @Value("${openiot.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * 配置拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitEnabled) {
            registry.addInterceptor(rateLimitInterceptor)
                    .addPathPatterns("/api/**")  // 拦截所有 API 请求
                    .excludePathPatterns(
                            // 排除不需要限流的路径
                            "/api/v1/devices/*/trajectory/stream",  // SSE 端点
                            "/api/sse/**",                          // SSE 端点
                            "/api/actuator/**",                     // 健康检查端点
                            "/api/swagger-ui/**",                   // Swagger UI
                            "/api/v3/api-docs/**",                  // OpenAPI 文档
                            "/api/doc.html",                        // Knife4j 文档
                            "/api/webjars/**"                       // 静态资源
                    )
                    .order(1);  // 优先级，数值越小优先级越高

            WebMvcConfigurer.super.addInterceptors(registry);
        }
    }
}

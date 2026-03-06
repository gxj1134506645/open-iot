package com.openiot.common.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * <p>
 * 配置 Sa-Token 拦截器，用于权限校验
 * </p>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "openiot.security", name = "auth-enabled", havingValue = "true", matchIfMissing = false)
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器
     * <p>
     * 校验规则：
     * 1. 所有 API 请求都需要登录（除了白名单路径）
     * 2. 权限校验通过 StpInterfaceImpl 实现
     * </p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 是否登录
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 排除登录接口
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        // 排除健康检查接口
                        "/actuator/**",
                        // 排除 Swagger 文档
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        // 排除静态资源
                        "/favicon.ico",
                        "/error"
                );
    }
}

package com.openiot.common.observability.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.openiot.common.observability.metrics.BusinessMetrics;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * 可观测性自动配置类
 *
 * <p>自动配置以下组件：
 * <ul>
 *   <li>Trace ID 过滤器 - 通过 {@link com.openiot.common.observability.filter.TraceIdFilter} 自动注册</li>
 *   <li>Tenant ID MDC 过滤器 - 通过 {@link com.openiot.common.observability.filter.TenantIdMdcFilter} 自动注册</li>
 *   <li>业务指标收集器 - 收集自定义业务指标</li>
 * </ul>
 *
 * <p><b>注意：</b>Filter 类使用 @ConditionalOnClass 注解，仅在 Servlet 环境下加载
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@Import({
    HikariMetricsConfig.class,
    RedisMetricsConfig.class
})
public class ObservabilityAutoConfiguration {

    /**
     * 业务指标收集器
     *
     * <p>提供自定义业务指标的定义和收集能力。
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "openiot.observability.metrics", name = "business-enabled", havingValue = "true", matchIfMissing = true)
    public BusinessMetrics businessMetrics(MeterRegistry meterRegistry) {
        return new BusinessMetrics(meterRegistry);
    }
}

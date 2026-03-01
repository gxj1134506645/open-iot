package com.openiot.common.observability.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.openiot.common.observability.filter.TenantIdMdcFilter;
import com.openiot.common.observability.filter.TraceIdFilter;
import com.openiot.common.observability.metrics.BusinessMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Filter;

/**
 * 可观测性自动配置类
 *
 * <p>自动配置以下组件：
 * <ul>
 *   <li>Trace ID 过滤器 - 为每个请求生成或传播 Trace ID</li>
 *   <li>Tenant ID MDC 过滤器 - 将租户 ID 注入到日志 MDC</li>
 *   <li>业务指标收集器 - 收集自定义业务指标</li>
 * </ul>
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
     * Trace ID 过滤器
     *
     * <p>为每个 HTTP 请求生成或传播 Trace ID，并注入到 MDC 中，
     * 以便日志可以关联到链路追踪。
     */
    @Bean
    @ConditionalOnClass(Filter.class)
    @ConditionalOnProperty(prefix = "openiot.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    /**
     * Tenant ID MDC 过滤器
     *
     * <p>将当前请求的租户 ID 注入到 MDC 中，
     * 以便日志可以按租户进行查询和过滤。
     */
    @Bean
    @ConditionalOnClass(Filter.class)
    @ConditionalOnProperty(prefix = "openiot.observability.logging", name = "tenant-mdc", havingValue = "true", matchIfMissing = true)
    public TenantIdMdcFilter tenantIdMdcFilter() {
        return new TenantIdMdcFilter();
    }

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

package com.openiot.common.observability.config;

import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Tracing Propagator 配置
 *
 * <p>指定使用 OpenTelemetry 的 Propagator 作为主要实现。
 * 解决 Spring Boot 同时引入 Brave 和 OpenTelemetry 导致的冲突。
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
@ConditionalOnProperty(prefix = "openiot.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingPropagatorConfig {

    /**
     * 将 OpenTelemetry Propagator 设置为主要实现
     *
     * @param otelPropagator OpenTelemetry Propagator（由 Spring Boot 自动配置提供）
     * @return OpenTelemetry Propagator
     */
    @Bean
    @Primary
    public Propagator primaryPropagator(@Qualifier("otelPropagator") Propagator otelPropagator) {
        log.info("Using OpenTelemetry Propagator as primary tracing propagator");
        return otelPropagator;
    }
}

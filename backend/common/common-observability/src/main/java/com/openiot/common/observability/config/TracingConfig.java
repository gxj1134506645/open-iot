package com.openiot.common.observability.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OpenTelemetry Tracing 配置
 *
 * <p>配置分布式链路追踪，支持：
 * <ul>
 *   <li>Trace ID 传播（W3C 标准）</li>
 *   <li>OTLP 导出到 Tempo</li>
 *   <li>自定义采样率</li>
 *   <li>服务标识标签</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({OpenTelemetry.class, Tracer.class})
@ConditionalOnProperty(prefix = "openiot.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class TracingConfig {

    /**
     * OTLP Span 导出器
     *
     * <p>将 Span 数据导出到 Tempo（通过 OTLP gRPC 协议）
     */
    @Bean
    @ConditionalOnProperty(prefix = "openiot.observability.tracing", name = "otlp-endpoint")
    public OtlpGrpcSpanExporter otlpSpanExporter(ObservabilityProperties properties) {
        String endpoint = properties.getTracing().getOtlpEndpoint();
        log.info("Configuring OTLP span exporter with endpoint: {}", endpoint);

        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint + "/v1/traces")
                .build();
    }

    /**
     * OpenTelemetry SDK 配置
     */
    @Bean
    public OpenTelemetry openTelemetry(
            OtlpGrpcSpanExporter spanExporter,
            ObservabilityProperties properties) {

        // 获取服务名称
        String serviceName = System.getProperty("spring.application.name", "unknown-service");
        log.info("Initializing OpenTelemetry for service: {}", serviceName);

        // 配置采样率
        double samplingProbability = properties.getTracing().getSamplingProbability();
        Sampler sampler = Sampler.traceIdRatioBased(samplingProbability);
        log.info("Tracing sampling probability: {}", samplingProbability);

        // 配置资源（服务标识）
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName,
                        AttributeKey.stringKey("service.version"), "1.0.0",
                        AttributeKey.stringKey("deployment.environment"), "production"
                )));

        // 构建 Tracer Provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setMaxExportBatchSize(512)
                        .setExporterTimeout(java.time.Duration.ofSeconds(30))
                        .setScheduleDelay(java.time.Duration.ofSeconds(5))
                        .build())
                .setResource(resource)
                .setSampler(sampler)
                .build();

        // 构建 OpenTelemetry SDK
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    /**
     * Micrometer Tracer 桥接
     *
     * <p>将 Micrometer Tracing 桥接到 OpenTelemetry
     */
    @Bean
    public OtelTracer otelTracer(OpenTelemetry openTelemetry) {
        return new OtelTracer(
                openTelemetry.getTracer("com.openiot", "1.0.0"),
                null,
                null
        );
    }
}

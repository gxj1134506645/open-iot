package com.openiot.common.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可观测性配置属性
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "openiot.observability")
public class ObservabilityProperties {

    /**
     * 链路追踪配置
     */
    private TracingConfig tracing = new TracingConfig();

    /**
     * 日志配置
     */
    private LoggingConfig logging = new LoggingConfig();

    /**
     * 指标配置
     */
    private MetricsConfig metrics = new MetricsConfig();

    @Data
    public static class TracingConfig {
        /**
         * 是否启用链路追踪
         */
        private boolean enabled = true;

        /**
         * 采样率 (0.0 - 1.0)
         * 开发环境建议 1.0，生产环境建议 0.1
         */
        private double samplingProbability = 1.0;

        /**
         * OTLP 导出端点
         */
        private String otlpEndpoint = "http://tempo:4317";

        /**
         * OTLP gRPC 端点
         */
        private String otlpGrpcEndpoint = "http://tempo:4317";

        /**
         * 是否记录 HTTP 请求体
         */
        private boolean recordRequestBody = false;

        /**
         * 是否记录 HTTP 响应体
         */
        private boolean recordResponseBody = false;

        /**
         * 最大记录字节数
         */
        private int maxRecordSize = 1024;
    }

    @Data
    public static class LoggingConfig {
        /**
         * 是否启用租户 ID MDC 注入
         */
        private boolean tenantMdc = true;

        /**
         * 是否启用敏感数据脱敏
         */
        private boolean sensitiveDataMasking = true;

        /**
         * 敏感字段列表
         */
        private String[] sensitiveFields = {"password", "token", "secret", "apiKey", "idCard", "phone"};

        /**
         * 日志格式：JSON 或 TEXT
         */
        private String format = "JSON";
    }

    @Data
    public static class MetricsConfig {
        /**
         * 是否启用业务指标
         */
        private boolean businessEnabled = true;

        /**
         * 是否启用 HikariCP 指标
         */
        private boolean hikariEnabled = true;

        /**
         * 是否启用 Redis 指标
         */
        private boolean redisEnabled = true;

        /**
         * 是否启用 Kafka 指标
         */
        private boolean kafkaEnabled = true;

        /**
         * 是否启用 MongoDB 指标
         */
        private boolean mongodbEnabled = true;

        /**
         * 是否启用 Netty 指标
         */
        private boolean nettyEnabled = true;

        /**
         * 是否启用自定义 JVM 指标
         */
        private boolean jvmEnabled = true;
    }
}

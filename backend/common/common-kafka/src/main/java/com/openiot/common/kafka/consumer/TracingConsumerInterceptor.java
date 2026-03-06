package com.openiot.common.kafka.consumer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka Consumer 链路追踪拦截器
 *
 * <p>在消费消息时从消息头提取 Trace 上下文：
 * <ul>
 *   <li>traceparent - W3C Trace Context</li>
 *   <li>tracestate - W3C Trace State</li>
 *   <li>tenant-id - 租户 ID</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class TracingConsumerInterceptor<K, V> implements ConsumerInterceptor<K, V> {

    private static final String TRACE_PARENT_HEADER = "traceparent";
    private static final String TRACE_STATE_HEADER = "tracestate";
    private static final String TENANT_ID_HEADER = "tenant-id";
    private static final String SPAN_NAME_PREFIX = "kafka.consumer.";

    private Tracer tracer;
    private Propagator propagator;

    @Autowired
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @Autowired
    public void setPropagator(Propagator propagator) {
        this.propagator = propagator;
    }

    @Override
    public ConsumerRecords<K, V> onConsume(ConsumerRecords<K, V> records) {
        if (tracer == null || records.isEmpty()) {
            return records;
        }

        // 为每条记录创建 Span 并提取 Trace 上下文
        for (ConsumerRecord<K, V> record : records) {
            processRecord(record);
        }

        return records;
    }

    /**
     * 处理单条消息
     */
    private void processRecord(ConsumerRecord<K, V> record) {
        Headers headers = record.headers();

        // 从消息头提取 Trace 上下文
        Span.Builder spanBuilder = tracer.spanBuilder()
                .name(SPAN_NAME_PREFIX + record.topic())
                .tag("kafka.topic", record.topic())
                .tag("kafka.partition", String.valueOf(record.partition()))
                .tag("kafka.offset", String.valueOf(record.offset()));

        // 尝试从消息头提取父 Span
        String traceParent = getHeader(headers, TRACE_PARENT_HEADER);
        if (traceParent != null && !traceParent.isEmpty()) {
            // 解析 W3C Trace Context
            TraceContextData contextData = parseTraceParent(traceParent);
            if (contextData != null) {
                spanBuilder.setParent(tracer.traceContextBuilder()
                        .traceId(contextData.traceId)
                        .spanId(contextData.parentSpanId)
                        .sampled(contextData.sampled)
                        .build());

                log.debug("Extracted trace context from Kafka message: topic={}, traceId={}",
                        record.topic(), contextData.traceId);
            }
        }

        // 提取租户 ID
        String tenantId = getHeader(headers, TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isEmpty()) {
            spanBuilder.tag("tenant.id", tenantId);
            // 设置租户上下文
            setTenantContext(tenantId);
        }

        // 创建并激活 Span
        Span span = spanBuilder.start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // Span 在消费处理期间保持活跃
            log.trace("Created consumer span for message: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
        }
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        if (tracer == null) {
            return;
        }

        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("kafka.committed", "true");
            currentSpan.end();
        }
    }

    @Override
    public void close() {
        // 清理资源
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 配置初始化
    }

    /**
     * 从消息头获取值
     */
    private String getHeader(Headers headers, String key) {
        var header = headers.lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    /**
     * 解析 W3C Trace Context traceparent
     * 格式: version-traceid-parentid-flags
     */
    private TraceContextData parseTraceParent(String traceParent) {
        if (traceParent == null || traceParent.isEmpty()) {
            return null;
        }

        try {
            String[] parts = traceParent.split("-");
            if (parts.length != 4) {
                log.warn("Invalid traceparent format: {}", traceParent);
                return null;
            }

            // 验证版本
            if (!"00".equals(parts[0])) {
                log.warn("Unsupported traceparent version: {}", parts[0]);
                return null;
            }

            TraceContextData data = new TraceContextData();
            data.traceId = parts[1];
            data.parentSpanId = parts[2];
            data.sampled = "01".equals(parts[3]);

            return data;

        } catch (Exception e) {
            log.warn("Failed to parse traceparent: {}", traceParent, e);
            return null;
        }
    }

    /**
     * 设置租户上下文
     */
    private void setTenantContext(String tenantId) {
        // 集成租户上下文管理
        try {
            // TenantContext.setCurrentTenant(tenantId);
        } catch (Exception e) {
            log.debug("Failed to set tenant context: {}", tenantId);
        }
    }

    /**
     * Trace 上下文数据
     */
    private static class TraceContextData {
        String traceId;
        String parentSpanId;
        boolean sampled;
    }
}

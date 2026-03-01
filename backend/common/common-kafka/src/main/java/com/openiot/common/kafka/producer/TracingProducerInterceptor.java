package com.openiot.common.kafka.producer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka Producer 链路追踪拦截器
 *
 * <p>在发送消息时注入 Trace 上下文到消息头：
 * <ul>
 *   <li>traceparent - W3C Trace Context</li>
 *   <li>tracestate - W3C Trace State</li>
 *   <li>tenant-id - 租户 ID（如果存在）</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class TracingProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

    private static final String TRACE_PARENT_HEADER = "traceparent";
    private static final String TRACE_STATE_HEADER = "tracestate";
    private static final String TENANT_ID_HEADER = "tenant-id";
    private static final String SPAN_NAME_PREFIX = "kafka.producer.";

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
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        if (tracer == null) {
            return record;
        }

        // 获取当前 Span
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            // 创建新的 Span
            currentSpan = tracer.nextSpan()
                    .name(SPAN_NAME_PREFIX + record.topic())
                    .tag("kafka.topic", record.topic())
                    .tag("kafka.partition", String.valueOf(record.partition()))
                    .start();
        }

        try (Tracer.SpanInScope ws = tracer.withSpan(currentSpan)) {
            Headers headers = record.headers();

            // 注入 Trace 上下文到消息头
            if (propagator != null) {
                propagator.inject(currentSpan.context(), headers, (carrier, key, value) -> {
                    carrier.remove(key);
                    carrier.add(key, value.getBytes(StandardCharsets.UTF_8));
                });
            } else {
                // 手动注入 W3C Trace Context
                String traceParent = formatTraceParent(currentSpan.context());
                headers.remove(TRACE_PARENT_HEADER);
                headers.add(TRACE_PARENT_HEADER, traceParent.getBytes(StandardCharsets.UTF_8));
            }

            // 添加租户 ID（如果存在）
            String tenantId = getTenantId();
            if (tenantId != null && !tenantId.isEmpty()) {
                headers.remove(TENANT_ID_HEADER);
                headers.add(TENANT_ID_HEADER, tenantId.getBytes(StandardCharsets.UTF_8));
            }

            log.debug("Injected trace context to Kafka message: topic={}, traceId={}",
                    record.topic(), currentSpan.context().traceId());

            return record;

        } finally {
            currentSpan.end();
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (tracer == null || metadata == null) {
            return;
        }

        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            if (exception != null) {
                currentSpan.error(exception);
                currentSpan.tag("kafka.error", exception.getMessage());
            } else {
                currentSpan.tag("kafka.partition", String.valueOf(metadata.partition()));
                currentSpan.tag("kafka.offset", String.valueOf(metadata.offset()));
            }
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
     * 格式化 W3C Trace Context traceparent
     */
    private String formatTraceParent(TraceContext context) {
        // W3C Trace Context 格式: version-traceid-parentid-flags
        // version: 00
        // traceid: 32 hex chars
        // parentid: 16 hex chars
        // flags: 2 hex chars (01 = sampled)
        return String.format("00-%s-%s-01",
                context.traceId(),
                context.spanId());
    }

    /**
     * 获取当前租户 ID
     */
    private String getTenantId() {
        // 从 ThreadLocal 或上下文中获取租户 ID
        try {
            // 这里可以集成租户上下文
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

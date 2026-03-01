package com.openiot.data.metrics;

import com.openiot.common.observability.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 消息指标收集器
 *
 * <p>收集消息处理相关的业务指标：
 * <ul>
 *   <li>消息接收/处理/失败计数</li>
 *   <li>消息处理延迟</li>
 *   <li>消息大小分布</li>
 *   <li>按类型和租户分组的消息统计</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class MessageMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final BusinessMetrics businessMetrics;

    // 消息计数器
    private final Counter messagesReceived;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;

    // 消息处理计时器
    private final Timer processingLatency;

    // 消息大小分布
    private final DistributionSummary messageSize;

    @Autowired
    public MessageMetricsCollector(MeterRegistry meterRegistry, BusinessMetrics businessMetrics) {
        this.meterRegistry = meterRegistry;
        this.businessMetrics = businessMetrics;

        // 初始化消息计数器
        this.messagesReceived = Counter.builder("openiot_data_messages_received_total")
                .description("Total messages received")
                .tag("service", "data-service")
                .register(meterRegistry);

        this.messagesProcessed = Counter.builder("openiot_data_messages_processed_total")
                .description("Total messages processed successfully")
                .tag("service", "data-service")
                .register(meterRegistry);

        this.messagesFailed = Counter.builder("openiot_data_messages_failed_total")
                .description("Total messages failed to process")
                .tag("service", "data-service")
                .register(meterRegistry);

        // 初始化处理延迟计时器
        this.processingLatency = Timer.builder("openiot_data_message_processing_latency")
                .description("Message processing latency")
                .tag("service", "data-service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(java.time.Duration.ofMillis(1))
                .maximumExpectedValue(java.time.Duration.ofSeconds(30))
                .register(meterRegistry);

        // 初始化消息大小分布
        this.messageSize = DistributionSummary.builder("openiot_data_message_size_bytes")
                .description("Message size in bytes")
                .tag("service", "data-service")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        log.info("Message metrics collector initialized");
    }

    /**
     * 记录消息接收
     *
     * @param messageType 消息类型
     * @param tenantId 租户 ID
     * @param sizeBytes 消息大小（字节）
     */
    public void recordMessageReceived(String messageType, String tenantId, long sizeBytes) {
        messagesReceived.increment();
        businessMetrics.recordMessageReceived(tenantId, messageType);
        messageSize.record(sizeBytes);

        // 记录按类型分组的计数器
        Counter.builder("openiot_data_messages_by_type_total")
                .tag("service", "data-service")
                .tag("type", messageType)
                .tag("tenant", tenantId)
                .description("Messages by type")
                .register(meterRegistry)
                .increment();

        log.debug("Recorded message received: type={}, tenant={}, size={}",
                messageType, tenantId, sizeBytes);
    }

    /**
     * 记录消息处理成功
     *
     * @param messageType 消息类型
     * @param tenantId 租户 ID
     * @param processingTimeMs 处理时间（毫秒）
     */
    public void recordMessageProcessed(String messageType, String tenantId, long processingTimeMs) {
        messagesProcessed.increment();
        businessMetrics.recordMessageProcessed(processingTimeMs);
        processingLatency.record(processingTimeMs, TimeUnit.MILLISECONDS);

        log.debug("Recorded message processed: type={}, tenant={}, latency={}ms",
                messageType, tenantId, processingTimeMs);
    }

    /**
     * 记录消息处理失败
     *
     * @param messageType 消息类型
     * @param tenantId 租户 ID
     * @param errorType 错误类型
     */
    public void recordMessageFailed(String messageType, String tenantId, String errorType) {
        messagesFailed.increment();
        businessMetrics.recordMessageFailed(errorType);

        // 记录按错误类型分组的计数器
        Counter.builder("openiot_data_message_errors_total")
                .tag("service", "data-service")
                .tag("type", messageType)
                .tag("error", errorType)
                .description("Message processing errors")
                .register(meterRegistry)
                .increment();

        log.debug("Recorded message failed: type={}, tenant={}, error={}",
                messageType, tenantId, errorType);
    }

    /**
     * 使用 Timer.Sample 记录消息处理时间
     *
     * @param sample Timer.Sample 对象
     * @param messageType 消息类型
     * @param tenantId 租户 ID
     * @param success 是否成功
     */
    public void recordProcessingResult(Timer.Sample sample, String messageType,
                                       String tenantId, boolean success) {
        sample.stop(processingLatency);

        if (success) {
            recordMessageProcessed(messageType, tenantId, 0);
        } else {
            recordMessageFailed(messageType, tenantId, "processing_error");
        }
    }

    /**
     * 开始计时
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 记录批量消息处理
     *
     * @param count 消息数量
     * @param successCount 成功数量
     * @param failCount 失败数量
     * @param totalTimeMs 总处理时间（毫秒）
     */
    public void recordBatchProcessing(int count, int successCount, int failCount, long totalTimeMs) {
        // 记录批量计数器
        Counter.builder("openiot_data_batch_total")
                .tag("service", "data-service")
                .description("Batch processing count")
                .register(meterRegistry)
                .increment();

        Counter.builder("openiot_data_batch_messages_total")
                .tag("service", "data-service")
                .tag("result", "success")
                .description("Batch messages processed")
                .register(meterRegistry)
                .increment(successCount);

        if (failCount > 0) {
            Counter.builder("openiot_data_batch_messages_total")
                    .tag("service", "data-service")
                    .tag("result", "failed")
                    .description("Batch messages failed")
                    .register(meterRegistry)
                    .increment(failCount);
        }

        // 记录批量大小分布
        DistributionSummary.builder("openiot_data_batch_size")
                .tag("service", "data-service")
                .description("Batch size distribution")
                .register(meterRegistry)
                .record(count);

        log.debug("Recorded batch processing: total={}, success={}, failed={}, time={}ms",
                count, successCount, failCount, totalTimeMs);
    }
}

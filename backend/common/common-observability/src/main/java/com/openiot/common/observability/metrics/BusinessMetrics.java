package com.openiot.common.observability.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务指标收集器
 *
 * <p>提供 OpenIoT 平台业务指标的统一收集和管理。
 * 所有指标以 `openiot.` 为前缀，便于区分和查询。
 *
 * <h3>指标命名规范：</h3>
 * <pre>
 * {namespace}.{subsystem}.{metric_name}_{unit}
 * 例如：openiot.device.connected_count
 * </pre>
 *
 * <h3>核心指标：</h3>
 * <ul>
 *   <li>设备指标：连接数、在线数、消息数</li>
 *   <li>消息指标：接收、处理、失败数量和延迟</li>
 *   <li>错误指标：按类型分类的错误计数</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Getter
public class BusinessMetrics {

    private static final String NAMESPACE = "openiot";

    private final MeterRegistry meterRegistry;

    // ========================================
    // 设备指标
    // ========================================

    /**
     * 设备连接总数
     */
    private final AtomicLong connectedDevices = new AtomicLong(0);

    /**
     * 设备在线总数
     */
    private final AtomicLong onlineDevices = new AtomicLong(0);

    /**
     * 设备连接计数器
     */
    private final Counter deviceConnectedCounter;

    /**
     * 设备断开计数器
     */
    private final Counter deviceDisconnectedCounter;

    // ========================================
    // 消息指标
    // ========================================

    /**
     * 消息接收计数器
     */
    private final Counter messageReceivedCounter;

    /**
     * 消息处理成功计数器
     */
    private final Counter messageProcessedCounter;

    /**
     * 消息处理失败计数器
     */
    private final Counter messageFailedCounter;

    /**
     * 消息处理延迟计时器
     */
    private final Timer messageProcessingTimer;

    // ========================================
    // 错误指标
    // ========================================

    /**
     * 错误计数器
     */
    private final Counter errorCounter;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("Initializing business metrics with namespace: {}", NAMESPACE);

        // 初始化设备指标
        Gauge.builder(NAMESPACE + ".device.connected.count", connectedDevices, AtomicLong::get)
                .description("当前连接的设备总数")
                .register(meterRegistry);

        Gauge.builder(NAMESPACE + ".device.online.count", onlineDevices, AtomicLong::get)
                .description("当前在线的设备总数")
                .register(meterRegistry);

        this.deviceConnectedCounter = Counter.builder(NAMESPACE + ".device.connected.total")
                .description("设备连接总数")
                .register(meterRegistry);

        this.deviceDisconnectedCounter = Counter.builder(NAMESPACE + ".device.disconnected.total")
                .description("设备断开总数")
                .register(meterRegistry);

        // 初始化消息指标
        this.messageReceivedCounter = Counter.builder(NAMESPACE + ".message.received.total")
                .description("接收的消息总数")
                .tag("protocol", "unknown")
                .register(meterRegistry);

        this.messageProcessedCounter = Counter.builder(NAMESPACE + ".message.processed.total")
                .description("处理成功的消息总数")
                .register(meterRegistry);

        this.messageFailedCounter = Counter.builder(NAMESPACE + ".message.failed.total")
                .description("处理失败的消息总数")
                .register(meterRegistry);

        this.messageProcessingTimer = Timer.builder(NAMESPACE + ".message.processing.duration")
                .description("消息处理延迟")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(java.time.Duration.ofMillis(1))
                .maximumExpectedValue(java.time.Duration.ofSeconds(30))
                .register(meterRegistry);

        // 初始化错误指标
        this.errorCounter = Counter.builder(NAMESPACE + ".error.total")
                .description("错误总数")
                .tag("type", "unknown")
                .register(meterRegistry);
    }

    // ========================================
    // 设备指标方法
    // ========================================

    /**
     * 记录设备连接
     */
    public void recordDeviceConnected(String tenantId, String protocol) {
        connectedDevices.incrementAndGet();
        deviceConnectedCounter.increment();
        log.debug("Device connected, total: {}", connectedDevices.get());
    }

    /**
     * 记录设备断开
     */
    public void recordDeviceDisconnected(String tenantId) {
        connectedDevices.decrementAndGet();
        deviceDisconnectedCounter.increment();
        log.debug("Device disconnected, total: {}", connectedDevices.get());
    }

    /**
     * 设置在线设备数
     */
    public void setOnlineDevices(long count) {
        onlineDevices.set(count);
    }

    /**
     * 增加在线设备数
     */
    public void incrementOnlineDevices() {
        onlineDevices.incrementAndGet();
    }

    /**
     * 减少在线设备数
     */
    public void decrementOnlineDevices() {
        onlineDevices.decrementAndGet();
    }

    // ========================================
    // 消息指标方法
    // ========================================

    /**
     * 记录消息接收
     */
    public void recordMessageReceived(String tenantId, String protocol) {
        messageReceivedCounter.increment();
    }

    /**
     * 记录消息处理成功
     */
    public void recordMessageProcessed(long durationMillis) {
        messageProcessedCounter.increment();
        messageProcessingTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录消息处理失败
     */
    public void recordMessageFailed(String errorType) {
        messageFailedCounter.increment();
        recordError(errorType);
    }

    // ========================================
    // 错误指标方法
    // ========================================

    /**
     * 记录错误
     */
    public void recordError(String errorType) {
        errorCounter.increment();
        log.debug("Error recorded, type: {}", errorType);
    }

    /**
     * 创建带租户标签的计数器
     */
    public Counter createTenantCounter(String name, String tenantId) {
        return Counter.builder(NAMESPACE + "." + name)
                .tag("tenant_id", tenantId)
                .register(meterRegistry);
    }

    /**
     * 创建带租户标签的计时器
     */
    public Timer createTenantTimer(String name, String tenantId) {
        return Timer.builder(NAMESPACE + "." + name)
                .tag("tenant_id", tenantId)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }
}

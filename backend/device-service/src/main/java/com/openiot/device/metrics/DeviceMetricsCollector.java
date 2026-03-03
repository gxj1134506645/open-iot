package com.openiot.device.metrics;

import com.openiot.common.observability.metrics.BusinessMetrics;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 设备指标收集器
 *
 * <p>收集设备相关的业务指标：
 * <ul>
 *   <li>设备总数</li>
 *   <li>在线设备数</li>
 *   <li>设备连接/断开事件</li>
 *   <li>设备注册速率</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class DeviceMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final DeviceMapper deviceMapper;
    private final BusinessMetrics businessMetrics;

    // 缓存的指标值
    private final AtomicLong totalDevices = new AtomicLong(0);
    private final AtomicLong onlineDevices = new AtomicLong(0);

    // 计数器
    private final Counter deviceConnections;
    private final Counter deviceDisconnections;
    private final Counter deviceRegistrations;

    // 计时器
    private final Timer connectionLatency;

    @Autowired
    public DeviceMetricsCollector(MeterRegistry meterRegistry,
                                  DeviceMapper deviceMapper,
                                  BusinessMetrics businessMetrics) {
        this.meterRegistry = meterRegistry;
        this.deviceMapper = deviceMapper;
        this.businessMetrics = businessMetrics;

        // 注册 Gauge 指标
        Gauge.builder("openiot_device_total_count", totalDevices, AtomicLong::get)
                .description("Total number of devices")
                .tag("service", "device-service")
                .register(meterRegistry);

        Gauge.builder("openiot_device_online_count", onlineDevices, AtomicLong::get)
                .description("Number of online devices")
                .tag("service", "device-service")
                .register(meterRegistry);

        // 初始化计数器
        this.deviceConnections = Counter.builder("openiot_device_connections_total")
                .description("Total device connection events")
                .tag("service", "device-service")
                .register(meterRegistry);

        this.deviceDisconnections = Counter.builder("openiot_device_disconnections_total")
                .description("Total device disconnection events")
                .tag("service", "device-service")
                .register(meterRegistry);

        this.deviceRegistrations = Counter.builder("openiot_device_registrations_total")
                .description("Total device registration events")
                .tag("service", "device-service")
                .register(meterRegistry);

        // 初始化计时器
        this.connectionLatency = Timer.builder("openiot_device_connection_latency")
                .description("Device connection latency")
                .tag("service", "device-service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        log.info("Device metrics collector initialized");
    }

    /**
     * 定时更新设备统计指标
     * 每 30 秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void updateDeviceMetrics() {
        try {
            // 使用 MyBatis Plus Lambda 查询
            long total = deviceMapper.selectCount(null);
            long online = deviceMapper.selectCount(
                    new LambdaQueryWrapper<Device>().eq(Device::getStatus, "1")
            );

            totalDevices.set(total);
            onlineDevices.set(online);

            // 同步到通用业务指标
            businessMetrics.setOnlineDevices(online);

            log.debug("Updated device metrics: total={}, online={}", total, online);
        } catch (Exception e) {
            log.error("Failed to update device metrics", e);
        }
    }

    /**
     * 记录设备连接事件
     *
     * @param deviceId 设备 ID
     * @param tenantId 租户 ID
     * @param latencyMs 连接延迟（毫秒）
     */
    public void recordConnection(String deviceId, String tenantId, long latencyMs) {
        deviceConnections.increment();
        businessMetrics.recordDeviceConnected(tenantId, "tcp");
        connectionLatency.record(latencyMs, TimeUnit.MILLISECONDS);

        log.debug("Recorded device connection: deviceId={}, tenantId={}, latency={}ms",
                deviceId, tenantId, latencyMs);
    }

    /**
     * 记录设备断开事件
     *
     * @param deviceId 设备 ID
     * @param tenantId 租户 ID
     * @param reason 断开原因
     */
    public void recordDisconnection(String deviceId, String tenantId, String reason) {
        deviceDisconnections.increment();
        businessMetrics.recordDeviceDisconnected(tenantId);

        log.debug("Recorded device disconnection: deviceId={}, tenantId={}, reason={}",
                deviceId, tenantId, reason);
    }

    /**
     * 记录设备注册事件
     *
     * @param deviceId 设备 ID
     * @param tenantId 租户 ID
     */
    public void recordRegistration(String deviceId, String tenantId) {
        deviceRegistrations.increment();

        // 更新总数
        totalDevices.incrementAndGet();

        log.debug("Recorded device registration: deviceId={}, tenantId={}", deviceId, tenantId);
    }

    /**
     * 获取当前设备总数
     */
    public long getTotalDevices() {
        return totalDevices.get();
    }

    /**
     * 获取当前在线设备数
     */
    public long getOnlineDevices() {
        return onlineDevices.get();
    }

    /**
     * 获取设备在线率
     */
    public double getOnlineRate() {
        long total = totalDevices.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) onlineDevices.get() / total;
    }
}

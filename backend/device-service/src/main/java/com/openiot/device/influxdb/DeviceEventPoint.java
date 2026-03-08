package com.openiot.device.influxdb;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备事件时序数据
 *
 * @author OpenIoT Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "device_event")
public class DeviceEventPoint {

    /**
     * 时间戳（InfluxDB 主键）
     */
    @Column(timestamp = true)
    private Instant time;

    /**
     * 租户ID（Tag）
     */
    @Column(name = "tenant_id", tag = true)
    private Long tenantId;

    /**
     * 设备ID（Tag）
     */
    @Column(name = "device_id", tag = true)
    private Long deviceId;

    /**
     * 设备编码（Tag）
     */
    @Column(name = "device_code", tag = true)
    private String deviceCode;

    /**
     * 产品ID（Tag）
     */
    @Column(name = "product_id", tag = true)
    private Long productId;

    /**
     * 事件标识符（Tag）
     */
    @Column(name = "event_identifier", tag = true)
    private String eventIdentifier;

    /**
     * 事件类型（Tag）
     */
    @Column(name = "event_type", tag = true)
    private String eventType;

    /**
     * 事件级别（Tag）
     */
    @Column(name = "event_level", tag = true)
    private String eventLevel;

    /**
     * 事件输出参数（Field，存储为 JSON 字符串）
     */
    @Column(name = "output_params")
    private String outputParams;
}

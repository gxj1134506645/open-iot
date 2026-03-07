package com.openiot.device.influxdb;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备状态时序数据
 *
 * @author OpenIoT Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "device_status")
public class DeviceStatusPoint {

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
     * 设备状态（Field）
     */
    @Column(name = "status")
    private String status;

    /**
     * 在线时长（秒）（Field）
     */
    @Column(name = "online_duration")
    private Long onlineDuration;

    /**
     * 信号强度（Field）
     */
    @Column(name = "signal_strength")
    private Integer signalStrength;

    /**
     * 电量百分比（Field）
     */
    @Column(name = "battery_level")
    private Integer batteryLevel;

    /**
     * 固件版本（Tag）
     */
    @Column(name = "firmware_version", tag = true)
    private String firmwareVersion;

    /**
     * IP 地址（Tag）
     */
    @Column(name = "ip_address", tag = true)
    private String ipAddress;
}

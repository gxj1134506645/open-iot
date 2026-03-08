package com.openiot.device.influxdb;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 设备属性时序数据
 *
 * @author OpenIoT Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "device_property")
public class DevicePropertyPoint {

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
     * 属性标识符（Tag）
     */
    @Column(name = "property_identifier", tag = true)
    private String propertyIdentifier;

    /**
     * 属性值（Field，存储为字符串）
     */
    @Column(name = "value")
    private String value;

    /**
     * 数据类型（Tag）
     */
    @Column(name = "data_type", tag = true)
    private String dataType;

    /**
     * 数据质量（Field）
     */
    @Column(name = "quality")
    private Integer quality;

    /**
     * 扩展属性（Field，存储为 JSON 字符串）
     */
    @Column(name = "extensions")
    private String extensions;
}

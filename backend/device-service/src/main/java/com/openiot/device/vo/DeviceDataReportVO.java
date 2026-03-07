package com.openiot.device.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备数据上报 VO
 *
 * @author OpenIoT Team
 */
@Data
public class DeviceDataReportVO {

    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /**
     * 上报数据（属性标识符 -> 值）
     * 示例：{"temperature": 25.5, "humidity": 60.2}
     */
    @NotNull(message = "上报数据不能为空")
    private Map<String, Object> data;

    /**
     * 数据时间戳（设备上报时间，可选，默认当前时间）
     */
    private LocalDateTime dataTime;
}

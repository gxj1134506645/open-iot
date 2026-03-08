package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备数据上报 VO
 *
 * @author OpenIoT Team
 */
@Data
@Schema(description = "设备数据上报请求")
public class DeviceDataReportVO {

    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    @Schema(description = "设备ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deviceId;

    /**
     * 上报数据（属性标识符 -> 值）
     * 示例：{"temperature": 25.5, "humidity": 60.2}
     */
    @NotNull(message = "上报数据不能为空")
    @Schema(description = "上报数据，属性标识符映射到值", example = "{\"temperature\": 25.5, \"humidity\": 60.2}", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> data;

    /**
     * 数据时间戳（设备上报时间，可选，默认当前时间）
     */
    @Schema(description = "数据时间戳，设备上报时间，不指定则使用服务器时间", example = "2026-03-07T12:00:00")
    private LocalDateTime dataTime;
}

package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备数据实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "device_data", autoResultMap = true)
public class DeviceData {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备上报数据（JSON格式）
     * 示例：{"temperature": 25.5, "humidity": 60.2}
     */
    @TableField(value = "data", typeHandler = JsonbTypeHandler.class)
    private JsonNode data;

    /**
     * 数据时间戳（设备上报时间）
     */
    private LocalDateTime dataTime;

    /**
     * 入库时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

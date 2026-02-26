package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备轨迹实体类
 */
@Data
@TableName("device_trajectory")
public class DeviceTrajectory {

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
     * 设备ID
     */
    private Long deviceId;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 速度(km/h)
     */
    private BigDecimal speed;

    /**
     * 航向角(度)
     */
    private BigDecimal heading;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

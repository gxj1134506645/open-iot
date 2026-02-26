package com.openiot.data.entity;

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

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long deviceId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speed;
    private BigDecimal heading;
    private LocalDateTime eventTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

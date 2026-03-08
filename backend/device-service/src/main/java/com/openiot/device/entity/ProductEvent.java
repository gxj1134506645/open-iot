package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品事件定义实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "product_event", autoResultMap = true)
public class ProductEvent {

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
     * 事件标识符（英文）
     */
    private String eventIdentifier;

    /**
     * 事件名称（中文）
     */
    private String eventName;

    /**
     * 事件类型：info-信息，warn-告警，error-故障
     */
    private String eventType;

    /**
     * 事件级别（用于告警策略）
     */
    private String eventLevel;

    /**
     * 事件参数定义（JSON格式）
     * 示例：[{"identifier": "temperature", "name": "当前温度", "dataType": "float"}]
     */
    @TableField(value = "params", typeHandler = JsonbTypeHandler.class)
    private JsonNode params;

    /**
     * 删除标记：0-正常，1-已删除
     */
    @TableLogic
    private String deleteFlag;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}

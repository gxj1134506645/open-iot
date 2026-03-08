package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品服务定义实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "product_service", autoResultMap = true)
public class ProductService {

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
     * 服务标识符（英文）
     */
    private String serviceIdentifier;

    /**
     * 服务名称（中文）
     */
    private String serviceName;

    /**
     * 调用方式：sync-同步，async-异步
     */
    private String callType;

    /**
     * 输入参数定义（JSON格式）
     * 示例：[{"identifier": "interval", "name": "上报间隔", "dataType": "int"}]
     */
    @TableField(value = "input_params", typeHandler = JsonbTypeHandler.class)
    private JsonNode inputParams;

    /**
     * 输出参数定义（JSON格式）
     * 示例：[{"identifier": "code", "name": "返回码", "dataType": "int"}]
     */
    @TableField(value = "output_params", typeHandler = JsonbTypeHandler.class)
    private JsonNode outputParams;

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

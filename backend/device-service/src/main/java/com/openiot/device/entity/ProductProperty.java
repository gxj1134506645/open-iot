package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品属性定义实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "product_property", autoResultMap = true)
public class ProductProperty {

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
     * 属性标识符（英文，用于设备通信）
     */
    private String propertyIdentifier;

    /**
     * 属性名称（中文，用于展示）
     */
    private String propertyName;

    /**
     * 数据类型：int/float/enum/bool/string/text/date/json
     */
    private String dataType;

    /**
     * 属性规格定义（JSON格式）
     * 示例：{"min": -40, "max": 120, "unit": "℃", "step": 0.1}
     */
    @TableField(value = "spec", typeHandler = JsonbTypeHandler.class)
    private JsonNode spec;

    /**
     * 读写标识：r-只读，rw-读写，w-只写
     */
    private String readWriteFlag;

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

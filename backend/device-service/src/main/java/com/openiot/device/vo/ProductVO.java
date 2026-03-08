package com.openiot.device.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "产品信息")
public class ProductVO {

    /**
     * 主键
     */
    @Schema(description = "产品ID", example = "1")
    private Long id;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    /**
     * 产品密钥（租户内唯一）
     */
    @Schema(description = "产品密钥，租户内唯一", example = "a1b2c3d4e5")
    private String productKey;

    /**
     * 产品名称
     */
    @Schema(description = "产品名称", example = "温湿度传感器")
    private String productName;

    /**
     * 产品类型（DEVICE/GATEWAY）
     */
    @Schema(description = "产品类型", example = "DEVICE", allowableValues = {"DEVICE", "GATEWAY"})
    private String productType;

    /**
     * 协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM）
     */
    @Schema(description = "协议类型", example = "MQTT", allowableValues = {"MQTT", "HTTP", "CoAP", "LwM2M", "CUSTOM"})
    private String protocolType;

    /**
     * 节点类型（DIRECT/GATEWAY）
     */
    @Schema(description = "节点类型", example = "DIRECT", allowableValues = {"DIRECT", "GATEWAY"})
    private String nodeType;

    /**
     * 数据格式（JSON/XML/BINARY/CUSTOM）
     */
    @Schema(description = "数据格式", example = "JSON", allowableValues = {"JSON", "XML", "BINARY", "CUSTOM"})
    private String dataFormat;

    /**
     * 物模型定义（JSON格式）
     */
    @Schema(description = "物模型定义，包含属性、事件、服务")
    private JsonNode thingModel;

    /**
     * 产品描述
     */
    @Schema(description = "产品描述")
    private String description;

    /**
     * 状态（1=启用，0=禁用）
     */
    @Schema(description = "状态：1-启用，0-禁用", example = "1", allowableValues = {"0", "1"})
    private String status;

    /**
     * 关联设备数量
     */
    @Schema(description = "关联设备数量", example = "10")
    private Integer deviceCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-03-07T12:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-03-07T14:30:00")
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @Schema(description = "创建人ID", example = "1")
    private Long createBy;

    /**
     * 更新人ID
     */
    @Schema(description = "更新人ID", example = "1")
    private Long updateBy;
}

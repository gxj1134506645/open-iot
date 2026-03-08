package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产品更新 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "产品更新请求")
public class ProductUpdateVO {

    /**
     * 产品名称
     */
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    @Schema(description = "产品名称", example = "温湿度传感器", maxLength = 100)
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
    @Schema(description = "节点类型：DIRECT-直连设备，GATEWAY-网关设备", example = "DIRECT", allowableValues = {"DIRECT", "GATEWAY"})
    private String nodeType;

    /**
     * 数据格式（JSON/XML/BINARY/CUSTOM）
     */
    @Schema(description = "数据格式", example = "JSON", allowableValues = {"JSON", "XML", "BINARY", "CUSTOM"})
    private String dataFormat;

    /**
     * 产品描述
     */
    @Size(max = 500, message = "产品描述长度不能超过500个字符")
    @Schema(description = "产品描述", example = "用于环境温湿度监测的传感器设备", maxLength = 500)
    private String description;

    /**
     * 状态（1=启用，0=禁用）
     */
    @Schema(description = "状态：1-启用，0-禁用", example = "1", allowableValues = {"0", "1"})
    private String status;
}

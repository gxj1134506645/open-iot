package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产品创建 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "产品创建请求")
public class ProductCreateVO {

    /**
     * 产品名称
     */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    @Schema(description = "产品名称", example = "温湿度传感器", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;

    /**
     * 产品类型（DEVICE/GATEWAY）
     */
    @NotBlank(message = "产品类型不能为空")
    @Schema(description = "产品类型", example = "DEVICE", allowableValues = {"DEVICE", "GATEWAY"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String productType;

    /**
     * 协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM）
     */
    @NotBlank(message = "协议类型不能为空")
    @Schema(description = "协议类型", example = "MQTT", allowableValues = {"MQTT", "HTTP", "CoAP", "LwM2M", "CUSTOM"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String protocolType;

    /**
     * 节点类型（DIRECT/GATEWAY）
     */
    @Schema(description = "节点类型：DIRECT-直连设备，GATEWAY-网关设备", example = "DIRECT", allowableValues = {"DIRECT", "GATEWAY"})
    private String nodeType = "DIRECT";

    /**
     * 数据格式（JSON/XML/BINARY/CUSTOM）
     */
    @Schema(description = "数据格式", example = "JSON", allowableValues = {"JSON", "XML", "BINARY", "CUSTOM"})
    private String dataFormat = "JSON";

    /**
     * 产品描述
     */
    @Size(max = 500, message = "产品描述长度不能超过500个字符")
    @Schema(description = "产品描述", example = "用于环境温湿度监测的传感器设备", maxLength = 500)
    private String description;
}

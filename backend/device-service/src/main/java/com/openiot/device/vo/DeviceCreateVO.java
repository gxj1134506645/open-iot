package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备创建 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "设备创建请求")
public class DeviceCreateVO {

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    @Schema(description = "产品ID", example = "1")
    private Long productId;

    /**
     * 设备编码
     */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 50, message = "设备编码长度不能超过50个字符")
    @Schema(description = "设备编码，租户内唯一", example = "device001", maxLength = 50)
    private String deviceCode;

    /**
     * 设备名称
     */
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    @Schema(description = "设备名称", example = "温湿度传感器-1", maxLength = 100)
    private String deviceName;

    /**
     * 协议类型（MQTT/TCP/HTTP）
     */
    @NotBlank(message = "协议类型不能为空")
    @Schema(description = "协议类型", example = "MQTT", allowableValues = {"MQTT", "TCP", "HTTP", "CoAP"})
    private String protocolType;
}

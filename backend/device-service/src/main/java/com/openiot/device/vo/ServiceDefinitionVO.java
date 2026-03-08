package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 服务定义 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "服务定义")
public class ServiceDefinitionVO {

    /**
     * 服务标识符
     */
    @NotBlank(message = "标识符不能为空")
    @Schema(description = "服务标识符")
    private String identifier;

    /**
     * 服务名称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(description = "服务显示名称")
    private String name;

    /**
     * 调用方式: sync(同步), async(异步)
     */
    @NotBlank(message = "调用方式不能为空")
    @Schema(description = "调用方式: sync/async")
    private String callType;

    /**
     * 描述
     */
    @Schema(description = "服务描述")
    private String description;
}

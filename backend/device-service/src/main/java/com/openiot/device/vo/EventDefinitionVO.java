package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 事件定义 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "事件定义")
public class EventDefinitionVO {

    /**
     * 事件标识符
     */
    @NotBlank(message = "标识符不能为空")
    @Schema(description = "事件标识符")
    private String identifier;

    /**
     * 事件名称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(description = "事件显示名称")
    private String name;

    /**
     * 事件类型: info(信息), alert(告警), fault(故障)
     */
    @NotBlank(message = "事件类型不能为空")
    @Schema(description = "事件类型: info/alert/fault")
    private String type;

    /**
     * 描述
     */
    @Schema(description = "事件描述")
    private String description;
}

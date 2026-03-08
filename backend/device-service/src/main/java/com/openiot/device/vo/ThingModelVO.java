package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 物模型 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "物模型定义，包含属性、事件、服务")
public class ThingModelVO {

    /**
     * 属性定义列表
     */
    @Schema(description = "属性定义列表")
    private List<PropertyDefinitionVO> properties;

    /**
     * 事件定义列表
     */
    @Schema(description = "事件定义列表")
    private List<EventDefinitionVO> events;

    /**
     * 服务定义列表
     */
    @Schema(description = "服务定义列表")
    private List<ServiceDefinitionVO> services;
}

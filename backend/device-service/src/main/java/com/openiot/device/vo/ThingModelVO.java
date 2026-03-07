package com.openiot.device.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 物模型 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "物模型定义")
public class ThingModelVO {

    /**
     * 属性定义列表
     */
    private List<PropertyDefinitionVO> properties;

    /**
     * 事件定义列表
     */
    private List<EventDefinitionVO> events;

    /**
     * 服务定义列表
     */
    private List<ServiceDefinitionVO> services;
}

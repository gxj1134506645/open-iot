package com.openiot.connect.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 物模型验证器
 *
 * 在 connect-service 中用于验证设备上报的数据是否符合产品物模型定义
 *
 * @author open-iot
 */
@Slf4j
@Component
public class ThingModelValidator {

    /**
     * 支持的数据类型
     */
    private static final Set<String> VALID_DATA_TYPES = Set.of(
        "int", "float", "double", "string", "boolean", "date", "array", "struct"
    );

    /**
     * 验证属性值是否符合物模型定义
     *
     * @param thingModel 物模型定义
     * @param propertyIdentifier 属性标识符
     * @param value 属性值
     * @return 验证通过返回 true
     */
    public boolean validateProperty(JsonNode thingModel, String propertyIdentifier, Object value) {
        if (thingModel == null || thingModel.isNull()) {
            return true; // 没有物模型定义，允许任意数据
        }

        JsonNode properties = thingModel.get("properties");
        if (properties == null || !properties.isArray()) {
            return true;
        }

        // 查找属性定义
        JsonNode propertyDef = findDefinition(properties, propertyIdentifier);
        if (propertyDef == null) {
                log.warn("属性 '{}' 未在物模型中定义，将被忽略", propertyIdentifier);
                return true; // 未定义的属性允许通过但记录警告
        }

        // 验证数据类型
        String dataType = propertyDef.get("dataType").asText();
        validateValueByType(propertyIdentifier, value, dataType, propertyDef);

        return true;
    }

    /**
     * 验证事件数据是否符合物模型定义
     *
     * @param thingModel 物模型定义
     * @param eventIdentifier 事件标识符
     * @param eventData 事件数据
     * @return 验证通过返回 true
     */
    public boolean validateEvent(JsonNode thingModel, String eventIdentifier, JsonNode eventData) {
        if (thingModel == null || thingModel.isNull()) {
                return true;
        }

        JsonNode events = thingModel.get("events");
        if (events == null || !events.isArray()) {
                return true;
            }

        // 查找事件定义
        JsonNode eventDef = findDefinition(events, eventIdentifier);
        if (eventDef == null) {
                log.warn("事件 '{}' 未在物模型中定义", eventIdentifier);
                return true;
            }

        // 验证事件类型
            if (eventDef.has("type")) {
                String expectedType = eventDef.get("type").asText();
                if (eventData.has("type")) {
                    String actualType = eventData.get("type").asText();
                    if (!expectedType.equals(actualType)) {
                        throw BusinessException.badRequest(
                            String.format("事件 '%s' 类型不匹配，期望: %s, 实际: %s",
                                eventIdentifier, expectedType, actualType));
                    }
                }
            }

        return true;
    }

    /**
     * 验证服务调用参数是否符合物模型定义
     *
     * @param thingModel 物模型定义
     * @param serviceIdentifier 服务标识符
     * @param inputParams 输入参数
     * @return 验证通过返回 true
     */
    public boolean validateServiceInput(JsonNode thingModel, String serviceIdentifier, JsonNode inputParams) {
        if (thingModel == null || thingModel.isNull()) {
                return true;
            }

        JsonNode services = thingModel.get("services");
        if (services == null || !services.isArray()) {
                return true;
            }

        // 查找服务定义
        JsonNode serviceDef = findDefinition(services, serviceIdentifier);
        if (serviceDef == null) {
                throw BusinessException.badRequest(
                    String.format("服务 '%s' 未在物模型中定义", serviceIdentifier));
            }

            // 验证输入参数
            JsonNode expectedParams = serviceDef.get("inputParams");
            if (expectedParams != null && expectedParams.isArray()) {
                for (JsonNode expectedParam : expectedParams) {
                    String paramIdentifier = expectedParam.get("identifier").asText();
                    boolean required = expectedParam.has("required") && expectedParam.get("required").asBoolean();

                    if (required && (inputParams == null || !inputParams.has(paramIdentifier))) {
                        throw BusinessException.badRequest(
                            String.format("服务 '%s' 缺少必填参数: %s", serviceIdentifier, paramIdentifier));
                    }

                    // 验证参数类型
                    if (inputParams != null && inputParams.has(paramIdentifier)) {
                        String expectedType = expectedParam.get("dataType").asText();
                        Object actualValue = parseValueFromJson(inputParams, paramIdentifier);
                        validateParamType(serviceIdentifier, paramIdentifier, actualValue, expectedType);
                    }
                }
            }

        return true;
    }

    /**
     * 从定义列表中查找指定标识符的定义
     */
    private JsonNode findDefinition(JsonNode definitions, String identifier) {
        for (JsonNode def : definitions) {
            if (def.has("identifier") && def.get("identifier").asText().equals(identifier)) {
                return def;
            }
        }
        return null;
    }

    /**
     * 根据类型验证值
     */
    private void validateValueByType(String identifier, Object value, String dataType, JsonNode propertyDef) {
        if (value == null) {
                return; // null 值允许
            }

            switch (dataType.toLowerCase()) {
                case "int":
                    if (!(value instanceof Number)) {
                        throw BusinessException.badRequest(
                            String.format("属性 '%s' 期望整数类型，实际: %s", identifier, value.getClass().getSimpleName()));
                    }
                    checkRange(identifier, ((Number) value).doubleValue(), propertyDef);
                    break;

                case "float":
                case "double":
                    if (!(value instanceof Number)) {
                        throw BusinessException.badRequest(
                            String.format("属性 '%s' 期望浮点类型,实际: %s", identifier, value.getClass().getSimpleName()));
                    }
                    checkRange(identifier, ((Number) value).doubleValue(), propertyDef);
                    break;

                case "string":
                    if (!(value instanceof String)) {
                        throw BusinessException.badRequest(
                            String.format("属性 '%s' 期望字符串类型,实际: %s", identifier, value.getClass().getSimpleName()));
                    }
                    break;

                case "boolean":
                    if (!(value instanceof Boolean)) {
                        throw BusinessException.badRequest(
                            String.format("属性 '%s' 期望布尔类型,实际: %s", identifier, value.getClass().getSimpleName()));
                    }
                    break;

                default:
                    // 其他类型（如 date, array, struct）不做严格验证
                    break;
            }
    }

    /**
     * 检查数值范围
     */
    private void checkRange(String identifier, double value, JsonNode propertyDef) {
        if (propertyDef.has("min")) {
            double min = propertyDef.get("min").asDouble();
            if (value < min) {
                throw BusinessException.badRequest(
                    String.format("属性 '%s' 的值 %.2f 小于最小值 %.2f", identifier, value, min));
            }
        }

        if (propertyDef.has("max")) {
            double max = propertyDef.get("max").asDouble();
            if (value > max) {
                throw BusinessException.badRequest(
                    String.format("属性 '%s' 的值 %.2f 大于最大值 %.2f", identifier, value, max));
            }
        }
    }

    /**
     * 从 JSON 中解析值
     */
    private Object parseValueFromJson(JsonNode json, String field) {
        JsonNode fieldNode = json.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }

        if (fieldNode.isNumber()) {
            return fieldNode.asDouble();
        } else if (fieldNode.isBoolean()) {
            return fieldNode.asBoolean();
        } else if (fieldNode.isTextual()) {
            return fieldNode.asText();
        } else {
            return fieldNode;
        }
    }

    /**
     * 验证参数类型
     */
    private void validateParamType(String serviceId, String paramId, Object value, String expectedType) {
        if (value == null) {
            return;
        }

        switch (expectedType.toLowerCase()) {
            case "int":
            case "float":
            case "double":
                if (!(value instanceof Number)) {
                    throw BusinessException.badRequest(
                        String.format("服务 '%s' 参数 '%s' 期望数值类型", serviceId, paramId));
                }
                break;
            case "string":
                if (!(value instanceof String)) {
                    throw BusinessException.badRequest(
                        String.format("服务 '%s' 参数 '%s' 期望字符串类型", serviceId, paramId));
                }
                break;
            case "boolean":
                if (!(value instanceof Boolean)) {
                    throw BusinessException.badRequest(
                        String.format("服务 '%s' 参数 '%s' 期望布尔类型", serviceId, paramId));
                }
                break;
            default:
                break;
        }
    }
}

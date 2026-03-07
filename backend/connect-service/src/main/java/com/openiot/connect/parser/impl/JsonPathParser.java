package com.openiot.connect.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.connect.parser.ParseException;
import com.openiot.connect.parser.ParseRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON 解析器
 * 使用 JSON Path 风格的字段映射解析 JSON 数据
 *
 * <p>规则配置格式：
 * <pre>
 * {
 *   "mappings": [
 *     {
 *       "source": "$.temperature",   // JSON 路径
 *       "target": "temp",            // 目标属性名
 *       "type": "double"             // 类型（string/int/long/double/boolean）
 *     }
 *   ]
 * }
 * </pre>
 *
 * @author open-iot
 */
@Slf4j
@Component
public class JsonPathParser implements ParseRuleEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "JSON";
    }

    @Override
    public Map<String, Object> parse(String rawData, String ruleConfig) throws ParseException {
        try {
            // 解析规则配置
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            JsonNode mappingsNode = configNode.get("mappings");

            if (mappingsNode == null || !mappingsNode.isArray()) {
                throw ParseException.configError("规则配置必须包含 'mappings' 数组");
            }

            // 解析原始 JSON 数据
            JsonNode dataNode;
            try {
                dataNode = objectMapper.readTree(rawData);
            } catch (JsonProcessingException e) {
                throw ParseException.dataFormatError("原始数据不是合法的 JSON 格式: " + e.getMessage());
            }

            // 执行字段映射
            Map<String, Object> result = new HashMap<>();

            for (JsonNode mapping : mappingsNode) {
                String source = mapping.path("source").asText();
                String target = mapping.path("target").asText();
                String type = mapping.path("type").asText("string");

                // 提取值
                Object value = extractValue(dataNode, source);

                if (value != null) {
                    // 类型转换
                    result.put(target, convertType(value, type));
                }
            }

            log.debug("JSON 解析完成: 映射字段数={}", result.size());
            return result;

        } catch (JsonProcessingException e) {
            throw ParseException.configError("规则配置 JSON 解析失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validateConfig(String ruleConfig) {
        try {
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            JsonNode mappingsNode = configNode.get("mappings");
            return mappingsNode != null && mappingsNode.isArray();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 JSON 节点中提取值
     * 支持简化版 JsonPath：$.field.nested[0]
     */
    private Object extractValue(JsonNode node, String path) {
        // 去除 $. 前缀
        if (path.startsWith("$.")) {
            path = path.substring(2);
        }

        String[] parts = path.split("\\.");
        JsonNode current = node;

        for (String part : parts) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }

            // 处理数组索引 [0]
            if (part.contains("[")) {
                int bracketIndex = part.indexOf("[");
                String fieldName = part.substring(0, bracketIndex);
                int arrayIndex = Integer.parseInt(
                        part.substring(bracketIndex + 1, part.indexOf("]"))
                );

                if (!fieldName.isEmpty()) {
                    current = current.get(fieldName);
                }

                if (current != null && current.isArray() && arrayIndex < current.size()) {
                    current = current.get(arrayIndex);
                } else {
                    return null;
                }
            } else {
                current = current.get(part);
            }
        }

        return toJsonValue(current);
    }

    /**
     * 将 JsonNode 转换为 Java 值
     */
    private Object toJsonValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        if (node.isTextual()) {
            return node.asText();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isLong()) {
            return node.asLong();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isArray() || node.isObject()) {
            return node.toString();
        }

        return node.asText();
    }

    /**
     * 类型转换
     */
    private Object convertType(Object value, String type) {
        if (value == null) {
            return null;
        }

        try {
            return switch (type.toLowerCase()) {
                case "int", "integer" -> {
                    if (value instanceof Number num) yield num.intValue();
                    yield Integer.parseInt(value.toString());
                }
                case "long" -> {
                    if (value instanceof Number num) yield num.longValue();
                    yield Long.parseLong(value.toString());
                }
                case "double", "float" -> {
                    if (value instanceof Number num) yield num.doubleValue();
                    yield Double.parseDouble(value.toString());
                }
                case "boolean", "bool" -> {
                    if (value instanceof Boolean bool) yield bool;
                    yield Boolean.parseBoolean(value.toString());
                }
                default -> value.toString(); // string
            };
        } catch (NumberFormatException e) {
            log.warn("类型转换失败: value={}, type={}, error={}", value, type, e.getMessage());
            return value;
        }
    }
}

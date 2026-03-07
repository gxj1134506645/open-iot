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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则表达式解析器
 * 使用正则表达式从原始数据中提取字段
 *
 * <p>规则配置格式：
 * <pre>
 * {
 *   "pattern": "temp=(\\d+)&hum=(\\d+)",
 *   "groups": [
 *     { "index": 1, "target": "temperature", "type": "int" },
 *     { "index": 2, "target": "humidity", "type": "int" }
 *   ]
 * }
 * </pre>
 *
 * @author open-iot
 */
@Slf4j
@Component
public class RegexParser implements ParseRuleEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "REGEX";
    }

    @Override
    public Map<String, Object> parse(String rawData, String ruleConfig) throws ParseException {
        try {
            // 解析规则配置
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            String patternStr = configNode.path("pattern").asText();
            JsonNode groupsNode = configNode.path("groups");

            if (patternStr == null || patternStr.isEmpty()) {
                throw ParseException.configError("规则配置必须包含 'pattern' 字段");
            }

            if (groupsNode == null || !groupsNode.isArray()) {
                throw ParseException.configError("规则配置必须包含 'groups' 数组");
            }

            // 编译正则表达式
            Pattern pattern;
            try {
                pattern = Pattern.compile(patternStr);
            } catch (PatternSyntaxException e) {
                throw ParseException.configError("正则表达式语法错误: " + e.getMessage());
            }

            // 执行匹配
            Matcher matcher = pattern.matcher(rawData);
            if (!matcher.find()) {
                throw ParseException.dataFormatError("正则表达式未匹配到数据");
            }

            // 提取分组数据
            Map<String, Object> result = new HashMap<>();

            for (JsonNode group : groupsNode) {
                int index = group.path("index").asInt(0);
                String target = group.path("target").asText();
                String type = group.path("type").asText("string");

                if (target == null || target.isEmpty()) {
                    log.warn("正则分组配置缺少 target 字段，跳过");
                    continue;
                }

                // 检查分组索引是否有效
                if (index < 0 || index > matcher.groupCount()) {
                    log.warn("正则分组索引超出范围: index={}, groupCount={}", index, matcher.groupCount());
                    continue;
                }

                String value = matcher.group(index);
                if (value != null) {
                    result.put(target, convertType(value, type));
                }
            }

            log.debug("正则解析完成: 匹配字段数={}", result.size());
            return result;

        } catch (JsonProcessingException e) {
            throw ParseException.configError("规则配置 JSON 解析失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validateConfig(String ruleConfig) {
        try {
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            String pattern = configNode.path("pattern").asText();
            JsonNode groups = configNode.path("groups");

            if (pattern == null || pattern.isEmpty() || groups == null || !groups.isArray()) {
                return false;
            }

            // 验证正则表达式是否可编译
            Pattern.compile(pattern);
            return true;

        } catch (PatternSyntaxException | JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 类型转换
     */
    private Object convertType(String value, String type) {
        if (value == null) {
            return null;
        }

        try {
            return switch (type.toLowerCase()) {
                case "int", "integer" -> Integer.parseInt(value);
                case "long" -> Long.parseLong(value);
                case "double", "float" -> Double.parseDouble(value);
                case "boolean", "bool" -> Boolean.parseBoolean(value);
                default -> value; // string
            };
        } catch (NumberFormatException e) {
            log.warn("类型转换失败: value={}, type={}, 返回原始字符串", value, type);
            return value;
        }
    }
}

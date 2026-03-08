package com.openiot.connect.parser;

import java.util.Map;

/**
 * 解析规则引擎接口
 * 定义设备数据解析的统一接口
 *
 * @author open-iot
 */
public interface ParseRuleEngine {

    /**
     * 获取解析器类型
     *
     * @return 解析器类型标识（JSON/JAVASCRIPT/REGEX/BINARY）
     */
    String getType();

    /**
     * 解析原始数据
     *
     * @param rawData    原始数据字符串
     * @param ruleConfig 规则配置（JSON格式）
     * @return 解析后的属性映射（key=属性名，value=属性值）
     * @throws ParseException 解析失败时抛出
     */
    Map<String, Object> parse(String rawData, String ruleConfig) throws ParseException;

    /**
     * 验证规则配置是否合法
     *
     * @param ruleConfig 规则配置
     * @return 配置是否合法
     */
    boolean validateConfig(String ruleConfig);
}

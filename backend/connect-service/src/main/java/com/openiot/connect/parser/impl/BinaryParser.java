package com.openiot.connect.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.connect.parser.ParseException;
import com.openiot.connect.parser.ParseRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 二进制协议解析器
 * 从二进制数据（十六进制字符串）中提取字段
 *
 * <p>支持的字段类型：
 * <ul>
 *   <li>int8/byte: 1 字节无符号整数</li>
 *   <li>int16/short: 2 字节有符号整数</li>
 *   <li>int32/int: 4 字节有符号整数</li>
 *   <li>int64/long: 8 字节有符号整数</li>
 *   <li>float: 4 字节浮点数</li>
 *   <li>double: 8 字节双精度浮点数</li>
 *   <li>string/hex: 十六进制字符串</li>
 *   <li>utf8/ascii: UTF-8/ASCII 字符串</li>
 * </ul>
 *
 * <p>规则配置格式：
 * <pre>
 * {
 *   "byteOrder": "BIG_ENDIAN",  // 字节序：BIG_ENDIAN 或 LITTLE_ENDIAN
 *   "fields": [
 *     { "offset": 0, "length": 2, "target": "header", "type": "hex" },
 *     { "offset": 2, "length": 4, "target": "temperature", "type": "float" },
 *     { "offset": 6, "length": 2, "target": "humidity", "type": "int16" }
 *   ]
 * }
 * </pre>
 *
 * @author open-iot
 */
@Slf4j
@Component
public class BinaryParser implements ParseRuleEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "BINARY";
    }

    @Override
    public Map<String, Object> parse(String rawData, String ruleConfig) throws ParseException {
        try {
            // 解析规则配置
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            String byteOrderStr = configNode.path("byteOrder").asText("BIG_ENDIAN");
            JsonNode fieldsNode = configNode.path("fields");

            if (fieldsNode == null || !fieldsNode.isArray()) {
                throw ParseException.configError("规则配置必须包含 'fields' 数组");
            }

            // 解析字节序
            ByteOrder byteOrder = "LITTLE_ENDIAN".equalsIgnoreCase(byteOrderStr)
                    ? ByteOrder.LITTLE_ENDIAN
                    : ByteOrder.BIG_ENDIAN;

            // 将十六进制字符串转换为字节数组
            byte[] bytes = hexToBytes(rawData);

            // 提取字段
            Map<String, Object> result = new HashMap<>();

            for (JsonNode field : fieldsNode) {
                int offset = field.path("offset").asInt(0);
                int length = field.path("length").asInt(1);
                String target = field.path("target").asText();
                String type = field.path("type").asText("hex");

                if (target == null || target.isEmpty()) {
                    log.warn("二进制字段配置缺少 target 字段，跳过");
                    continue;
                }

                // 检查偏移和长度是否有效
                if (offset < 0 || length <= 0) {
                    log.warn("无效的字段偏移或长度: offset={}, length={}", offset, length);
                    continue;
                }

                if (offset + length > bytes.length) {
                    log.warn("字段超出数据范围: offset={}, length={}, dataLength={}",
                            offset, length, bytes.length);
                    continue;
                }

                // 提取字段数据
                byte[] fieldBytes = new byte[length];
                System.arraycopy(bytes, offset, fieldBytes, 0, length);

                Object value = parseField(fieldBytes, type, byteOrder);
                result.put(target, value);
            }

            log.debug("二进制解析完成: 字段数={}", result.size());
            return result;

        } catch (JsonProcessingException e) {
            throw ParseException.configError("规则配置 JSON 解析失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ParseException.dataFormatError("数据格式错误: " + e.getMessage());
        }
    }

    @Override
    public boolean validateConfig(String ruleConfig) {
        try {
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            JsonNode fields = configNode.path("fields");
            return fields != null && fields.isArray() && fields.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 十六进制字符串转字节数组
     */
    private byte[] hexToBytes(String hex) {
        // 移除所有空白字符和常见分隔符
        hex = hex.replaceAll("[\\s\\-:]", "");

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须是偶数");
        }

        // 验证是否为有效的十六进制字符串
        if (!hex.matches("^[0-9a-fA-F]+$")) {
            throw new IllegalArgumentException("包含非十六进制字符");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    /**
     * 解析单个字段
     */
    private Object parseField(byte[] bytes, String type, ByteOrder byteOrder) {
        return switch (type.toLowerCase()) {
            case "int8", "byte" -> bytes[0] & 0xFF; // 无符号 1 字节

            case "int8s", "bytes" -> bytes[0]; // 有符号 1 字节

            case "int16", "short" -> ByteBuffer.wrap(bytes)
                    .order(byteOrder)
                    .getShort(); // 有符号 2 字节

            case "uint16" -> ByteBuffer.wrap(bytes)
                    .order(byteOrder)
                    .getShort() & 0xFFFF; // 无符号 2 字节

            case "int32", "int" -> ByteBuffer.wrap(bytes)
                    .order(byteOrder)
                    .getInt(); // 有符号 4 字节

            case "uint32" -> ByteBuffer.wrap(bytes)
                    .order(byteOrder)
                    .getInt() & 0xFFFFFFFFL; // 无符号 4 字节（用 long 存储）

            case "int64", "long" -> ByteBuffer.wrap(bytes)
                    .order(byteOrder)
                    .getLong(); // 有符号 8 字节

            case "float" -> ByteBuffer.wrap(bytes)
                    .order(byteOrder)
                    .getFloat(); // 4 字节浮点

            case "double" -> ByteBuffer.wrap(bytes)
                    .order(byteOrder)
                    .getDouble(); // 8 字节双精度

            case "utf8" -> new String(bytes, StandardCharsets.UTF_8); // UTF-8 字符串

            case "ascii" -> new String(bytes, StandardCharsets.US_ASCII); // ASCII 字符串

            case "hex", "string" -> bytesToHex(bytes); // 十六进制字符串

            default -> {
                log.warn("未知的二进制字段类型: {}, 使用十六进制字符串", type);
                yield bytesToHex(bytes);
            }
        };
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}

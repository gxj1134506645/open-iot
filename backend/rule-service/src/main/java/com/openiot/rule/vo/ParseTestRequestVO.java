package com.openiot.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解析规则测试请求 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "解析规则测试请求")
public class ParseTestRequestVO {

    /**
     * 原始数据
     * 根据规则类型不同，数据格式也不同：
     * - JSON: JSON 字符串
     * - JAVASCRIPT: 任意格式字符串
     * - REGEX: 任意格式字符串
     * - BINARY: 十六进制字符串
     */
    @NotBlank(message = "测试数据不能为空")
    @Schema(description = "原始数据（JSON字符串/普通字符串/十六进制字符串）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String rawData;
}

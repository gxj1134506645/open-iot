package com.openiot.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 解析规则创建 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "解析规则创建请求")
public class ParseRuleCreateVO {

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;

    /**
     * 规则类型（JSON/JAVASCRIPT/REGEX/BINARY）
     */
    @NotBlank(message = "规则类型不能为空")
    @Schema(description = "规则类型：JSON-JSON解析，JAVASCRIPT-脚本解析，REGEX-正则解析，BINARY-二进制解析",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"JSON", "JAVASCRIPT", "REGEX", "BINARY"})
    private String ruleType;

    /**
     * 规则配置（JSON格式）
     * 不同类型规则配置格式：
     * - JSON: {"mappings": [{"source": "$.temperature", "target": "temp", "type": "double"}]}
     * - JAVASCRIPT: {"script": "function parse(data) { return JSON.parse(data); }"}
     * - REGEX: {"pattern": "temp=(\\d+)", "groups": [{"index": 1, "target": "temperature", "type": "int"}]}
     * - BINARY: {"format": "hex", "fields": [{"offset": 0, "length": 2, "target": "header", "type": "string"}]}
     */
    @NotBlank(message = "规则配置不能为空")
    @Schema(description = "规则配置（JSON格式）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleConfig;

    /**
     * 优先级（数字越大优先级越高）
     */
    @Schema(description = "优先级（数字越大优先级越高，默认为0）")
    private Integer priority;

    /**
     * 状态（1=启用，0=禁用）
     */
    @Schema(description = "状态：1-启用，0-禁用（默认为1）")
    private String status;
}

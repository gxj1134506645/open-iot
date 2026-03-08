package com.openiot.rule.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析规则返回 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "解析规则响应")
public class ParseRuleVO {

    /**
     * 主键
     */
    @Schema(description = "规则ID")
    private Long id;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 产品ID
     */
    @Schema(description = "产品ID")
    private Long productId;

    /**
     * 规则名称
     */
    @Schema(description = "规则名称")
    private String ruleName;

    /**
     * 规则类型
     */
    @Schema(description = "规则类型：JSON/JAVASCRIPT/REGEX/BINARY")
    private String ruleType;

    /**
     * 规则配置
     */
    @Schema(description = "规则配置（JSON格式）")
    private String ruleConfig;

    /**
     * 优先级
     */
    @Schema(description = "优先级")
    private Integer priority;

    /**
     * 状态
     */
    @Schema(description = "状态：1-启用，0-禁用")
    private String status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @Schema(description = "创建人ID")
    private Long createBy;

    /**
     * 更新人ID
     */
    @Schema(description = "更新人ID")
    private Long updateBy;
}

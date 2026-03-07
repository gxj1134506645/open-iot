package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 属性定义 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "属性定义")
public class PropertyDefinitionVO {

    /**
     * 属性标识符
     */
    @NotBlank(message = "标识符不能为空")
    @Schema(description = "属性标识符，只允许字母、数字、下划线，以字母或下划线开头，最多50字符")
    private String identifier;

    /**
     * 属性名称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(description = "属性显示名称")
    private String name;

    /**
     * 数据类型: int, float, double, string, boolean, date, array, struct
     */
    @NotBlank(message = "数据类型不能为空")
    @Schema(description = "数据类型")
    private String dataType;

    /**
     * 单位（可选)
     */
    @Schema(description = "单位，如：摄氏度、百分比等")
    private String unit;

    /**
     * 最小值（可选，数值类型时使用)
     */
    @Schema(description = "最小值")
    private String min;

    /**
     * 最大值(可选, 数值类型时使用)
     */
    @Schema(description = "最大值")
    private String max;

    /**
     * 是否只读
     */
    @Schema(description = "是否只读，默认 false")
    private Boolean readonly;

    /**
     * 描述
     */
    @Schema(description = "属性描述")
    private String description;
}

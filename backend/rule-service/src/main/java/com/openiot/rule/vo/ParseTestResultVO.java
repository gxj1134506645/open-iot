package com.openiot.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析测试结果 VO
 *
 * @author open-iot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "解析测试结果")
public class ParseTestResultVO {

    /**
     * 是否解析成功
     */
    @Schema(description = "是否解析成功")
    private Boolean success;

    /**
     * 解析结果（JSON格式）
     */
    @Schema(description = "解析结果（JSON格式）")
    private Object result;

    /**
     * 错误信息（解析失败时）
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 解析耗时（毫秒）
     */
    @Schema(description = "解析耗时（毫秒）")
    private Long durationMs;

    /**
     * 创建成功结果
     */
    public static ParseTestResultVO success(Object result, Long durationMs) {
        return ParseTestResultVO.builder()
                .success(true)
                .result(result)
                .durationMs(durationMs)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ParseTestResultVO fail(String errorMessage, Long durationMs) {
        return ParseTestResultVO.builder()
                .success(false)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }
}

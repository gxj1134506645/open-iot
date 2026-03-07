package com.openiot.device.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务调用返回 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "服务调用返回")
public class ServiceInvokeVO {

    /**
     * 调用记录 ID
     */
    @Schema(description = "调用记录 ID")
    private Long id;

    /**
     * 调用唯一标识（UUID）
     */
    @Schema(description = "调用唯一标识（UUID）")
    private String invokeId;

    /**
     * 设备 ID
     */
    @Schema(description = "设备 ID")
    private Long deviceId;

    /**
     * 服务标识符
     */
    @Schema(description = "服务标识符")
    private String serviceIdentifier;

    /**
     * 服务名称
     */
    @Schema(description = "服务名称")
    private String serviceName;

    /**
     * 调用方式：sync-同步，async-异步
     */
    @Schema(description = "调用方式：sync-同步，async-异步")
    private String invokeType;

    /**
     * 输入参数
     */
    @Schema(description = "输入参数")
    private JsonNode inputParams;

    /**
     * 输出参数（设备响应）
     */
    @Schema(description = "输出参数（设备响应）")
    private JsonNode outputParams;

    /**
     * 状态：pending-待处理，calling-调用中，success-成功，failed-失败，timeout-超时
     */
    @Schema(description = "状态：pending-待处理，calling-调用中，success-成功，failed-失败，timeout-超时")
    private String status;

    /**
     * 错误消息
     */
    @Schema(description = "错误消息")
    private String errorMessage;

    /**
     * 调用时间
     */
    @Schema(description = "调用时间")
    private LocalDateTime invokeTime;

    /**
     * 完成时间
     */
    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

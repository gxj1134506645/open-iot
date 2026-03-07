package com.openiot.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 服务调用请求 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "服务调用请求")
public class ServiceInvokeRequestVO {

    /**
     * 输入参数（JSON 格式）
     */
    @Schema(description = "输入参数")
    private Map<String, Object> inputParams;

    /**
     * 调用类型：sync-同步，async-异步
     * 不指定时使用服务定义中的默认调用方式
     */
    @Schema(description = "调用类型：sync-同步，async-异步")
    private String invokeType;

    /**
     * 超时时间（秒），默认 30 秒
     */
    @Schema(description = "超时时间（秒），默认 30 秒")
    private Integer timeout;
}

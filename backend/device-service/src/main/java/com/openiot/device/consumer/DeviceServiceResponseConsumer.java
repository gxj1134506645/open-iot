package com.openiot.device.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.device.service.DeviceServiceInvokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备服务响应消费者
 *
 * 消费 connect-service 转发的设备服务响应，更新调用记录状态
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceServiceResponseConsumer {

    private final DeviceServiceInvokeService deviceServiceInvokeService;
    private final ObjectMapper objectMapper;

    /**
     * 消费设备服务响应
     *
     * 消息格式：
     * {
     *   "invokeId": "调用ID",
     *   "success": true/false,
     *   "outputParams": { ... },
     *   "errorMessage": "错误消息",
     *   "timestamp": 1234567890
     * }
     *
     * @param message JSON 消息
     */
    @KafkaListener(
            topics = "device-service-response",
            groupId = "${spring.kafka.consumer.group-id:device-service}"
    )
    public void consumeServiceResponse(String message) {
        log.info("消费设备服务响应: {}", message);

        try {
            JsonNode response = objectMapper.readTree(message);

            // 解析响应字段
            String invokeId = response.has("invokeId") ? response.get("invokeId").asText() : null;
            boolean success = response.has("success") && response.get("success").asBoolean();
            JsonNode outputParamsNode = response.get("outputParams");
            String errorMessage = response.has("errorMessage") ? response.get("errorMessage").asText() : null;

            if (invokeId == null) {
                log.warn("响应缺少 invokeId，忽略: {}", message);
                return;
            }

            // 转换输出参数
            Map<String, Object> outputParams = null;
            if (outputParamsNode != null && !outputParamsNode.isNull()) {
                outputParams = objectMapper.convertValue(outputParamsNode, HashMap.class);
            }

            // 调用服务处理响应
            deviceServiceInvokeService.handleDeviceServiceResponse(invokeId, outputParams, success, errorMessage);

            log.info("设备服务响应处理完成: invokeId={}, success={}", invokeId, success);

        } catch (Exception e) {
            log.error("消费设备服务响应失败: {}", message, e);
        }
    }
}

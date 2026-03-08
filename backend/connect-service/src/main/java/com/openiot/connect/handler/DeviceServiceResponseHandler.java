package com.openiot.connect.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 设备服务响应处理器
 *
 * 负责处理设备上报的服务调用响应：
 * - 订阅设备响应主题
 * - 解析响应数据
 * - 转发到 device-service 处理
 *
 * MQTT 响应主题格式：/sys/{productKey}/{deviceKey}/service/response
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceServiceResponseHandler {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 响应主题：device-service-response
     */
    private static final String TOPIC_SERVICE_RESPONSE = "device-service-response";

    /**
     * 处理设备服务响应（从 MQTT Broker 接收）
     *
     * 消息格式：
     * {
     *   "invokeId": "调用ID",
     *   "productKey": "产品密钥",
     *   "deviceKey": "设备密钥",
     *   "serviceIdentifier": "服务标识符",
     *   "code": 200,  // 响应码，200 表示成功
     *   "data": { ... },  // 输出参数
     *   "message": "错误消息（失败时）"
     * }
     *
     * @param message JSON 消息
     */
    @KafkaListener(
            topics = "device-mqtt-response",
            groupId = "${spring.kafka.consumer.group-id:connect-service}"
    )
    public void handleServiceResponse(String message) {
        log.info("收到设备服务响应: {}", message);

        try {
            JsonNode response = objectMapper.readTree(message);

            // 解析响应字段
            String invokeId = response.has("invokeId") ? response.get("invokeId").asText() : null;
            String productKey = response.has("productKey") ? response.get("productKey").asText() : null;
            String deviceKey = response.has("deviceKey") ? response.get("deviceKey").asText() : null;
            String serviceIdentifier = response.has("serviceIdentifier") ? response.get("serviceIdentifier").asText() : null;
            int code = response.has("code") ? response.get("code").asInt() : 500;
            JsonNode data = response.get("data");
            String errorMsg = response.has("message") ? response.get("message").asText() : null;

            if (invokeId == null) {
                log.warn("响应缺少 invokeId，忽略: {}", message);
                return;
            }

            // 判断是否成功
            boolean success = (code == 200);

            // 转发到 device-service 处理
            forwardToDeviceService(invokeId, data, success, errorMsg);

            log.info("设备服务响应已转发: invokeId={}, success={}", invokeId, success);

        } catch (Exception e) {
            log.error("处理设备服务响应失败: {}", message, e);
        }
    }

    /**
     * 转发响应到 device-service
     */
    private void forwardToDeviceService(String invokeId, JsonNode data, boolean success, String errorMessage) {
        try {
            // 构造转发消息
            var forwardMessage = new java.util.HashMap<String, Object>();
            forwardMessage.put("invokeId", invokeId);
            forwardMessage.put("success", success);
            forwardMessage.put("outputParams", data);
            forwardMessage.put("errorMessage", errorMessage);
            forwardMessage.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(forwardMessage);
            kafkaTemplate.send(TOPIC_SERVICE_RESPONSE, invokeId, jsonMessage);

        } catch (Exception e) {
            log.error("转发设备服务响应失败: invokeId={}", invokeId, e);
        }
    }
}

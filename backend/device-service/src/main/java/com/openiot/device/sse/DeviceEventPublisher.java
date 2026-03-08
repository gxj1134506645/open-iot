package com.openiot.device.sse;

import com.openiot.device.influxdb.DeviceEventPoint;
import com.openiot.device.influxdb.DevicePropertyPoint;
import com.openiot.device.influxdb.DeviceStatusPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * 设备事件发布器
 * 监听 Kafka 消息，通过 SSE 推送给前端
 *
 * @author OpenIoT Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceEventPublisher {

    private final SseEmitterManager emitterManager;

    /**
     * 监听设备数据上报
     */
    @KafkaListener(topics = "device-data", groupId = "sse-publisher-group")
    public void onDeviceData(@Payload String message,
                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                             @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        log.debug("收到设备数据: topic={}, partition={}, offset={}", topic, partition, offset);

        try {
            // 解析消息（简化处理）
            // 实际应该解析 JSON 并提取设备ID、数据等
            Map<String, Object> data = parseMessage(message);

            Long deviceId = getLongValue(data, "deviceId");
            if (deviceId != null) {
                // 推送设备数据更新
                emitterManager.sendMessage("all", SseMessage.builder()
                        .type("device_data")
                        .data(Map.of(
                                "deviceId", deviceId,
                                "data", data,
                                "timestamp", System.currentTimeMillis()
                        ))
                        .build());
            }
        } catch (Exception e) {
            log.error("处理设备数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听设备状态变化
     */
    @KafkaListener(topics = "device-status-change", groupId = "sse-publisher-group")
    public void onDeviceStatusChange(@Payload String message) {
        log.debug("收到设备状态变化: {}", message);

        try {
            Map<String, Object> data = parseMessage(message);

            Long deviceId = getLongValue(data, "deviceId");
            String status = getStringValue(data, "status");

            if (deviceId != null && status != null) {
                emitterManager.sendDeviceStatusChange(deviceId, status);
            }
        } catch (Exception e) {
            log.error("处理设备状态变化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听设备属性变化
     */
    @KafkaListener(topics = "device-property-change", groupId = "sse-publisher-group")
    public void onDevicePropertyChange(@Payload String message) {
        log.debug("收到设备属性变化: {}", message);

        try {
            Map<String, Object> data = parseMessage(message);

            Long deviceId = getLongValue(data, "deviceId");
            String propertyIdentifier = getStringValue(data, "propertyIdentifier");
            Object value = data.get("value");

            if (deviceId != null && propertyIdentifier != null) {
                emitterManager.sendDevicePropertyChange(deviceId, propertyIdentifier, value);
            }
        } catch (Exception e) {
            log.error("处理设备属性变化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听设备事件
     */
    @KafkaListener(topics = "device-event", groupId = "sse-publisher-group")
    public void onDeviceEvent(@Payload String message) {
        log.debug("收到设备事件: {}", message);

        try {
            Map<String, Object> data = parseMessage(message);

            Long deviceId = getLongValue(data, "deviceId");
            String eventIdentifier = getStringValue(data, "eventIdentifier");
            String eventType = getStringValue(data, "eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> outputParams = (Map<String, Object>) data.get("outputParams");

            if (deviceId != null && eventIdentifier != null) {
                emitterManager.sendDeviceEvent(deviceId, eventIdentifier, eventType, outputParams);
            }
        } catch (Exception e) {
            log.error("处理设备事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听告警消息
     */
    @KafkaListener(topics = "alert", groupId = "sse-publisher-group")
    public void onAlert(@Payload String message) {
        log.info("收到告警消息: {}", message);

        try {
            Map<String, Object> data = parseMessage(message);

            Long alertId = getLongValue(data, "alertId");
            Long deviceId = getLongValue(data, "deviceId");
            String alertLevel = getStringValue(data, "alertLevel");
            String alertTitle = getStringValue(data, "alertTitle");

            if (alertId != null && deviceId != null) {
                emitterManager.sendAlert(alertId, deviceId, alertLevel, alertTitle);
            }
        } catch (Exception e) {
            log.error("处理告警消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析 JSON 消息
     * 简化实现，实际应该使用 ObjectMapper
     */
    private Map<String, Object> parseMessage(String message) {
        // TODO: 使用 Jackson 解析 JSON
        // 这里简化处理，实际需要完整的 JSON 解析
        return Map.of("raw", message);
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
}

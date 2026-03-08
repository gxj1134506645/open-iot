package com.openiot.device.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * SSE 消息
 *
 * @author OpenIoT Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessage {

    /**
     * 消息ID（可选）
     */
    private String id;

    /**
     * 消息类型
     * - connected: 连接成功
     * - heartbeat: 心跳
     * - device_status_change: 设备状态变化
     * - device_property_change: 设备属性变化
     * - device_event: 设备事件
     * - alert: 告警
     */
    private String type;

    /**
     * 消息数据
     */
    private Map<String, Object> data;

    /**
     * 创建带 ID 的消息
     */
    public static SseMessage withId(String type, Map<String, Object> data) {
        return SseMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .data(data)
                .build();
    }

    /**
     * 创建不带 ID 的消息
     */
    public static SseMessage withoutId(String type, Map<String, Object> data) {
        return SseMessage.builder()
                .type(type)
                .data(data)
                .build();
    }
}

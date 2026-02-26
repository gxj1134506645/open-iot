package com.openiot.device.service;

import com.openiot.common.kafka.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 设备数据上报服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataReportService {

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    private static final String DEVICE_EVENTS_TOPIC = "device-events";
    private static final String RAW_EVENTS_TOPIC = "raw-events";

    /**
     * 上报设备数据（HTTP）
     */
    public void reportData(String deviceId, String tenantId, String eventType, Object payload, String rawPayload) {
        // 构建事件信封
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .deviceId(deviceId)
                .eventType(eventType)
                .protocol(EventEnvelope.PROTOCOL_HTTP)
                .payload(payload)
                .rawPayload(rawPayload)
                .timestamp(System.currentTimeMillis())
                .traceId(UUID.randomUUID().toString().substring(0, 16))
                .build();

        // 发送到 Kafka
        kafkaTemplate.send(DEVICE_EVENTS_TOPIC, envelope.getKafkaKey(), envelope);
        kafkaTemplate.send(RAW_EVENTS_TOPIC, envelope.getKafkaKey(), envelope);

        log.info("设备数据上报成功: deviceId={}, eventType={}", deviceId, eventType);
    }
}

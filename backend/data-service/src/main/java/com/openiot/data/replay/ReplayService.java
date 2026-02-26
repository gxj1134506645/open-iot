package com.openiot.data.replay;

import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.common.mongodb.document.RawEventDocument;
import com.openiot.data.parser.TrajectoryParser;
import com.openiot.data.repository.RawEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 重放服务
 * 支持按时间窗口和手动触发重放
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayService {

    private final RawEventRepository rawEventRepository;
    private final TrajectoryParser trajectoryParser;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    private static final String DEVICE_EVENTS_TOPIC = "device-events";

    /**
     * 按时间窗口重放
     */
    public int replayByTimeWindow(String tenantId, String deviceId,
                                  LocalDateTime startTime, LocalDateTime endTime) {
        List<RawEventDocument> events = rawEventRepository
                .findByTenantIdAndDeviceIdAndTimestampBetween(tenantId, deviceId, startTime, endTime);

        log.info("开始重放: tenant={}, device={}, count={}", tenantId, deviceId, events.size());

        int successCount = 0;
        for (RawEventDocument event : events) {
            try {
                trajectoryParser.parseAndSave(event);
                successCount++;
            } catch (Exception e) {
                log.error("重放失败: eventId={}", event.getEventId(), e);
            }
        }

        log.info("重放完成: 成功={}/{}", successCount, events.size());
        return successCount;
    }

    /**
     * 手动触发单条重放
     */
    public boolean replaySingle(String eventId) {
        return rawEventRepository.findById(eventId)
                .map(event -> {
                    try {
                        trajectoryParser.parseAndSave(event);
                        log.info("单条重放成功: eventId={}", eventId);
                        return true;
                    } catch (Exception e) {
                        log.error("单条重放失败: eventId={}", eventId, e);
                        return false;
                    }
                })
                .orElse(false);
    }

    /**
     * 重新发送到 Kafka
     */
    public boolean resendToKafka(String eventId) {
        return rawEventRepository.findById(eventId)
                .map(event -> {
                    try {
                        EventEnvelope envelope = EventEnvelope.builder()
                                .eventId(event.getEventId())
                                .tenantId(event.getTenantId())
                                .deviceId(event.getDeviceId())
                                .eventType(event.getEventType())
                                .protocol(event.getProtocol())
                                .payload(event.getParsedPayload())
                                .rawPayload(event.getRawPayload())
                                .timestamp(System.currentTimeMillis())
                                .traceId("replay-" + eventId.substring(0, 8))
                                .build();

                        kafkaTemplate.send(DEVICE_EVENTS_TOPIC, envelope.getKafkaKey(), envelope);
                        log.info("重新发送到Kafka: eventId={}", eventId);
                        return true;
                    } catch (Exception e) {
                        log.error("重新发送失败: eventId={}", eventId, e);
                        return false;
                    }
                })
                .orElse(false);
    }
}

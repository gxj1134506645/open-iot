package com.openiot.data.service;

import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.common.mongodb.document.RawEventDocument;
import com.openiot.data.repository.RawEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 原始事件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RawEventService {

    private final RawEventRepository rawEventRepository;

    /**
     * 保存原始事件
     */
    public void saveRawEvent(EventEnvelope event) {
        RawEventDocument document = RawEventDocument.builder()
                .eventId(event.getEventId())
                .tenantId(event.getTenantId())
                .deviceId(event.getDeviceId())
                .eventType(event.getEventType())
                .protocol(event.getProtocol())
                .rawPayload(event.getRawPayload())
                .parsedPayload(event.getPayload())
                .timestamp(convertToLocalDateTime(event.getTimestamp()))
                .processed(false)
                .build();

        rawEventRepository.save(document);
        log.debug("原始事件已保存: eventId={}", event.getEventId());
    }

    /**
     * 标记为已处理
     */
    public void markAsProcessed(String eventId, String result) {
        rawEventRepository.findById(eventId).ifPresent(doc -> {
            doc.setProcessed(true);
            doc.setProcessResult(result);
            doc.setUpdateTime(LocalDateTime.now());
            rawEventRepository.save(doc);
        });
    }

    private LocalDateTime convertToLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}

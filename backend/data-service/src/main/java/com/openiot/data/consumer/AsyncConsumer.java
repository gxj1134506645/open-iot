package com.openiot.data.consumer;

import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.data.service.RawEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 异步消费者
 * 持久化原始数据到 MongoDB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncConsumer {

    private final RawEventService rawEventService;

    @KafkaListener(
            topics = "raw-events",
            groupId = "async-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope event = record.value();
        log.debug("异步消费事件: eventId={}, deviceId={}", event.getEventId(), event.getDeviceId());

        try {
            // 持久化到 MongoDB
            rawEventService.saveRawEvent(event);

            // 确认消息
            ack.acknowledge();

        } catch (Exception e) {
            log.error("异步消费异常: eventId={}", event.getEventId(), e);
            // 不确认，触发重试
        }
    }
}

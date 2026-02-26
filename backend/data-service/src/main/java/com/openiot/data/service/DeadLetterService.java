package com.openiot.data.service;

import com.openiot.common.mongodb.document.DeadLetterDocument;
import com.openiot.data.repository.DeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 死信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final DeadLetterRepository deadLetterRepository;

    /**
     * 创建死信记录
     */
    public void createDeadLetter(String originalEventId, String tenantId, String deviceId,
                                 String rawPayload, String failureReason) {
        DeadLetterDocument document = DeadLetterDocument.builder()
                .originalEventId(originalEventId)
                .tenantId(tenantId)
                .deviceId(deviceId)
                .rawPayload(rawPayload)
                .failureReason(failureReason)
                .retryCount(0)
                .status(DeadLetterDocument.STATUS_PENDING)
                .createTime(LocalDateTime.now())
                .build();

        deadLetterRepository.save(document);
        log.warn("死信记录已创建: eventId={}, reason={}", originalEventId, failureReason);
    }

    /**
     * 获取待重试的死信列表
     */
    public List<DeadLetterDocument> getPendingDeadLetters() {
        return deadLetterRepository.findByStatus(DeadLetterDocument.STATUS_PENDING);
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount(String id) {
        deadLetterRepository.findById(id).ifPresent(doc -> {
            doc.incrementRetryCount();
            deadLetterRepository.save(doc);
        });
    }

    /**
     * 标记为已解决
     */
    public void markResolved(String id) {
        deadLetterRepository.findById(id).ifPresent(doc -> {
            doc.markResolved();
            deadLetterRepository.save(doc);
            log.info("死信已解决: id={}", id);
        });
    }
}

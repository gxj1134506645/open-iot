package com.openiot.data.scheduler;

import com.openiot.common.mongodb.document.RawEventDocument;
import com.openiot.data.parser.TrajectoryParser;
import com.openiot.data.repository.RawEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析任务调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParseScheduler {

    private final RawEventRepository rawEventRepository;
    private final TrajectoryParser trajectoryParser;

    /**
     * 每 5 秒执行一次解析任务
     */
    @Scheduled(fixedRate = 5000)
    public void parseRawEvents() {
        List<RawEventDocument> unprocessed = rawEventRepository.findByProcessedFalse();

        if (unprocessed.isEmpty()) {
            return;
        }

        log.debug("待解析事件数量: {}", unprocessed.size());

        for (RawEventDocument event : unprocessed) {
            try {
                trajectoryParser.parseAndSave(event);
            } catch (Exception e) {
                log.error("解析失败: eventId={}", event.getEventId(), e);
            }
        }
    }
}

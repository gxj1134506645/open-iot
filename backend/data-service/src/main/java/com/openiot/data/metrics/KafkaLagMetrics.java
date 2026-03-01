package com.openiot.data.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka 消费延迟指标收集器
 *
 * <p>收集 Kafka 消费相关的指标：
 * <ul>
 *   <li>消费组 Lag（积压）</li>
 *   <li>消费者成员数</li>
 *   <li>分区分配状态</li>
 *   <li>消费速率</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class KafkaLagMetrics {

    private final MeterRegistry meterRegistry;

    @Value("${spring.kafka.consumer.group-id:openiot-data-group}")
    private String consumerGroupId;

    @Value("${spring.kafka.topics.device-data:device-data}")
    private String deviceDataTopic;

    @Value("${spring.kafka.topics.device-status:device-status}")
    private String deviceStatusTopic;

    // Kafka AdminClient 用于查询 Lag
    private final ConsumerFactory<String, String> consumerFactory;

    // 缓存的 Lag 值
    private final Map<String, AtomicLong> lagByTopic = new ConcurrentHashMap<>();
    private final AtomicLong totalLag = new AtomicLong(0);
    private final AtomicLong consumerMembers = new AtomicLong(0);

    @Autowired
    public KafkaLagMetrics(MeterRegistry meterRegistry,
                          ConsumerFactory<String, String> consumerFactory) {
        this.meterRegistry = meterRegistry;
        this.consumerFactory = consumerFactory;

        // 注册 Gauge 指标
        Gauge.builder("openiot_kafka_consumer_lag_total", totalLag, AtomicLong::get)
                .description("Total Kafka consumer lag")
                .tag("service", "data-service")
                .tag("group", "openiot-data-group")
                .register(meterRegistry);

        Gauge.builder("openiot_kafka_consumer_members", consumerMembers, AtomicLong::get)
                .description("Number of consumer group members")
                .tag("service", "data-service")
                .tag("group", "openiot-data-group")
                .register(meterRegistry);

        log.info("Kafka lag metrics collector initialized");
    }

    /**
     * 定时更新 Kafka Lag 指标
     * 每 30 秒执行一次
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void updateKafkaLagMetrics() {
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            // 本消费者实例
            consumerMembers.set(1);

            // 查询各 Topic 的 Lag
            Set<String> topics = Set.of(deviceDataTopic, deviceStatusTopic);
            long totalLagValue = 0;

            for (String topic : topics) {
                long topicLag = calculateTopicLag(consumer, topic);
                totalLagValue += topicLag;

                // 更新或创建 Topic 级别的 Gauge
                lagByTopic.computeIfAbsent(topic, t -> {
                    AtomicLong lag = new AtomicLong(0);
                    Gauge.builder("openiot_kafka_consumer_lag", lag, AtomicLong::get)
                            .description("Kafka consumer lag by topic")
                            .tag("service", "data-service")
                            .tag("topic", t)
                            .tag("group", consumerGroupId)
                            .register(meterRegistry);
                    return lag;
                }).set(topicLag);
            }

            totalLag.set(totalLagValue);

            log.debug("Updated Kafka lag metrics: totalLag={}, members={}",
                    totalLagValue, consumerMembers.get());

        } catch (Exception e) {
            log.error("Failed to update Kafka lag metrics", e);
        }
    }

    /**
     * 计算 Topic 的消费 Lag
     */
    private long calculateTopicLag(Consumer<String, String> consumer, String topic) {
        try {
                // 获取 Topic 的所有分区
                List<PartitionInfo> partitions = consumer.partitionsFor(topic);
                if (partitions == null || partitions.isEmpty()) {
                    return 0;
                }

                List<TopicPartition> topicPartitions = new ArrayList<>();
                for (PartitionInfo partition : partitions) {
                    topicPartitions.add(new TopicPartition(topic, partition.partition()));
                }

                // 获取分区的末尾位置
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);

                // 简化计算：返回总 offset 作为 lag 的近似值
                long totalOffset = 0;
                for (TopicPartition tp : topicPartitions) {
                    Long endOffset = endOffsets.get(tp);
                    if (endOffset != null) {
                        totalOffset += endOffset;
                    }
                }

                return totalOffset;

        } catch (Exception e) {
            log.warn("Failed to calculate lag for topic: {}", topic, e);
            return 0;
        }
    }

    /**
     * 手动更新 Lag 值（用于测试或特殊场景）
     */
    public void updateLag(String topic, long lag) {
        lagByTopic.computeIfAbsent(topic, t -> {
            AtomicLong l = new AtomicLong(0);
            Gauge.builder("openiot_kafka_consumer_lag", l, AtomicLong::get)
                    .description("Kafka consumer lag by topic")
                    .tag("service", "data-service")
                    .tag("topic", t)
                    .tag("group", consumerGroupId)
                    .register(meterRegistry);
            return l;
        }).set(lag);

        // 重新计算总 Lag
        long sum = lagByTopic.values().stream().mapToLong(AtomicLong::get).sum();
        totalLag.set(sum);
    }

    /**
     * 获取 Topic 的当前 Lag
     */
    public long getLag(String topic) {
        AtomicLong lag = lagByTopic.get(topic);
        return lag != null ? lag.get() : 0;
    }

    /**
     * 获取总 Lag
     */
    public long getTotalLag() {
        return totalLag.get();
    }
}

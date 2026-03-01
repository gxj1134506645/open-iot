package com.openiot.common.observability.config;

import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Redis (Lettuce) 指标配置
 *
 * <p>配置 Lettuce Redis 客户端指标的自动采集。
 * 指标前缀：lettuce
 *
 * <h3>关键指标：</h3>
 * <ul>
 *   <li>lettuce.command.completion - 命令完成延迟</li>
 *   <li>lettuce.connection.active - 活跃连接数</li>
 *   <li>lettuce.connection.idle - 空闲连接数</li>
 *   <li>lettuce.connection.pending - 等待中的命令数</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({io.lettuce.core.RedisClient.class, MeterRegistry.class})
@ConditionalOnProperty(prefix = "openiot.observability.metrics", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
public class RedisMetricsConfig {

    /**
     * 配置 Lettuce 客户端资源
     *
     * <p>使用 Micrometer 记录 Redis 命令延迟。
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources lettuceClientResources(MeterRegistry meterRegistry) {
        log.info("Lettuce Redis metrics binding enabled");

        MicrometerOptions options = MicrometerOptions.builder()
                .histogram(true)
                .build();

        return ClientResources.builder()
                .commandLatencyRecorder(new MicrometerCommandLatencyRecorder(meterRegistry, options))
                .build();
    }
}

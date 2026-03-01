package com.openiot.common.core.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 服务健康检查配置
 * 为各服务提供统一的健康检查能力
 */
@Configuration
public class HealthConfig {

    /**
     * Redis 健康检查
     */
    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return () -> {
            try {
                redisConnectionFactory.getConnection().ping();
                return Health.up().withDetail("redis", "available").build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("redis", "unavailable")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}

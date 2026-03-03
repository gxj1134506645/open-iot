package com.openiot.common.redis.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 自动配置类
 * 配置 Redisson 客户端，用于分布式锁、分布式对象等高级功能
 *
 * <p>Redisson 与 RedisTemplate 的分工：
 * <ul>
 *   <li><b>RedisTemplate</b>：简单缓存操作（String、Hash、List、Set、ZSet）</li>
 *   <li><b>Redisson</b>：分布式锁、分布式对象、布隆过滤器等高级功能</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
public class RedissonConfig {

    /**
     * 自定义 Redisson 配置
     * 优化连接池、超时等参数
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            // 使用单机模式配置（根据 spring.redis.* 自动配置）
            // 如需集群/哨兵模式，在 application.yml 中配置 spring.redis.cluster/sentinel
            config.setThreads(16)  // 工作线程数
                    .setNettyThreads(32)  // Netty 线程数
                    .setTransportMode(org.redisson.config.TransportMode.NIO);  // 使用 NIO 模式
        };
    }
}

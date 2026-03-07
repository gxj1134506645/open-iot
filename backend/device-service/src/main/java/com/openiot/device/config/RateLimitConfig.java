package com.openiot.device.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * API 限流配置
 * <p>
 * 使用 Guava RateLimiter 实现基于 IP 的令牌桶限流算法。
 * 每个独立 IP 地址维护一个 RateLimiter 实例，支持配置每秒请求数。
 * </p>
 *
 * <h3>配置项：</h3>
 * <ul>
 *     <li>openiot.rate-limit.permits-per-second: 每秒允许的请求数，默认 10</li>
 *     <li>openiot.rate-limit.cache-expire-minutes: IP 缓存过期时间（分钟），默认 10</li>
 * </ul>
 *
 * @author open-iot
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    /**
     * 每秒允许的请求数
     */
    @Value("${openiot.rate-limit.permits-per-second:10}")
    private double permitsPerSecond;

    /**
     * IP 缓存过期时间（分钟）
     * 长时间不活跃的 IP 对应的限流器会被清理
     */
    @Value("${openiot.rate-limit.cache-expire-minutes:10}")
    private int cacheExpireMinutes;

    /**
     * IP 地址到限流器的缓存
     * 使用 Guava Cache 自动清理过期条目
     */
    private Cache<String, RateLimiter> rateLimiterCache;

    /**
     * 获取指定 IP 的限流器
     * 如果不存在则创建新的限流器
     *
     * @param clientIp 客户端 IP 地址
     * @return 对应的 RateLimiter 实例
     */
    public RateLimiter getRateLimiter(String clientIp) {
        if (rateLimiterCache == null) {
            synchronized (this) {
                if (rateLimiterCache == null) {
                    rateLimiterCache = CacheBuilder.newBuilder()
                            .expireAfterAccess(cacheExpireMinutes, TimeUnit.MINUTES)
                            .maximumSize(10000)  // 最多缓存 10000 个 IP
                            .build();
                    log.info("限流器缓存初始化完成: permitsPerSecond={}, cacheExpireMinutes={}",
                            permitsPerSecond, cacheExpireMinutes);
                }
            }
        }

        return rateLimiterCache.asMap()
                .computeIfAbsent(clientIp, ip -> {
                    RateLimiter limiter = RateLimiter.create(permitsPerSecond);
                    log.debug("为 IP [{}] 创建限流器, permitsPerSecond={}", ip, permitsPerSecond);
                    return limiter;
                });
    }

    /**
     * 尝试获取许可（非阻塞）
     *
     * @param clientIp 客户端 IP 地址
     * @return true 表示获取成功，false 表示被限流
     */
    public boolean tryAcquire(String clientIp) {
        RateLimiter limiter = getRateLimiter(clientIp);
        return limiter.tryAcquire();
    }

    /**
     * 尝试获取指定数量的许可（非阻塞）
     *
     * @param clientIp 客户端 IP 地址
     * @param permits  请求数量
     * @return true 表示获取成功，false 表示被限流
     */
    public boolean tryAcquire(String clientIp, int permits) {
        RateLimiter limiter = getRateLimiter(clientIp);
        return limiter.tryAcquire(permits);
    }

    /**
     * 获取当前配置的每秒许可数
     *
     * @return 每秒允许的请求数
     */
    public double getPermitsPerSecond() {
        return permitsPerSecond;
    }

    /**
     * 获取当前缓存的 IP 数量
     * 用于监控和统计
     *
     * @return 缓存中的 IP 数量
     */
    public long getCacheSize() {
        return rateLimiterCache != null ? rateLimiterCache.size() : 0;
    }
}

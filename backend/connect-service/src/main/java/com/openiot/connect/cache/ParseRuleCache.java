package com.openiot.connect.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.connect.parser.ParseException;
import com.openiot.connect.parser.ParseRuleEngine;
import com.openiot.connect.parser.ParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 解析规则缓存
 * 使用 Caffeine/ConcurrentHashMap 缓存解析规则配置
 * 支持 Redis Pub/Sub 热更新
 *
 * <p>缓存策略：
 * <ul>
 *   <li>本地内存缓存（ConcurrentHashMap）</li>
 *   <li>30 分钟 TTL</li>
 *   <li>Redis Pub/Sub 通知实时刷新</li>
 * </ul>
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParseRuleCache implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ParserFactory parserFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Redis 发布/订阅通道名称
     */
    private static final String PARSE_RULE_UPDATE_CHANNEL = "openiot:parse_rule:update";

    /**
     * 规则缓存 Key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "openiot:cache:parse_rule:";

    /**
     * 本地缓存（产品ID -> 规则配置列表）
     */
    private final ConcurrentHashMap<Long, List<RuleConfig>> localCache = new ConcurrentHashMap<>();

    /**
     * 缓存过期时间（毫秒）
     */
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30);

    /**
     * 缓存时间戳
     */
    private final ConcurrentHashMap<Long, Long> cacheTimestamp = new ConcurrentHashMap<>();

    /**
     * 初始化：订阅 Redis 消息
     */
    @PostConstruct
    public void init() {
        try {
            // 创建 Redis 消息监听容器
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(redisTemplate.getConnectionFactory());
            container.addMessageListener(this, new ChannelTopic(PARSE_RULE_UPDATE_CHANNEL));
            container.afterPropertiesSet();
            container.start();

            log.info("ParseRuleCache 初始化完成，订阅通道: {}", PARSE_RULE_UPDATE_CHANNEL);
        } catch (Exception e) {
            log.error("ParseRuleCache 初始化失败", e);
        }
    }

    /**
     * 获取产品的解析规则配置
     *
     * @param productId 产品ID
     * @return 规则配置列表（按优先级排序）
     */
    public Optional<List<RuleConfig>> getRuleConfigs(Long productId) {
        // 检查缓存是否过期
        if (isCacheExpired(productId)) {
            localCache.remove(productId);
            cacheTimestamp.remove(productId);
            return Optional.empty();
        }

        List<RuleConfig> configs = localCache.get(productId);
        return Optional.ofNullable(configs);
    }

    /**
     * 缓存产品的解析规则配置
     *
     * @param productId 产品ID
     * @param configs   规则配置列表
     */
    public void putRuleConfigs(Long productId, List<RuleConfig> configs) {
        localCache.put(productId, configs);
        cacheTimestamp.put(productId, System.currentTimeMillis());
        log.debug("缓存解析规则: productId={}, 规则数={}", productId, configs.size());
    }

    /**
     * 使缓存失效
     *
     * @param productId 产品ID
     */
    public void invalidate(Long productId) {
        localCache.remove(productId);
        cacheTimestamp.remove(productId);
        log.info("缓存失效: productId={}", productId);
    }

    /**
     * 使所有缓存失效
     */
    public void invalidateAll() {
        localCache.clear();
        cacheTimestamp.clear();
        log.info("所有缓存已失效");
    }

    /**
     * 检查缓存是否过期
     */
    private boolean isCacheExpired(Long productId) {
        Long timestamp = cacheTimestamp.get(productId);
        if (timestamp == null) {
            return true;
        }
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
    }

    /**
     * 处理 Redis 消息（规则更新通知）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            log.info("收到规则更新通知: {}", body);

            // 解析消息
            var node = objectMapper.readTree(body);
            String action = node.path("action").asText();
            Long productId = node.path("productId").asLong();

            // 使缓存失效
            invalidate(productId);

            log.info("缓存已刷新: action={}, productId={}", action, productId);

        } catch (JsonProcessingException e) {
            log.error("解析规则更新消息失败: {}", new String(message.getBody()), e);
        } catch (Exception e) {
            log.error("处理规则更新消息异常", e);
        }
    }

    /**
     * 解析数据
     * 遍历产品的所有规则，使用第一个匹配的规则解析数据
     *
     * @param productId 产品ID
     * @param rawData   原始数据
     * @return 解析结果（如果没有任何规则匹配则返回空）
     */
    public Optional<Map<String, Object>> parseData(Long productId, String rawData) {
        Optional<List<RuleConfig>> configsOpt = getRuleConfigs(productId);

        if (configsOpt.isEmpty()) {
            log.debug("产品 {} 没有缓存的解析规则", productId);
            return Optional.empty();
        }

        List<RuleConfig> configs = configsOpt.get();

        // 按优先级遍历规则
        for (RuleConfig config : configs) {
            try {
                ParseRuleEngine parser = parserFactory.getParser(config.getRuleType());
                Map<String, Object> result = parser.parse(rawData, config.getRuleConfig());

                log.debug("解析成功: productId={}, ruleId={}, ruleType={}",
                        productId, config.getRuleId(), config.getRuleType());

                return Optional.of(result);

            } catch (ParseException e) {
                log.debug("规则 {} 解析失败，尝试下一个规则: {}", config.getRuleId(), e.getMessage());
                // 继续尝试下一个规则
            }
        }

        log.warn("产品 {} 的所有规则都无法解析数据", productId);
        return Optional.empty();
    }

    /**
     * 规则配置（缓存项）
     */
    public static class RuleConfig {
        /**
         * 规则ID
         */
        private Long ruleId;

        /**
         * 产品ID
         */
        private Long productId;

        /**
         * 规则类型
         */
        private String ruleType;

        /**
         * 规则配置（JSON字符串）
         */
        private String ruleConfig;

        /**
         * 优先级
         */
        private Integer priority;

        // Getters and Setters

        public Long getRuleId() {
            return ruleId;
        }

        public void setRuleId(Long ruleId) {
            this.ruleId = ruleId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getRuleType() {
            return ruleType;
        }

        public void setRuleType(String ruleType) {
            this.ruleType = ruleType;
        }

        public String getRuleConfig() {
            return ruleConfig;
        }

        public void setRuleConfig(String ruleConfig) {
            this.ruleConfig = ruleConfig;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }
}

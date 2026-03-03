package com.openiot.common.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 自动配置类
 * 配置 RedisTemplate 序列化方式和连接池
 *
 * <p><b>Redis 使用规范：</b>
 * <ul>
 *   <li><b>简单缓存操作</b>：使用 RedisTemplate（本类配置）
 *     <ul>
 *       <li>String 操作：缓存对象、计数器</li>
 *       <li>Hash 操作：存储对象属性</li>
 *       <li>List/Set/ZSet 操作：队列、集合、排行榜</li>
 *     </ul>
 *   </li>
 *   <li><b>分布式高级功能</b>：使用 Redisson（见 {@link RedissonConfig}）
 *     <ul>
 *       <li>分布式锁：RLock</li>
 *       <li>分布式对象：RMap、RList 等</li>
 *       <li>布隆过滤器、限流器等</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>序列化配置：</b>
 * <ul>
 *   <li>Key：String 序列化（可读性强）</li>
 *   <li>Value：Jackson JSON 序列化（支持复杂对象）</li>
 *   <li>支持 Java 8 时间类型（LocalDateTime、LocalDate 等）</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(RedisTemplate.class)
@EnableCaching
@ComponentScan("com.openiot.common.redis")
@EnableConfigurationProperties({RedisProperties.class, RedisPoolProperties.class})
public class RedisConfig {

    private final RedisProperties redisProperties;
    private final RedisPoolProperties poolProperties;

    public RedisConfig(RedisProperties redisProperties,
                       ObjectProvider<RedisPoolProperties> poolPropertiesProvider) {
        this.redisProperties = redisProperties;
        this.poolProperties = poolPropertiesProvider.getIfUnique(RedisPoolProperties::new);
    }

    /**
     * 配置 Redis 连接工厂（带连接池）
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis 单机配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setPassword(redisProperties.getPassword());
        config.setDatabase(redisProperties.getDatabase());

        // 连接池配置
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolProperties.getMaxActive());
        poolConfig.setMaxIdle(poolProperties.getMaxIdle());
        poolConfig.setMinIdle(poolProperties.getMinIdle());
        poolConfig.setMaxWait(Duration.ofMillis(poolProperties.getMaxWait()));
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(poolProperties.getTimeBetweenEvictionRuns()));
        poolConfig.setMinEvictableIdleDuration(Duration.ofMillis(poolProperties.getMinEvictableIdleTime()));

        // Socket 配置
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(poolProperties.getConnectTimeout()))
                .keepAlive(true)
                .tcpNoDelay(true)
                .build();

        // Client 配置
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();

        // Lettuce 客户端配置（带连接池）
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(poolProperties.getReadTimeout()))
                .shutdownTimeout(Duration.ofSeconds(5))
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * 配置 RedisTemplate
     * Key 使用 String 序列化
     * Value 使用 JSON 序列化
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 创建 JSON 序列化器
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Key 使用 String 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}

package com.openiot.common.kafka.config;

import com.openiot.common.kafka.producer.TracingProducerInterceptor;
import com.openiot.common.kafka.consumer.TracingConsumerInterceptor;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 链路追踪配置
 *
 * <p>配置 Kafka 消息的 Trace 传播：
 * <ul>
 *   <li>Producer 端注入 Trace 上下文到消息头</li>
 *   <li>Consumer 端从消息头提取 Trace 上下文</li>
 *   <li>支持 W3C Trace Context 标准</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Configuration
@AutoConfiguration
@ConditionalOnClass({Tracer.class})
@ConditionalOnProperty(prefix = "openiot.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTracingConfig {

    @Value("${spring.kafka.producer.interceptor.classes:}")
    private String producerInterceptors;

    @Value("${spring.kafka.consumer.interceptor.classes:}")
    private String consumerInterceptors;

    /**
     * 配置 Producer 拦截器
     *
     * <p>将 Trace 上下文注入到 Kafka 消息头中
     */
    @Bean
    public Map<String, Object> kafkaTracingProducerConfig(ProducerFactory<Object, Object> producerFactory) {
        Map<String, Object> props = new HashMap<>(producerFactory.getConfigurationProperties());

        // 添加 Tracing 拦截器
        String interceptors = (String) props.getOrDefault(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "");
        if (!interceptors.contains(TracingProducerInterceptor.class.getName())) {
            if (!interceptors.isEmpty()) {
                interceptors += ",";
            }
            interceptors += TracingProducerInterceptor.class.getName();
            props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
        }

        return props;
    }

    /**
     * 配置 Consumer 拦截器
     *
     * <p>从 Kafka 消息头中提取 Trace 上下文
     */
    @Bean
    public Map<String, Object> kafkaTracingConsumerConfig(ConsumerFactory<Object, Object> consumerFactory) {
        Map<String, Object> props = new HashMap<>(consumerFactory.getConfigurationProperties());

        // 添加 Tracing 拦截器
        String interceptors = (String) props.getOrDefault(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, "");
        if (!interceptors.contains(TracingConsumerInterceptor.class.getName())) {
            if (!interceptors.isEmpty()) {
                interceptors += ",";
            }
            interceptors += TracingConsumerInterceptor.class.getName();
            props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
        }

        return props;
    }
}

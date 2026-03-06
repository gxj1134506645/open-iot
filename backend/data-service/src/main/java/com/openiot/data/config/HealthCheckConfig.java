package com.openiot.data.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

/**
 * Data 服务健康检查配置
 *
 * <p>提供详细的健康检查指标：
 * <ul>
 *   <li>PostgreSQL 数据库连接状态</li>
 *   <li>MongoDB 连接状态</li>
 *   <li>Kafka 连接状态</li>
 *   <li>JVM 内存状态</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class HealthCheckConfig {

    /**
     * PostgreSQL 数据库健康检查指示器
     */
    @Component
    @RequiredArgsConstructor
    public static class PostgresHealthIndicator implements HealthIndicator {

        private final DataSource dataSource;

        @Override
        public Health health() {
            try (Connection connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(5);
                if (valid) {
                    String catalog = connection.getCatalog();
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("catalog", catalog != null ? catalog : "connected")
                            .withDetail("connection", "valid")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("error", "Database connection is not valid")
                            .build();
                }
            } catch (Exception e) {
                log.error("PostgreSQL health check failed", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * MongoDB 健康检查指示器
     */
    @Component
    @RequiredArgsConstructor
    public static class MongoHealthIndicator implements HealthIndicator {

        private final MongoTemplate mongoTemplate;

        @Override
        public Health health() {
            try {
                // 执行简单的 ping 命令
                var result = mongoTemplate.executeCommand("{ ping: 1 }");
                double ok = result.get("ok", Number.class).doubleValue();

                if (ok == 1.0) {
                    String databaseName = mongoTemplate.getDb().getName();
                    return Health.up()
                            .withDetail("database", "MongoDB")
                            .withDetail("catalog", databaseName)
                            .withDetail("connection", "active")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("error", "MongoDB ping failed")
                            .build();
                }
            } catch (Exception e) {
                log.error("MongoDB health check failed", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * Kafka 健康检查指示器
     */
    @Component
    @RequiredArgsConstructor
    public static class KafkaHealthIndicator implements HealthIndicator {

        private final KafkaAdmin kafkaAdmin;

        @Override
        public Health health() {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                DescribeClusterResult clusterResult = adminClient.describeCluster();

                String clusterId = clusterResult.clusterId().get(5, TimeUnit.SECONDS);
                int nodeCount = clusterResult.nodes().get(5, TimeUnit.SECONDS).size();
                var controller = clusterResult.controller().get(5, TimeUnit.SECONDS);

                return Health.up()
                        .withDetail("clusterId", clusterId)
                        .withDetail("nodeCount", nodeCount)
                        .withDetail("controllerId", controller != null ? controller.id() : "unknown")
                        .withDetail("connection", "active")
                        .build();
            } catch (Exception e) {
                log.error("Kafka health check failed", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * JVM 健康检查指示器
     */
    @Component
    public static class JvmHealthIndicator implements HealthIndicator {

        private static final long MAX_HEAP_USAGE_PERCENT = 90;

        @Override
        public Health health() {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double heapUsagePercent = (double) usedMemory / maxMemory * 100;

            Health.Builder builder;
            if (heapUsagePercent > MAX_HEAP_USAGE_PERCENT) {
                builder = Health.status(new Status("WARNING", "High heap usage"));
            } else {
                builder = Health.up();
            }

            return builder
                    .withDetail("maxMemory", formatBytes(maxMemory))
                    .withDetail("totalMemory", formatBytes(totalMemory))
                    .withDetail("freeMemory", formatBytes(freeMemory))
                    .withDetail("usedMemory", formatBytes(usedMemory))
                    .withDetail("heapUsagePercent", String.format("%.2f%%", heapUsagePercent))
                    .withDetail("availableProcessors", runtime.availableProcessors())
                    .build();
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "B";
            return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
        }
    }
}

package com.openiot.device.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Device 服务健康检查配置
 *
 * <p>提供详细的健康检查指标：
 * <ul>
 *   <li>数据库连接状态</li>
 *   <li>设备统计信息</li>
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
     * 数据库健康检查指示器
     */
    @Component
    @RequiredArgsConstructor
    public static class DatabaseHealthIndicator implements HealthIndicator {

        private final DataSource dataSource;

        @Override
        public Health health() {
            try (Connection connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(5);
                if (valid) {
                    String catalog = connection.getCatalog();
                    return Health.up()
                            .withDetail("database", catalog != null ? catalog : "connected")
                            .withDetail("connection", "valid")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("error", "Database connection is not valid")
                            .build();
                }
            } catch (Exception e) {
                log.error("Database health check failed", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * 设备统计健康检查指示器
     */
    @Component
    @RequiredArgsConstructor
    public static class DeviceStatsHealthIndicator implements HealthIndicator {

        private final DeviceMapper deviceMapper;

        @Override
        public Health health() {
            try {
                // 使用 MyBatis Plus Lambda 查询
                long totalDevices = deviceMapper.selectCount(null);
                // 查询在线设备数 (status = '1' 表示在线)
                long onlineDevices = deviceMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.openiot.device.entity.Device>()
                        .eq(com.openiot.device.entity.Device::getStatus, "1")
                );
                double onlineRate = totalDevices > 0 ? (double) onlineDevices / totalDevices * 100 : 0;

                Health.Builder builder;
                if (onlineRate < 50 && totalDevices > 10) {
                    builder = Health.status(new Status("WARNING", "Low device online rate"));
                } else {
                    builder = Health.up();
                }

                return builder
                        .withDetail("totalDevices", totalDevices)
                        .withDetail("onlineDevices", onlineDevices)
                        .withDetail("offlineDevices", totalDevices - onlineDevices)
                        .withDetail("onlineRate", String.format("%.2f%%", onlineRate))
                        .build();
            } catch (Exception e) {
                log.error("Device stats health check failed", e);
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

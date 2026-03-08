package com.openiot.data.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.openiot.data.config.InfluxDBProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * InfluxDB 服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "influxdb", name = "enabled", havingValue = "true", matchIfMissing = false)
public class InfluxDBService {

    private final InfluxDBProperties properties;
    private InfluxDBClient influxDBClient;
    private WriteApi writeApi;
    private WriteApiBlocking writeApiBlocking;

    /**
     * 初始化 InfluxDB 客户端
     */
    @PostConstruct
    public void init() {
        try {
            influxDBClient = InfluxDBClientFactory.create(
                    properties.getUrl(),
                    properties.getToken().toCharArray(),
                    properties.getOrg(),
                    properties.getBucket()
            );

            // 创建异步写入 API
            writeApi = influxDBClient.getWriteApi();

            // 创建同步写入 API
            writeApiBlocking = influxDBClient.getWriteApiBlocking();

            // 验证连接
            influxDBClient.health();

            log.info("InfluxDB 客户端初始化成功: url={}, org={}, bucket={}",
                    properties.getUrl(), properties.getOrg(), properties.getBucket());

        } catch (Exception e) {
            log.error("InfluxDB 客户端初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，允许系统在 InfluxDB 不可用时继续运行
        }
    }

    /**
     * 销毁 InfluxDB 客户端
     */
    @PreDestroy
    public void destroy() {
        if (writeApi != null) {
            writeApi.close();
        }
        if (influxDBClient != null) {
            influxDBClient.close();
        }
        log.info("InfluxDB 客户端已关闭");
    }

    /**
     * 写入单个数据点（异步）
     *
     * @param measurement 测量名称
     * @param tags        标签
     * @param fields      字段
     * @param timestamp   时间戳
     */
    public void writePoint(String measurement, Map<String, String> tags,
                           Map<String, Object> fields, LocalDateTime timestamp) {
        if (writeApi == null) {
            log.warn("InfluxDB 未初始化，跳过写入");
            return;
        }

        try {
            Point point = Point.measurement(measurement)
                    .addTags(tags)
                    .addFields(fields)
                    .time(timestampToInstant(timestamp), WritePrecision.MS);

            writeApi.writePoint(point);
        } catch (Exception e) {
            log.error("写入 InfluxDB 失败: measurement={}, error={}", measurement, e.getMessage());
        }
    }

    /**
     * 批量写入数据点（异步）
     *
     * @param points 数据点列表
     */
    public void writePoints(List<Point> points) {
        if (writeApi == null) {
            log.warn("InfluxDB 未初始化，跳过写入");
            return;
        }

        try {
            writeApi.writePoints(points);
        } catch (Exception e) {
            log.error("批量写入 InfluxDB 失败: count={}, error={}", points.size(), e.getMessage());
        }
    }

    /**
     * 写入设备属性
     *
     * @param deviceId        设备ID
     * @param deviceCode      设备编码
     * @param productName     产品名称
     * @param propertyIdentifier 属性标识符
     * @param propertyName    属性名称
     * @param value           属性值
     * @param timestamp       时间戳
     */
    public void writeDeviceProperty(Long deviceId, String deviceCode, String productName,
                                    String propertyIdentifier, String propertyName,
                                    Object value, LocalDateTime timestamp) {
        Map<String, String> tags = Map.of(
                "device_id", String.valueOf(deviceId),
                "device_code", deviceCode,
                "product_name", productName,
                "property_identifier", propertyIdentifier,
                "property_name", propertyName
        );

        Map<String, Object> fields = Map.of(
                "value", convertFieldValue(value)
        );

        writePoint("device_property", tags, fields, timestamp);
    }

    /**
     * 写入设备事件
     *
     * @param deviceId     设备ID
     * @param deviceCode   设备编码
     * @param eventIdentifier 事件标识符
     * @param eventName    事件名称
     * @param eventLevel   事件级别
     * @param params       事件参数
     * @param timestamp    时间戳
     */
    public void writeDeviceEvent(Long deviceId, String deviceCode,
                                String eventIdentifier, String eventName, String eventLevel,
                                Map<String, Object> params, LocalDateTime timestamp) {
        Map<String, String> tags = Map.of(
                "device_id", String.valueOf(deviceId),
                "device_code", deviceCode,
                "event_identifier", eventIdentifier,
                "event_name", eventName,
                "event_level", eventLevel
        );

        // 将参数转为字段
        Map<String, Object> fields = Map.of("count", 1);
        if (params != null && !params.isEmpty()) {
            var fieldsBuilder = new java.util.HashMap<>(fields);
            params.forEach((k, v) -> fieldsBuilder.put(k, convertFieldValue(v)));
            fields = fieldsBuilder;
        }

        writePoint("device_event", tags, fields, timestamp);
    }

    /**
     * 写入设备状态变化
     *
     * @param deviceId   设备ID
     * @param deviceCode 设备编码
     * @param online     在线状态
     * @param timestamp  时间戳
     */
    public void writeDeviceStatus(Long deviceId, String deviceCode,
                                  boolean online, LocalDateTime timestamp) {
        Map<String, String> tags = Map.of(
                "device_id", String.valueOf(deviceId),
                "device_code", deviceCode
        );

        Map<String, Object> fields = Map.of(
                "online", online ? 1 : 0
        );

        writePoint("device_status", tags, fields, timestamp);
    }

    /**
     * 检查 InfluxDB 是否可用
     *
     * @return 是否可用
     */
    public boolean isAvailable() {
        if (influxDBClient == null) {
            return false;
        }
        try {
            var health = influxDBClient.health();
            return health.getStatus().equals("pass");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 InfluxDB 客户端（用于高级查询）
     *
     * @return InfluxDB 客户端
     */
    public InfluxDBClient getClient() {
        return influxDBClient;
    }

    /**
     * 获取同步写入 API
     *
     * @return 同步写入 API
     */
    public WriteApiBlocking getWriteApiBlocking() {
        return writeApiBlocking;
    }

    /**
     * 获取组织名称
     */
    public String getOrg() {
        return properties.getOrg();
    }

    /**
     * 获取存储桶名称
     */
    public String getBucket() {
        return properties.getBucket();
    }

    /**
     * 转换字段值（确保类型兼容）
     */
    private Object convertFieldValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return String.valueOf(value);
    }

    /**
     * LocalDateTime 转 Instant
     */
    private Instant timestampToInstant(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}

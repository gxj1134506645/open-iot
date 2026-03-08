package com.openiot.data.influxdb;

import com.influxdb.client.domain.Query;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * InfluxDB 查询服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "influxdb", name = "enabled", havingValue = "true", matchIfMissing = false)
public class InfluxDBQueryService {

    private final InfluxDBService influxDBService;

    /**
     * 查询设备属性历史数据
     *
     * @param deviceId        设备ID
     * @param propertyIdentifier 属性标识符
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @return 历史数据列表
     */
    public List<PropertyDataPoint> queryPropertyHistory(Long deviceId, String propertyIdentifier,
                                                        LocalDateTime startTime, LocalDateTime endTime) {
        if (!influxDBService.isAvailable()) {
            log.warn("InfluxDB 不可用，返回空结果");
            return new ArrayList<>();
        }

        String flux = buildPropertyQuery(deviceId, propertyIdentifier, startTime, endTime);

        try {
            List<PropertyDataPoint> result = new ArrayList<>();
            List<FluxTable> tables = influxDBService.getClient().getQueryApi().query(flux, influxDBService.getOrg());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    PropertyDataPoint point = new PropertyDataPoint();
                    point.setTime(record.getTime() != null ?
                            LocalDateTime.ofInstant(record.getTime(), ZoneId.systemDefault()) : null);
                    point.setValue(record.getValueByKey("_value"));
                    point.setField(record.getValueByKey("_field").toString());
                    point.setDeviceId(record.getValueByKey("device_id") != null ?
                            Long.parseLong(record.getValueByKey("device_id").toString()) : null);
                    point.setDeviceCode((String) record.getValueByKey("device_code"));
                    point.setPropertyIdentifier((String) record.getValueByKey("property_identifier"));
                    point.setPropertyName((String) record.getValueByKey("property_name"));
                    result.add(point);
                }
            }

            log.debug("查询设备属性历史: deviceId={}, property={}, resultSize={}",
                    deviceId, propertyIdentifier, result.size());

            return result;

        } catch (Exception e) {
            log.error("查询设备属性历史失败: deviceId={}, property={}, error={}",
                    deviceId, propertyIdentifier, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询设备属性聚合数据
     *
     * @param deviceId        设备ID
     * @param propertyIdentifier 属性标识符
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @param window          聚合窗口（如 1m, 5m, 1h, 1d）
     * @param aggregateFunction 聚合函数（mean, max, min, sum, count）
     * @return 聚合数据列表
     */
    public List<PropertyDataPoint> queryPropertyAggregated(Long deviceId, String propertyIdentifier,
                                                           LocalDateTime startTime, LocalDateTime endTime,
                                                           String window, String aggregateFunction) {
        if (!influxDBService.isAvailable()) {
            log.warn("InfluxDB 不可用，返回空结果");
            return new ArrayList<>();
        }

        String flux = buildAggregateQuery(deviceId, propertyIdentifier, startTime, endTime, window, aggregateFunction);

        try {
            List<PropertyDataPoint> result = new ArrayList<>();
            List<FluxTable> tables = influxDBService.getClient().getQueryApi().query(flux, influxDBService.getOrg());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    PropertyDataPoint point = new PropertyDataPoint();
                    point.setTime(record.getTime() != null ?
                            LocalDateTime.ofInstant(record.getTime(), ZoneId.systemDefault()) : null);
                    point.setValue(record.getValueByKey("_value"));
                    point.setWindowStart(record.getValueByKey("_start") != null ?
                            LocalDateTime.ofInstant((Instant) record.getValueByKey("_start"), ZoneId.systemDefault()) : null);
                    point.setWindowEnd(record.getValueByKey("_stop") != null ?
                            LocalDateTime.ofInstant((Instant) record.getValueByKey("_stop"), ZoneId.systemDefault()) : null);
                    result.add(point);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("查询设备属性聚合失败: deviceId={}, property={}, error={}",
                    deviceId, propertyIdentifier, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询设备状态历史
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 状态变化记录
     */
    public List<StatusDataPoint> queryStatusHistory(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        if (!influxDBService.isAvailable()) {
            return new ArrayList<>();
        }

        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "device_status")
              |> filter(fn: (r) => r["device_id"] == "%s")
              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
            """,
                influxDBService.getBucket(),
                startTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                endTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                deviceId);

        try {
            List<StatusDataPoint> result = new ArrayList<>();
            List<FluxTable> tables = influxDBService.getClient().getQueryApi().query(flux, influxDBService.getOrg());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    StatusDataPoint point = new StatusDataPoint();
                    point.setTime(LocalDateTime.ofInstant(record.getTime(), ZoneId.systemDefault()));
                    point.setOnline(((Number) record.getValueByKey("online")).intValue() == 1);
                    result.add(point);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("查询设备状态历史失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询设备事件历史
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 事件记录
     */
    public List<EventDataPoint> queryEventHistory(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        if (!influxDBService.isAvailable()) {
            return new ArrayList<>();
        }

        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "device_event")
              |> filter(fn: (r) => r["device_id"] == "%s")
              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
            """,
                influxDBService.getBucket(),
                startTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                endTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                deviceId);

        try {
            List<EventDataPoint> result = new ArrayList<>();
            List<FluxTable> tables = influxDBService.getClient().getQueryApi().query(flux, influxDBService.getOrg());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    EventDataPoint point = new EventDataPoint();
                    point.setTime(LocalDateTime.ofInstant(record.getTime(), ZoneId.systemDefault()));
                    point.setDeviceId(deviceId);
                    point.setEventIdentifier((String) record.getValueByKey("event_identifier"));
                    point.setEventName((String) record.getValueByKey("event_name"));
                    point.setEventLevel((String) record.getValueByKey("event_level"));
                    result.add(point);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("查询设备事件历史失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建属性查询 Flux 语句
     */
    private String buildPropertyQuery(Long deviceId, String propertyIdentifier,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        return String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "device_property")
              |> filter(fn: (r) => r["device_id"] == "%s")
              |> filter(fn: (r) => r["property_identifier"] == "%s")
              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
            """,
                influxDBService.getBucket(),
                startTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                endTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                deviceId,
                propertyIdentifier);
    }

    /**
     * 构建聚合查询 Flux 语句
     */
    private String buildAggregateQuery(Long deviceId, String propertyIdentifier,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       String window, String aggregateFunction) {
        return String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "device_property")
              |> filter(fn: (r) => r["device_id"] == "%s")
              |> filter(fn: (r) => r["property_identifier"] == "%s")
              |> aggregateWindow(every: %s, fn: %s, createEmpty: false)
              |> yield(name: "aggregated")
            """,
                influxDBService.getBucket(),
                startTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                endTime.atZone(ZoneId.systemDefault()).toInstant().toString(),
                deviceId,
                propertyIdentifier,
                window,
                aggregateFunction);
    }

    /**
     * 属性数据点
     */
    @Data
    public static class PropertyDataPoint {
        private LocalDateTime time;
        private Object value;
        private String field;
        private Long deviceId;
        private String deviceCode;
        private String propertyIdentifier;
        private String propertyName;
        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;
    }

    /**
     * 状态数据点
     */
    @Data
    public static class StatusDataPoint {
        private LocalDateTime time;
        private boolean online;
    }

    /**
     * 事件数据点
     */
    @Data
    public static class EventDataPoint {
        private LocalDateTime time;
        private Long deviceId;
        private String eventIdentifier;
        private String eventName;
        private String eventLevel;
    }
}

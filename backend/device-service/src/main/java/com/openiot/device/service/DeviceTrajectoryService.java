package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.DeviceData;
import com.openiot.device.mapper.DeviceDataMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 设备轨迹统计服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTrajectoryService {

    private final DeviceDataMapper deviceDataMapper;
    private final DeviceService deviceService;

    /**
     * 查询设备数据统计
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果
     */
    public DataStatisticsVO getStatistics(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        // 验证设备权限
        Device device = validateDeviceAccess(deviceId);

        // 查询数据
        List<DeviceData> dataList = deviceDataMapper.selectByTimeRange(deviceId, startTime, endTime);

        DataStatisticsVO statistics = new DataStatisticsVO();
        statistics.setDeviceId(deviceId);
        statistics.setDeviceCode(device.getDeviceCode());
        statistics.setStartTime(startTime);
        statistics.setEndTime(endTime);
        statistics.setDataCount(dataList.size());

        // 计算统计信息
        if (!dataList.isEmpty()) {
            // 聚合数据（简化版，实际应该按属性分别统计）
            Map<String, Object> aggregatedData = aggregateData(dataList);
            statistics.setAggregatedData(aggregatedData);

            // 时间范围
            statistics.setFirstDataTime(dataList.get(0).getDataTime());
            statistics.setLastDataTime(dataList.get(dataList.size() - 1).getDataTime());
        }

        return statistics;
    }

    /**
     * 查询设备数据趋势
     *
     * @param deviceId  设备ID
     * @param property  属性标识符
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param interval  时间间隔（分钟）
     * @return 趋势数据
     */
    public List<TrendPointVO> getTrend(Long deviceId, String property,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       int interval) {
        // 验证设备权限
        validateDeviceAccess(deviceId);

        // 查询原始数据
        List<DeviceData> dataList = deviceDataMapper.selectByTimeRange(deviceId, startTime, endTime);

        // 按时间间隔聚合
        Map<LocalDateTime, List<Double>> timeGroups = new HashMap<>();
        for (DeviceData data : dataList) {
            if (data.getData() != null && data.getData().has(property)) {
                double value = data.getData().get(property).asDouble();

                // 计算时间分组
                LocalDateTime groupTime = roundToInterval(data.getDataTime(), interval);
                timeGroups.computeIfAbsent(groupTime, k -> new ArrayList<>()).add(value);
            }
        }

        // 计算每个时间点的平均值
        List<TrendPointVO> trend = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<Double>> entry : timeGroups.entrySet()) {
            TrendPointVO point = new TrendPointVO();
            point.setTime(entry.getKey());
            point.setValue(calculateAverage(entry.getValue()));
            point.setCount(entry.getValue().size());
            trend.add(point);
        }

        // 按时间排序
        trend.sort(Comparator.comparing(TrendPointVO::getTime));

        return trend;
    }

    /**
     * 查询设备数据分布
     *
     * @param deviceId  设备ID
     * @param property  属性标识符
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param buckets   分桶数量
     * @return 分布数据
     */
    public List<DistributionBucketVO> getDistribution(Long deviceId, String property,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      int buckets) {
        // 验证设备权限
        validateDeviceAccess(deviceId);

        // 查询原始数据
        List<DeviceData> dataList = deviceDataMapper.selectByTimeRange(deviceId, startTime, endTime);

        // 提取属性值
        List<Double> values = new ArrayList<>();
        for (DeviceData data : dataList) {
            if (data.getData() != null && data.getData().has(property)) {
                values.add(data.getData().get(property).asDouble());
            }
        }

        if (values.isEmpty()) {
            return new ArrayList<>();
        }

        // 计算分桶范围
        double min = values.stream().min(Double::compare).orElse(0.0);
        double max = values.stream().max(Double::compare).orElse(0.0);
        double bucketSize = (max - min) / buckets;

        // 分桶统计
        List<DistributionBucketVO> distribution = new ArrayList<>();
        for (int i = 0; i < buckets; i++) {
            double bucketMin = min + i * bucketSize;
            double bucketMax = min + (i + 1) * bucketSize;
            if (i == buckets - 1) {
                bucketMax = max; // 最后一个桶包含最大值
            }

            final double finalMin = bucketMin;
            final double finalMax = bucketMax;
            long count = values.stream()
                    .filter(v -> v >= finalMin && v < finalMax)
                    .count();

            DistributionBucketVO bucket = new DistributionBucketVO();
            bucket.setMin(bucketMin);
            bucket.setMax(bucketMax);
            bucket.setCount(count);

            distribution.add(bucket);
        }

        return distribution;
    }

    /**
     * 聚合数据
     */
    private Map<String, Object> aggregateData(List<DeviceData> dataList) {
        Map<String, List<Double>> propertyValues = new HashMap<>();

        // 收集所有属性值
        for (DeviceData data : dataList) {
            if (data.getData() != null) {
                Iterator<String> fieldNames = data.getData().fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (data.getData().get(fieldName).isNumber()) {
                        double value = data.getData().get(fieldName).asDouble();
                        propertyValues.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(value);
                    }
                }
            }
        }

        // 计算平均值
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : propertyValues.entrySet()) {
            double avg = calculateAverage(entry.getValue());
            result.put(entry.getKey() + "_avg", avg);

            // 简化版，也可以添加 min/max
            double min = entry.getValue().stream().min(Double::compare).orElse(0.0);
            double max = entry.getValue().stream().max(Double::compare).orElse(0.0);
            result.put(entry.getKey() + "_min", min);
            result.put(entry.getKey() + "_max", max);
        }

        return result;
    }

    /**
     * 计算平均值
     */
    private double calculateAverage(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 按时间间隔取整
     */
    private LocalDateTime roundToInterval(LocalDateTime time, int intervalMinutes) {
        long minutes = time.getHour() * 60L + time.getMinute();
        long roundedMinutes = (minutes / intervalMinutes) * intervalMinutes;
        return time.withMinute((int) (roundedMinutes % 60))
                     .withSecond(0)
                     .withNano(0);
    }

    /**
     * 验证设备访问权限
     */
    private Device validateDeviceAccess(Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        String tenantId = TenantContext.getTenantId();
        if (!device.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该设备");
        }

        return device;
    }

    /**
     * 数据统计 VO
     */
    @Data
    public static class DataStatisticsVO {
        private Long deviceId;
        private String deviceCode;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer dataCount;
        private LocalDateTime firstDataTime;
        private LocalDateTime lastDataTime;
        private Map<String, Object> aggregatedData;
    }

    /**
     * 趋势点 VO
     */
    @Data
    public static class TrendPointVO {
        private LocalDateTime time;
        private Double value;
        private Integer count;
    }

    /**
     * 分布桶 VO
     */
    @Data
    public static class DistributionBucketVO {
        private Double min;
        private Double max;
        private Long count;
    }
}

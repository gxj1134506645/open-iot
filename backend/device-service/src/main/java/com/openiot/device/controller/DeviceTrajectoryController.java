package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.service.DeviceTrajectoryService;
import com.openiot.device.service.DeviceTrajectoryService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备轨迹控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/{deviceId}/trajectory")
@RequiredArgsConstructor
@Tag(name = "设备轨迹查询", description = "设备历史数据统计和分析接口")
public class DeviceTrajectoryController {

    private final DeviceTrajectoryService trajectoryService;

    /**
     * 查询设备数据统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "数据统计", description = "查询指定时间范围内的数据统计信息")
    public ApiResponse<DataStatisticsVO> getStatistics(
            @Parameter(description = "设备ID") @PathVariable Long deviceId,
            @Parameter(description = "开始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("查询设备数据统计: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);

        DataStatisticsVO statistics = trajectoryService.getStatistics(deviceId, startTime, endTime);

        return ApiResponse.success(statistics);
    }

    /**
     * 查询数据趋势
     */
    @GetMapping("/trend")
    @Operation(summary = "数据趋势", description = "查询属性值随时间的变化趋势")
    public ApiResponse<List<TrendPointVO>> getTrend(
            @Parameter(description = "设备ID") @PathVariable Long deviceId,
            @Parameter(description = "属性标识符") @RequestParam String property,
            @Parameter(description = "开始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "时间间隔（分钟）") @RequestParam(defaultValue = "5") int interval) {

        log.info("查询设备数据趋势: deviceId={}, property={}, interval={}min", deviceId, property, interval);

        List<TrendPointVO> trend = trajectoryService.getTrend(deviceId, property, startTime, endTime, interval);

        return ApiResponse.success(trend);
    }

    /**
     * 查询数据分布
     */
    @GetMapping("/distribution")
    @Operation(summary = "数据分布", description = "查询属性值的分布情况")
    public ApiResponse<List<DistributionBucketVO>> getDistribution(
            @Parameter(description = "设备ID") @PathVariable Long deviceId,
            @Parameter(description = "属性标识符") @RequestParam String property,
            @Parameter(description = "开始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "分桶数量") @RequestParam(defaultValue = "10") int buckets) {

        log.info("查询设备数据分布: deviceId={}, property={}, buckets={}", deviceId, property, buckets);

        List<DistributionBucketVO> distribution = trajectoryService.getDistribution(
                deviceId, property, startTime, endTime, buckets);

        return ApiResponse.success(distribution);
    }
}

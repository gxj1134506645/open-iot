package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.DeviceTrajectory;
import com.openiot.device.mapper.DeviceTrajectoryMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 轨迹查询控制器
 * 提供设备历史轨迹查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/trajectory")
@RequiredArgsConstructor
public class TrajectoryController {

    private final DeviceTrajectoryMapper trajectoryMapper;

    /**
     * 查询设备历史轨迹
     *
     * @param deviceId 设备 ID
     * @param startTime 开始时间（时间戳，毫秒）
     * @param endTime 结束时间（时间戳，毫秒）
     * @return 轨迹点列表
     */
    @GetMapping("/history/{deviceId}")
    public ApiResponse<List<TrajectoryPointVO>> getHistoryTrajectory(
            @PathVariable Long deviceId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        String tenantId = TenantContext.getTenantId();
        log.info("查询历史轨迹: deviceId={}, tenantId={}, startTime={}, endTime={}",
                deviceId, tenantId, startTime, endTime);

        // 默认查询最近 1 小时的轨迹
        if (startTime == null) {
            startTime = System.currentTimeMillis() - 3600 * 1000;
        }
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }

        // TODO: 从 PostgreSQL 查询历史轨迹
        // List<DeviceTrajectory> trajectories = trajectoryMapper.selectByDeviceIdAndTimeRange(
        //     deviceId, tenantId, startTime, endTime);

        return ApiResponse.success(List.of());
    }

    /**
     * 查询设备实时轨迹（最近的轨迹点）
     *
     * @param deviceId 设备 ID
     * @param limit 轨迹点数量限制（默认 100）
     * @return 轨迹点列表
     */
    @GetMapping("/realtime/{deviceId}")
    public ApiResponse<List<TrajectoryPointVO>> getRealtimeTrajectory(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "100") Integer limit) {

        String tenantId = TenantContext.getTenantId();
        log.info("查询实时轨迹: deviceId={}, tenantId={}, limit={}", deviceId, tenantId, limit);

        // TODO: 从 Redis 查询实时轨迹
        // data-service 会将实时轨迹存入 Redis

        return ApiResponse.success(List.of());
    }

    /**
     * 查询多个设备的最新轨迹点
     *
     * @param deviceIds 设备 ID 列表
     * @return 轨迹点列表
     */
    @PostMapping("/latest/batch")
    public ApiResponse<List<TrajectoryPointVO>> batchGetLatestTrajectory(
            @RequestBody List<Long> deviceIds) {

        String tenantId = TenantContext.getTenantId();
        log.info("批量查询最新轨迹: tenantId={}, deviceIds={}", tenantId, deviceIds);

        // TODO: 从 Redis 批量查询

        return ApiResponse.success(List.of());
    }

    /**
     * 轨迹点 VO（前端展示用）
     */
    @Data
    public static class TrajectoryPointVO {
        /**
         * 设备 ID
         */
        private Long deviceId;

        /**
         * 纬度
         */
        private BigDecimal latitude;

        /**
         * 经度
         */
        private BigDecimal longitude;

        /**
         * 海拔（米）
         */
        private BigDecimal altitude;

        /**
         * 速度（km/h）
         */
        private BigDecimal speed;

        /**
         * 方向（度）
         */
        private BigDecimal direction;

        /**
         * 时间戳（毫秒）
         */
        private Long timestamp;

        /**
         * 扩展信息（JSON）
         */
        private String extra;
    }
}

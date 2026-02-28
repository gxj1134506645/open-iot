package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.service.DeviceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备状态查询控制器
 * 提供设备在线状态、设备列表等查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/device/status")
@RequiredArgsConstructor
public class DeviceStatusController {

    private final DeviceService deviceService;

    /**
     * 查询单个设备的在线状态
     *
     * @param deviceId 设备 ID
     * @return 设备状态信息
     */
    @GetMapping("/{deviceId}")
    public ApiResponse<DeviceStatusVO> getDeviceStatus(@PathVariable Long deviceId) {
        log.info("查询设备状态: deviceId={}", deviceId);

        // TODO: 从 Redis 或 data-service 获取实时状态
        // 这里先返回模拟数据
        DeviceStatusVO status = new DeviceStatusVO();
        status.setDeviceId(deviceId);
        status.setOnline(true);
        status.setLastSeen(System.currentTimeMillis());

        return ApiResponse.success(status);
    }

    /**
     * 批量查询设备在线状态
     *
     * @param deviceIds 设备 ID 列表
     * @return 设备状态列表
     */
    @PostMapping("/batch")
    public ApiResponse<List<DeviceStatusVO>> batchGetStatus(@RequestBody List<Long> deviceIds) {
        log.info("批量查询设备状态: deviceIds={}", deviceIds);

        // TODO: 从 Redis 批量获取状态
        return ApiResponse.success(List.of());
    }

    /**
     * 查询租户下所有在线设备
     *
     * @return 在线设备列表
     */
    @GetMapping("/online")
    public ApiResponse<List<DeviceStatusVO>> getOnlineDevices() {
        String tenantId = TenantContext.getTenantId();
        log.info("查询在线设备: tenantId={}", tenantId);

        // TODO: 从 Redis 查询在线设备
        return ApiResponse.success(List.of());
    }

    /**
     * 设备状态 VO
     */
    @Data
    public static class DeviceStatusVO {
        /**
         * 设备 ID
         */
        private Long deviceId;

        /**
         * 设备编码
         */
        private String deviceCode;

        /**
         * 是否在线
         */
        private Boolean online;

        /**
         * 最后在线时间（时间戳）
         */
        private Long lastSeen;
    }
}

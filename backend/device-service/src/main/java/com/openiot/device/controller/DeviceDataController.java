package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.service.DeviceDataReportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 设备数据上报控制器
 * 用于 HTTP 协议设备上报数据
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/data")
@RequiredArgsConstructor
public class DeviceDataController {

    private final DeviceDataReportService reportService;

    /**
     * HTTP 设备数据上报
     */
    @PostMapping
    public ApiResponse<Void> reportData(
            @RequestHeader("X-Device-Token") String deviceToken,
            @RequestBody DeviceDataRequest request) {
        // TODO: 验证 deviceToken 并获取 tenantId 和 deviceId
        String tenantId = "1"; // 临时硬编码
        String deviceId = "1"; // 临时硬编码

        reportService.reportData(
                deviceId,
                tenantId,
                request.getEventType(),
                request.getPayload(),
                request.getRawPayload()
        );

        return ApiResponse.success("上报成功", null);
    }

    /**
     * 设备数据上报请求
     */
    @Data
    public static class DeviceDataRequest {
        private String eventType;
        private Object payload;
        private String rawPayload;
        private String timestamp;
    }
}

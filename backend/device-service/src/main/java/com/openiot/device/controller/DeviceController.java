package com.openiot.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.service.DeviceService;
import com.openiot.device.vo.DeviceCreateVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设备管理控制器
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 创建设备
     */
    @PostMapping
    public ApiResponse<Device> create(@RequestBody DeviceCreateRequest request) {
        // 如果提供了 productId，使用带产品关联的创建方法
        if (request.getProductId() != null) {
            DeviceCreateVO vo = new DeviceCreateVO();
            vo.setDeviceCode(request.getDeviceCode());
            vo.setDeviceName(request.getDeviceName());
            vo.setProtocolType(request.getProtocolType());
            vo.setProductId(request.getProductId());

            Device created = deviceService.createDeviceWithProduct(vo);
            return ApiResponse.success("设备创建成功", created);
        }

        // 否则使用普通创建方法
        Device device = new Device();
        device.setDeviceCode(request.getDeviceCode());
        device.setDeviceName(request.getDeviceName());
        device.setProtocolType(request.getProtocolType());

        Device created = deviceService.createDevice(device);
        return ApiResponse.success("设备创建成功", created);
    }

    /**
     * 查询设备详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Device> getById(@PathVariable Long id) {
        Device device = deviceService.getById(id);
        if (device == null) {
            return ApiResponse.notFound("设备不存在");
        }
        return ApiResponse.success(device);
    }

    /**
     * 分页查询设备列表
     */
    @GetMapping
    public ApiResponse<Page<Device>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String protocolType) {
        Page<Device> result = deviceService.page(page, size, status, protocolType);
        return ApiResponse.success(result);
    }

    /**
     * 更新设备
     */
    @PutMapping("/{id}")
    public ApiResponse<Device> update(@PathVariable Long id, @RequestBody DeviceUpdateRequest request) {
        Device device = deviceService.getById(id);
        if (device == null) {
            return ApiResponse.notFound("设备不存在");
        }
        device.setDeviceName(request.getDeviceName());
        device.setStatus(request.getStatus());
        deviceService.updateById(device);
        return ApiResponse.success("设备更新成功", device);
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.removeById(id);
        return ApiResponse.success("设备删除成功", null);
    }

    /**
     * 重新生成设备Token
     */
    @PostMapping("/{id}/regenerate-token")
    public ApiResponse<TokenResponse> regenerateToken(@PathVariable Long id) {
        String token = deviceService.regenerateToken(id);
        TokenResponse response = new TokenResponse();
        response.setDeviceId(id);
        response.setDeviceToken(token);
        return ApiResponse.success("Token已重新生成", response);
    }

    /**
     * 设备创建请求
     */
    @Data
    public static class DeviceCreateRequest {
        private String deviceCode;
        private String deviceName;
        private String protocolType;
        private Long productId;  // 可选：关联产品ID
    }

    /**
     * 设备更新请求
     */
    @Data
    public static class DeviceUpdateRequest {
        private String deviceName;
        private String status;
    }

    /**
     * Token响应
     */
    @Data
    public static class TokenResponse {
        private Long deviceId;
        private String deviceToken;
    }
}

package com.openiot.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.service.DeviceService;
import com.openiot.device.service.DeviceServiceInvokeService;
import com.openiot.device.vo.DeviceCreateVO;
import com.openiot.device.vo.ServiceInvokeRequestVO;
import com.openiot.device.vo.ServiceInvokeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备 CRUD 和服务调用接口")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceServiceInvokeService deviceServiceInvokeService;

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

    // ==================== 设备服务调用接口 ====================

    /**
     * 调用设备服务（异步）
     *
     * @param id                设备 ID
     * @param serviceIdentifier 服务标识符
     * @param request           调用请求
     * @return 调用记录（包含 invokeId）
     */
    @PostMapping("/{id}/services/{serviceIdentifier}")
    @Operation(summary = "调用设备服务", description = "异步调用设备定义的服务（如开关、重启等），立即返回 invokeId")
    public ApiResponse<ServiceInvokeVO> invokeService(
            @Parameter(description = "设备 ID") @PathVariable Long id,
            @Parameter(description = "服务标识符") @PathVariable String serviceIdentifier,
            @RequestBody ServiceInvokeRequestVO request) {

        log.info("调用设备服务: deviceId={}, service={}", id, serviceIdentifier);

        // 获取输入参数
        Map<String, Object> inputParams = request.getInputParams();
        if (inputParams == null) {
            inputParams = new HashMap<>();
        }

        // 异步调用
        ServiceInvokeVO result = deviceServiceInvokeService.invokeServiceAsync(id, serviceIdentifier, inputParams);

        return ApiResponse.success("服务调用已下发", result);
    }

    /**
     * 同步调用设备服务（阻塞等待响应）
     *
     * @param id                设备 ID
     * @param serviceIdentifier 服务标识符
     * @param request           调用请求
     * @return 调用结果（包含设备响应）
     */
    @PostMapping("/{id}/services/{serviceIdentifier}/sync")
    @Operation(summary = "同步调用设备服务", description = "同步调用设备服务，阻塞等待设备响应（最多 30 秒）")
    public ApiResponse<ServiceInvokeVO> invokeServiceSync(
            @Parameter(description = "设备 ID") @PathVariable Long id,
            @Parameter(description = "服务标识符") @PathVariable String serviceIdentifier,
            @RequestBody ServiceInvokeRequestVO request) {

        log.info("同步调用设备服务: deviceId={}, service={}", id, serviceIdentifier);

        // 获取输入参数
        Map<String, Object> inputParams = request.getInputParams();
        if (inputParams == null) {
            inputParams = new HashMap<>();
        }

        // 获取超时时间
        int timeout = request.getTimeout() != null ? request.getTimeout() : 30;

        // 同步调用
        ServiceInvokeVO result = deviceServiceInvokeService.invokeServiceSync(id, serviceIdentifier, inputParams, timeout);

        return ApiResponse.success("服务调用完成", result);
    }

    /**
     * 查询服务调用状态
     *
     * @param invocationId 调用 ID
     * @return 调用记录
     */
    @GetMapping("/service-invocations/{invocationId}")
    @Operation(summary = "查询服务调用状态", description = "根据 invokeId 查询服务调用的执行状态和结果")
    public ApiResponse<ServiceInvokeVO> getInvocationStatus(
            @Parameter(description = "调用 ID") @PathVariable String invocationId) {

        log.info("查询服务调用状态: invocationId={}", invocationId);

        ServiceInvokeVO result = deviceServiceInvokeService.queryInvocationStatus(invocationId);

        return ApiResponse.success(result);
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

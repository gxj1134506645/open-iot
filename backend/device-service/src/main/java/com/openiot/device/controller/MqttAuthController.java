package com.openiot.device.controller;

import com.openiot.device.entity.Device;
import com.openiot.device.service.DeviceStatusService;
import com.openiot.device.service.DeviceTokenService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 设备认证控制器
 * EMQX Webhook 认证接口
 *
 * EMQX Rule Engine 配置示例：
 * 当设备连接时，EMQX 会调用此接口进行认证
 * 请求格式：POST /api/device/mqtt/auth
 * 请求体：{"username": "deviceCode", "password": "deviceToken", "clientid": "clientId"}
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/device/mqtt")
@RequiredArgsConstructor
public class MqttAuthController {

    private final DeviceTokenService deviceTokenService;
    private final DeviceStatusService deviceStatusService;

    /**
     * MQTT 设备认证接口
     * EMQX Webhook 认证回调
     *
     * @param request 认证请求
     * @return 认证结果
     */
    @PostMapping("/auth")
    public Map<String, Object> authenticate(@RequestBody MqttAuthRequest request) {
        log.info("MQTT 设备认证请求: username={}, clientId={}", request.getUsername(), request.getClientid());

        Map<String, Object> result = new HashMap<>();

        try {
            // EMQX 认证约定：username = deviceCode, password = deviceToken
            String deviceCode = request.getUsername();
            String deviceToken = request.getPassword();

            // 验证设备 Token
            Device device = deviceTokenService.validateToken(deviceCode, deviceToken);

            if (device != null) {
                // 认证成功
                result.put("result", "allow");
                result.put("tenant_id", device.getTenantId());
                result.put("device_id", device.getId());

                log.info("MQTT 设备认证成功: deviceCode={}, tenantId={}", deviceCode, device.getTenantId());
            } else {
                // 认证失败
                result.put("result", "deny");
                result.put("message", "设备认证失败：Token 无效或设备已禁用");

                log.warn("MQTT 设备认证失败: deviceCode={}", deviceCode);
            }
        } catch (Exception e) {
            log.error("MQTT 设备认证异常: username={}", request.getUsername(), e);
            result.put("result", "deny");
            result.put("message", "认证服务异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * MQTT 设备连接成功回调
     * 更新设备上线状态
     *
     * @param request 连接信息
     * @return 处理结果
     */
    @PostMapping("/connected")
    public Map<String, Object> onConnected(@RequestBody MqttConnectedRequest request) {
        log.info("MQTT 设备已连接: clientId={}, username={}", request.getClientid(), request.getUsername());

        // 更新设备在线状态
        deviceStatusService.setOnline(request.getUsername(), request.getClientid());

        Map<String, Object> result = new HashMap<>();
        result.put("result", "ok");
        return result;
    }

    /**
     * MQTT 设备断开连接回调
     * 更新设备离线状态
     *
     * @param request 断开信息
     * @return 处理结果
     */
    @PostMapping("/disconnected")
    public Map<String, Object> onDisconnected(@RequestBody MqttDisconnectedRequest request) {
        log.info("MQTT 设备已断开: clientId={}, username={}", request.getClientid(), request.getUsername());

        // 更新设备离线状态
        deviceStatusService.setOffline(request.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("result", "ok");
        return result;
    }

    /**
     * MQTT 认证请求
     */
    @Data
    public static class MqttAuthRequest {
        /**
         * MQTT 用户名（对应设备编码）
         */
        private String username;

        /**
         * MQTT 密码（对应设备 Token）
         */
        private String password;

        /**
         * MQTT 客户端 ID
         */
        private String clientid;
    }

    /**
     * MQTT 连接成功请求
     */
    @Data
    public static class MqttConnectedRequest {
        private String clientid;
        private String username;
        private String ipaddress;
        private Long keepalive;
    }

    /**
     * MQTT 断开连接请求
     */
    @Data
    public static class MqttDisconnectedRequest {
        private String clientid;
        private String username;
        private String reason;
    }
}

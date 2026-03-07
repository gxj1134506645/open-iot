package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.sse.SseEmitterManager;
import com.openiot.device.sse.SseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * SSE 实时推送控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "实时推送", description = "SSE 长连接实时推送接口")
public class SseController {

    private final SseEmitterManager emitterManager;

    /**
     * 建立 SSE 连接
     *
     * @param clientId 客户端ID（可选，不传则自动生成）
     * @param timeout  超时时间（毫秒，可选，默认30分钟）
     * @return SseEmitter
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立SSE连接", description = "建立Server-Sent Events长连接，接收实时推送消息")
    public SseEmitter connect(
            @Parameter(description = "客户端ID") @RequestParam(required = false) String clientId,
            @Parameter(description = "超时时间（毫秒）") @RequestParam(required = false) Long timeout) {

        // 如果没有提供 clientId，则自动生成
        if (clientId == null || clientId.isEmpty()) {
            clientId = UUID.randomUUID().toString();
        }

        log.info("建立 SSE 连接: clientId={}, timeout={}ms", clientId, timeout);

        return emitterManager.createEmitter(clientId, timeout);
    }

    /**
     * 断开 SSE 连接
     *
     * @param clientId 客户端ID
     * @return 操作结果
     */
    @DeleteMapping("/disconnect")
    @Operation(summary = "断开SSE连接", description = "主动断开SSE连接")
    public ApiResponse<Void> disconnect(
            @Parameter(description = "客户端ID") @RequestParam String clientId) {

        log.info("断开 SSE 连接: clientId={}", clientId);
        emitterManager.removeEmitter(clientId);

        return ApiResponse.success("连接已断开", null);
    }

    /**
     * 查询在线连接数
     *
     * @return 在线连接数
     */
    @GetMapping("/online-count")
    @Operation(summary = "查询在线连接数", description = "查询当前SSE在线连接数量")
    public ApiResponse<Map<String, Integer>> getOnlineCount() {
        int count = emitterManager.getOnlineCount();
        return ApiResponse.success(Map.of("onlineCount", count));
    }

    /**
     * 检查客户端是否在线
     *
     * @param clientId 客户端ID
     * @return 是否在线
     */
    @GetMapping("/check-online")
    @Operation(summary = "检查客户端在线状态", description = "检查指定客户端是否在线")
    public ApiResponse<Map<String, Boolean>> checkOnline(
            @Parameter(description = "客户端ID") @RequestParam String clientId) {

        boolean online = emitterManager.isOnline(clientId);
        return ApiResponse.success(Map.of("online", online));
    }

    /**
     * 测试消息推送
     *
     * @param message 消息内容
     * @return 推送结果
     */
    @PostMapping("/test-broadcast")
    @Operation(summary = "测试广播消息", description = "向所有在线客户端广播测试消息")
    public ApiResponse<Map<String, Object>> testBroadcast(@RequestBody Map<String, Object> message) {

        log.info("测试广播消息: {}", message);

        int count = emitterManager.broadcast(SseMessage.builder()
                .type("test")
                .data(message)
                .build());

        return ApiResponse.success(Map.of(
                "message", "广播成功",
                "onlineCount", emitterManager.getOnlineCount(),
                "sentCount", count
        ));
    }

    /**
     * 推送测试消息给指定客户端
     *
     * @param clientId 客户端ID
     * @param message  消息内容
     * @return 推送结果
     */
    @PostMapping("/test-send")
    @Operation(summary = "测试单发消息", description = "向指定客户端发送测试消息")
    public ApiResponse<Map<String, Object>> testSend(
            @Parameter(description = "客户端ID") @RequestParam String clientId,
            @RequestBody Map<String, Object> message) {

        log.info("测试单发消息: clientId={}, message={}", clientId, message);

        boolean success = emitterManager.sendMessage(clientId, SseMessage.builder()
                .type("test")
                .data(message)
                .build());

        return ApiResponse.success(Map.of(
                "success", success,
                "online", emitterManager.isOnline(clientId)
        ));
    }

    /**
     * 订阅设备事件
     *
     * @param clientId 客户端ID
     * @param deviceId 设备ID
     * @return 订阅结果
     */
    @PostMapping("/subscribe")
    @Operation(summary = "订阅设备事件", description = "订阅指定设备的事件推送")
    public ApiResponse<Map<String, Object>> subscribe(
            @Parameter(description = "客户端ID") @RequestParam String clientId,
            @Parameter(description = "设备ID") @RequestParam Long deviceId) {

        // TODO: 实现设备订阅逻辑
        log.info("订阅设备事件: clientId={}, deviceId={}", clientId, deviceId);

        return ApiResponse.success(Map.of(
                "message", "订阅成功",
                "clientId", clientId,
                "deviceId", deviceId
        ));
    }

    /**
     * 取消订阅设备事件
     *
     * @param clientId 客户端ID
     * @param deviceId 设备ID
     * @return 取消订阅结果
     */
    @DeleteMapping("/unsubscribe")
    @Operation(summary = "取消订阅设备事件", description = "取消订阅指定设备的事件推送")
    public ApiResponse<Map<String, Object>> unsubscribe(
            @Parameter(description = "客户端ID") @RequestParam String clientId,
            @Parameter(description = "设备ID") @RequestParam Long deviceId) {

        // TODO: 实现取消订阅逻辑
        log.info("取消订阅设备事件: clientId={}, deviceId={}", clientId, deviceId);

        return ApiResponse.success(Map.of(
                "message", "取消订阅成功",
                "clientId", clientId,
                "deviceId", deviceId
        ));
    }
}

package com.openiot.device.sse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * SSE 连接管理器单元测试
 *
 * @author OpenIoT Team
 */
@DisplayName("SSE 连接管理器测试")
class SseEmitterManagerTest {

    private SseEmitterManager manager;

    @BeforeEach
    void setUp() {
        manager = new SseEmitterManager();
    }

    @AfterEach
    void tearDown() {
        manager.closeAll();
    }

    @Test
    @DisplayName("创建 SSE 连接 - 成功")
    void createEmitter_Success() {
        // When
        SseEmitter emitter = manager.createEmitter("client-001", 60000L);

        // Then
        assertThat(emitter).isNotNull();
        assertThat(manager.getOnlineCount()).isEqualTo(1);
        assertThat(manager.isOnline("client-001")).isTrue();
    }

    @Test
    @DisplayName("创建 SSE 连接 - 自动生成 clientId")
    void createEmitter_AutoGenerateClientId() {
        // When
        SseEmitter emitter1 = manager.createEmitter(null, 60000L);
        SseEmitter emitter2 = manager.createEmitter("", 60000L);

        // Then
        assertThat(emitter1).isNotNull();
        assertThat(emitter2).isNotNull();
        assertThat(manager.getOnlineCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("发送消息 - 成功")
    void sendMessage_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);
        SseMessage message = SseMessage.builder()
                .type("test")
                .data(Map.of("content", "hello"))
                .build();

        // When
        boolean success = manager.sendMessage("client-001", message);

        // Then
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("发送消息 - 客户端不存在")
    void sendMessage_ClientNotFound() {
        // Given
        SseMessage message = SseMessage.builder()
                .type("test")
                .data(Map.of("content", "hello"))
                .build();

        // When
        boolean success = manager.sendMessage("not-exist", message);

        // Then
        assertThat(success).isFalse();
    }

    @Test
    @DisplayName("广播消息 - 成功")
    void broadcast_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);
        manager.createEmitter("client-002", 60000L);
        manager.createEmitter("client-003", 60000L);

        SseMessage message = SseMessage.builder()
                .type("broadcast")
                .data(Map.of("content", "broadcast message"))
                .build();

        // When
        int count = manager.broadcast(message);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("移除连接 - 成功")
    void removeEmitter_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);
        assertThat(manager.isOnline("client-001")).isTrue();

        // When
        manager.removeEmitter("client-001");

        // Then
        assertThat(manager.isOnline("client-001")).isFalse();
        assertThat(manager.getOnlineCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("发送设备状态变化消息")
    void sendDeviceStatusChange_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);

        // When
        manager.sendDeviceStatusChange(1L, "online");

        // Then
        assertThat(manager.getOnlineCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("发送设备属性变化消息")
    void sendDevicePropertyChange_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);

        // When
        manager.sendDevicePropertyChange(1L, "temperature", 25.5);

        // Then
        assertThat(manager.getOnlineCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("发送设备事件消息")
    void sendDeviceEvent_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);

        // When
        manager.sendDeviceEvent(1L, "high_temp", "alert", Map.of("temperature", 80));

        // Then
        assertThat(manager.getOnlineCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("发送告警消息")
    void sendAlert_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);

        // When
        manager.sendAlert(1L, 1L, "critical", "高温告警");

        // Then
        assertThat(manager.getOnlineCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("查询在线连接数")
    void getOnlineCount_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);
        manager.createEmitter("client-002", 60000L);

        // When
        int count = manager.getOnlineCount();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("检查客户端在线状态")
    void isOnline_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);

        // When & Then
        assertThat(manager.isOnline("client-001")).isTrue();
        assertThat(manager.isOnline("not-exist")).isFalse();
    }

    @Test
    @DisplayName("关闭所有连接")
    void closeAll_Success() {
        // Given
        manager.createEmitter("client-001", 60000L);
        manager.createEmitter("client-002", 60000L);
        assertThat(manager.getOnlineCount()).isEqualTo(2);

        // When
        manager.closeAll();

        // Then
        assertThat(manager.getOnlineCount()).isEqualTo(0);
    }
}

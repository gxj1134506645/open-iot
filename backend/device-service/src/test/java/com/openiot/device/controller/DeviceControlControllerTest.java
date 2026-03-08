package com.openiot.device.controller;

import com.openiot.device.entity.CommandSendRecord;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.PropertySetRecord;
import com.openiot.device.entity.ServiceCallRecord;
import com.openiot.device.mapper.CommandSendRecordMapper;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.mapper.PropertySetRecordMapper;
import com.openiot.device.mapper.ServiceCallRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 设备控制 API 集成测试
 *
 * @author OpenIoT Team
 */
@DisplayName("设备控制 API 集成测试")
public class DeviceControlControllerTest extends BaseControllerTest {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private ServiceCallRecordMapper serviceCallRecordMapper;

    @Autowired
    private PropertySetRecordMapper propertySetRecordMapper;

    @Autowired
    private CommandSendRecordMapper commandSendRecordMapper;

    private Device testDevice;

    /**
     * 创建测试设备
     */
    private void createTestDevice() {
        testDevice = new Device();
        testDevice.setTenantId(1L);
        testDevice.setProductId(1L);
        testDevice.setDeviceCode("TEST_DEVICE_001");
        testDevice.setDeviceName("测试设备");
        testDevice.setDeviceSecret("test-secret");
        testDevice.setOnline(true);
        testDevice.setStatus("1");
        deviceMapper.insert(testDevice);
    }

    /**
     * 清理测试数据
     */
    @AfterEach
    void cleanUp() {
        if (testDevice != null && testDevice.getId() != null) {
            deviceMapper.deleteById(testDevice.getId());
        }
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("调用设备服务 - 成功")
    void callService_Success() throws Exception {
        // Given
        createTestDevice();

        String serviceCallJson = """
            {
                "inputParams": {
                    "delay": 5000
                }
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/devices/" + testDevice.getId() + "/services/restart/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceCallJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.serviceIdentifier").value("restart"))
                .andExpect(jsonPath("$.data.status").value("sent"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("设置设备属性 - 成功")
    void setProperty_Success() throws Exception {
        // Given
        createTestDevice();

        String propertySetJson = """
            {
                "value": 25.5
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/v1/devices/" + testDevice.getId() + "/properties/temperature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertySetJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.propertyIdentifier").value("temperature"))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("发送设备命令 - 成功")
    void sendCommand_Success() throws Exception {
        // Given
        createTestDevice();

        String commandJson = """
            {
                "params": {
                    "mode": "factory"
                }
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/devices/" + testDevice.getId() + "/commands/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commandIdentifier").value("reset"))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询服务调用记录 - 成功")
    void getServiceCallRecords_Success() throws Exception {
        // Given
        createTestDevice();

        // 创建一条调用记录
        ServiceCallRecord record = new ServiceCallRecord();
        record.setTenantId(1L);
        record.setDeviceId(testDevice.getId());
        record.setProductId(1L);
        record.setServiceIdentifier("restart");
        record.setServiceName("重启服务");
        record.setStatus("sent");
        record.setCallType("async");
        serviceCallRecordMapper.insert(record);

        // When & Then
        mockMvc.perform(get("/api/v1/devices/" + testDevice.getId() + "/services/calls")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThan(0))));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询属性设置记录 - 成功")
    void getPropertySetRecords_Success() throws Exception {
        // Given
        createTestDevice();

        // 创建一条设置记录
        PropertySetRecord record = new PropertySetRecord();
        record.setTenantId(1L);
        record.setDeviceId(testDevice.getId());
        record.setProductId(1L);
        record.setPropertyIdentifier("temperature");
        record.setPropertyName("温度");
        record.setSetType("user");
        record.setStatus("pending");
        propertySetRecordMapper.insert(record);

        // When & Then
        mockMvc.perform(get("/api/v1/devices/" + testDevice.getId() + "/properties/sets")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询命令下发记录 - 成功")
    void getCommandSendRecords_Success() throws Exception {
        // Given
        createTestDevice();

        // 创建一条下发记录
        CommandSendRecord record = new CommandSendRecord();
        record.setTenantId(1L);
        record.setDeviceId(testDevice.getId());
        record.setProductId(1L);
        record.setCommandIdentifier("reset");
        record.setCommandName("重置");
        record.setStatus("pending");
        commandSendRecordMapper.insert(record);

        // When & Then
        mockMvc.perform(get("/api/v1/devices/" + testDevice.getId() + "/commands/sends")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("未认证访问设备控制 - 失败")
    void callDeviceControl_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/devices/1/commands/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}

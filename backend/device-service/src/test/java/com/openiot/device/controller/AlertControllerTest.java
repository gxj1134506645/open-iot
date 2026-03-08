package com.openiot.device.controller;

import com.openiot.device.entity.Alert;
import com.openiot.device.entity.AlertRecord;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.AlertMapper;
import com.openiot.device.mapper.AlertRecordMapper;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.mapper.ProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 告警管理 API 集成测试
 *
 * @author OpenIoT Team
 */
@DisplayName("告警管理 API 集成测试")
public class AlertControllerTest extends BaseControllerTest {

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private ProductMapper productMapper;

    private Product testProduct;
    private Device testDevice;
    private Alert testAlert;
    private AlertRecord testAlertRecord;

    /**
     * 创建测试数据
     */
    @BeforeEach
    void setUp() {
        // 创建测试产品
        testProduct = new Product();
        testProduct.setTenantId(1L);
        testProduct.setProductName("温湿度传感器");
        testProduct.setProductCode("TH001");
        testProduct.setProtocolType("MQTT");
        testProduct.setNodeType("device");
        testProduct.setProductKey("test-product-key");
        testProduct.setProductSecret("test-product-secret");
        testProduct.setStatus("1");
        productMapper.insert(testProduct);

        // 创建测试设备
        testDevice = new Device();
        testDevice.setTenantId(1L);
        testDevice.setProductId(testProduct.getId());
        testDevice.setDeviceCode("TEST_DEVICE_001");
        testDevice.setDeviceName("测试设备");
        testDevice.setDeviceSecret("test-secret");
        testDevice.setOnline(true);
        testDevice.setStatus("1");
        deviceMapper.insert(testDevice);

        // 创建测试告警规则
        testAlert = new Alert();
        testAlert.setTenantId(1L);
        testAlert.setProductId(testProduct.getId());
        testAlert.setDeviceId(testDevice.getId());
        testAlert.setAlertName("温度过高告警");
        testAlert.setAlertType("EXPRESSION");
        testAlert.setExpression("temperature > 50");
        testAlert.setAlertLevel("warning");
        testAlert.setAlertContent("设备温度超过阈值");
        testAlert.setStatus("1");
        alertMapper.insert(testAlert);
    }

    /**
     * 清理测试数据
     */
    @AfterEach
    void cleanUp() {
        if (testAlertRecord != null && testAlertRecord.getId() != null) {
            alertRecordMapper.deleteById(testAlertRecord.getId());
        }
        if (testAlert != null && testAlert.getId() != null) {
            alertMapper.deleteById(testAlert.getId());
        }
        if (testDevice != null && testDevice.getId() != null) {
            deviceMapper.deleteById(testDevice.getId());
        }
        if (testProduct != null && testProduct.getId() != null) {
            productMapper.deleteById(testProduct.getId());
        }
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询告警记录列表 - 成功")
    void getAlertRecords_Success() throws Exception {
        // Given
        createTestAlertRecord();

        // When & Then
        mockMvc.perform(get("/api/v1/alerts")
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
    @DisplayName("按告警级别筛选 - 成功")
    void getAlertRecordsByLevel_Success() throws Exception {
        // Given
        createTestAlertRecord();

        // When & Then
        mockMvc.perform(get("/api/v1/alerts")
                        .param("page", "1")
                        .param("size", "10")
                        .param("alertLevel", "warning"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("按状态筛选告警 - 成功")
    void getAlertRecordsByStatus_Success() throws Exception {
        // Given
        createTestAlertRecord();

        // When & Then
        mockMvc.perform(get("/api/v1/alerts")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "pending"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("处理告警 - 成功")
    void handleAlert_Success() throws Exception {
        // Given
        createTestAlertRecord();

        String handleJson = """
            {
                "status": "resolved",
                "remark": "温度已恢复正常"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/v1/alerts/" + testAlertRecord.getId() + "/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handleJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("resolved"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("批量处理告警 - 成功")
    void batchHandleAlerts_Success() throws Exception {
        // Given
        createTestAlertRecord();
        createTestAlertRecord(); // 创建第二条

        String batchHandleJson = """
            {
                "alertIds": [%s],
                "status": "resolved",
                "remark": "批量处理"
            }
            """.formatted(testAlertRecord.getId());

        // When & Then
        mockMvc.perform(put("/api/v1/alerts/batch-handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchHandleJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询告警统计 - 成功")
    void getAlertStatistics_Success() throws Exception {
        // Given
        createTestAlertRecord();

        // When & Then
        mockMvc.perform(get("/api/v1/alerts/statistics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").isNumber())
                .andExpect(jsonPath("$.data.pendingCount").isNumber())
                .andExpect(jsonPath("$.data.resolvedCount").isNumber())
                .andExpect(jsonPath("$.data.criticalCount").isNumber());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询告警详情 - 成功")
    void getAlertDetail_Success() throws Exception {
        // Given
        createTestAlertRecord();

        // When & Then
        mockMvc.perform(get("/api/v1/alerts/" + testAlertRecord.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.alertTitle").isNotEmpty())
                .andExpect(jsonPath("$.data.alertLevel").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("忽略告警 - 成功")
    void ignoreAlert_Success() throws Exception {
        // Given
        createTestAlertRecord();

        String ignoreJson = """
            {
                "status": "ignored",
                "remark": "误报"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/v1/alerts/" + testAlertRecord.getId() + "/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ignoreJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ignored"));
    }

    @Test
    @DisplayName("未认证访问告警 - 失败")
    void getAlerts_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/alerts"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    /**
     * 创建测试告警记录
     */
    private void createTestAlertRecord() {
        AlertRecord record = new AlertRecord();
        record.setTenantId(1L);
        record.setProductId(testProduct.getId());
        record.setDeviceId(testDevice.getId());
        record.setAlertId(testAlert.getId());
        record.setAlertTitle("温度过高告警");
        record.setAlertLevel("warning");
        record.setAlertContent("设备温度达到 60℃，超过阈值 50℃");
        record.setStatus("pending");
        record.setAlertTime(java.time.LocalDateTime.now());
        alertRecordMapper.insert(record);
        testAlertRecord = record;
    }
}

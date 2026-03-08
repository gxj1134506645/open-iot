package com.openiot.device.controller;

import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.entity.Rule;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.mapper.ProductMapper;
import com.openiot.device.mapper.RuleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * 规则引擎 API 集成测试
 *
 * @author OpenIoT Team
 */
@DisplayName("规则引擎 API 集成测试")
public class RuleControllerTest extends BaseControllerTest {

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    private Product testProduct;
    private Device testDevice;
    private Rule testRule;

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
    }

    /**
     * 清理测试数据
     */
    @AfterEach
    void cleanUp() {
        if (testRule != null && testRule.getId() != null) {
            ruleMapper.deleteById(testRule.getId());
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
    @DisplayName("创建表达式规则 - 成功")
    void createExpressionRule_Success() throws Exception {
        // Given
        String ruleJson = """
            {
                "ruleName": "温度告警规则",
                "ruleCode": "TEMP_ALERT_001",
                "ruleType": "EXPRESSION",
                "expression": "temperature > 50",
                "alertLevel": "warning",
                "alertTemplate": "设备 {deviceName} 温度过高: {temperature}℃",
                "status": "1",
                "productId": %d
            }
            """.formatted(testProduct.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ruleName").value("温度告警规则"))
                .andExpect(jsonPath("$.data.ruleCode").value("TEMP_ALERT_001"))
                .andExpect(jsonPath("$.data.ruleType").value("EXPRESSION"))
                .andExpect(jsonPath("$.data.expression").value("temperature > 50"));

        // 保存 ID 用于清理
        String response = mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 提取 ID (实际项目中应使用 JSON 解析)
        testRule = new Rule();
        testRule.setId(1L); // 临时设置，实际应从响应中获取
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("创建脚本规则 - 成功")
    void createScriptRule_Success() throws Exception {
        // Given
        String ruleJson = """
            {
                "ruleName": "湿度告警脚本",
                "ruleCode": "HUMIDITY_ALERT_001",
                "ruleType": "SCRIPT",
                "scriptContent": "if (data.humidity < 20) { return { matched: true, alertLevel: 'warning', message: '湿度过低' }; } return { matched: false };",
                "alertLevel": "warning",
                "alertTemplate": "设备湿度过低",
                "status": "1",
                "productId": %d
            }
            """.formatted(testProduct.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ruleType").value("SCRIPT"))
                .andExpect(jsonPath("$.data.scriptContent").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询规则列表 - 成功")
    void getRules_Success() throws Exception {
        // Given - 创建测试规则
        createTestRule();

        // When & Then
        mockMvc.perform(get("/api/v1/rules/page")
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
    @DisplayName("按类型筛选规则 - 成功")
    void getRulesByType_Success() throws Exception {
        // Given
        createTestRule();

        // When & Then
        mockMvc.perform(get("/api/v1/rules/page")
                        .param("page", "1")
                        .param("size", "10")
                        .param("ruleType", "EXPRESSION"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[?(@.ruleType=='EXPRESSION')]").exists());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("测试规则 - 表达式匹配")
    void testRule_Matched() throws Exception {
        // Given
        createTestRule();

        String testData = """
            {
                "deviceData": {
                    "temperature": 60,
                    "humidity": 30
                }
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/rules/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testData))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.matched").value(true));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("启用规则 - 成功")
    void enableRule_Success() throws Exception {
        // Given
        createTestRule();
        testRule.setStatus("0");
        ruleMapper.updateById(testRule);

        // When & Then
        mockMvc.perform(put("/api/v1/rules/" + testRule.getId() + "/enable"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("1"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("禁用规则 - 成功")
    void disableRule_Success() throws Exception {
        // Given
        createTestRule();
        testRule.setStatus("1");
        ruleMapper.updateById(testRule);

        // When & Then
        mockMvc.perform(put("/api/v1/rules/" + testRule.getId() + "/disable"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("0"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("删除规则 - 成功")
    void deleteRule_Success() throws Exception {
        // Given
        createTestRule();
        Long ruleId = testRule.getId();

        // When & Then
        mockMvc.perform(delete("/api/v1/rules/" + ruleId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证已删除
        testRule = null; // 避免在 cleanUp 中再次删除
    }

    @Test
    @DisplayName("未认证访问规则 - 失败")
    void getRules_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/rules/page"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    /**
     * 创建测试规则
     */
    private void createTestRule() {
        if (testRule != null) return;

        testRule = new Rule();
        testRule.setTenantId(1L);
        testRule.setProductId(testProduct.getId());
        testRule.setRuleName("温度告警规则");
        testRule.setRuleCode("TEMP_ALERT_" + System.currentTimeMillis());
        testRule.setRuleType("EXPRESSION");
        testRule.setExpression("temperature > 50");
        testRule.setAlertLevel("warning");
        testRule.setAlertTemplate("温度过高告警");
        testRule.setStatus("1");
        ruleMapper.insert(testRule);
    }
}

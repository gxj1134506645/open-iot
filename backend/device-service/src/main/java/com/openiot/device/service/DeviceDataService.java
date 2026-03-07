package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.DeviceData;
import com.openiot.device.entity.ProductProperty;
import com.openiot.device.mapper.DeviceDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备数据服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService extends ServiceImpl<DeviceDataMapper, DeviceData> {

    private final DeviceService deviceService;
    private final ThingModelService thingModelService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 接收并保存设备上报数据
     *
     * @param deviceId 设备ID
     * @param data     上报数据（属性标识符 -> 值）
     * @param dataTime 数据时间戳（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportData(Long deviceId, Map<String, Object> data, LocalDateTime dataTime) {
        // 1. 查询设备信息
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        // 2. 校验租户权限
        String tenantId = TenantContext.getTenantId();
        if (!device.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该设备");
        }

        // 3. 根据物模型校验数据
        validateDataWithThingModel(device.getProductId(), data);

        // 4. 构造设备数据实体
        DeviceData deviceData = new DeviceData();
        deviceData.setTenantId(device.getTenantId());
        deviceData.setProductId(device.getProductId());
        deviceData.setDeviceId(deviceId);

        // 转换为 JsonNode
        ObjectNode dataNode = objectMapper.valueToTree(data);
        deviceData.setData(dataNode);

        // 设置数据时间
        if (dataTime == null) {
            dataTime = LocalDateTime.now();
        }
        deviceData.setDataTime(dataTime);

        // 5. 保存到数据库
        this.save(deviceData);

        log.info("设备数据上报成功: device={}, dataCount={}", device.getDeviceCode(), data.size());

        // 6. 发送到 Kafka（异步处理）
        sendToKafka(device, data, dataTime);
    }

    /**
     * 查询设备在指定时间范围内的数据
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 数据列表
     */
    public List<DeviceData> queryByTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        // 验证设备存在性和权限
        validateDeviceAccess(deviceId);

        return baseMapper.selectByTimeRange(deviceId, startTime, endTime);
    }

    /**
     * 查询设备最新的 N 条数据
     *
     * @param deviceId 设备ID
     * @param limit    条数
     * @return 数据列表
     */
    public List<DeviceData> queryLatest(Long deviceId, int limit) {
        // 验证设备存在性和权限
        validateDeviceAccess(deviceId);

        return baseMapper.selectLatest(deviceId, limit);
    }

    /**
     * 查询产品下所有设备的最新数据
     *
     * @param productId 产品ID
     * @param limit     条数
     * @return 数据列表
     */
    public List<DeviceData> queryLatestByProduct(Long productId, int limit) {
        return baseMapper.selectLatestByProduct(productId, limit);
    }

    /**
     * 根据物模型校验数据
     *
     * @param productId 产品ID
     * @param data      上报数据
     */
    private void validateDataWithThingModel(Long productId, Map<String, Object> data) {
        // 查询产品属性定义
        List<ProductProperty> properties = thingModelService.getProperties(productId);

        // 校验每个属性
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String propertyIdentifier = entry.getKey();
            Object value = entry.getValue();

            // 查找属性定义
            ProductProperty property = properties.stream()
                    .filter(p -> p.getPropertyIdentifier().equals(propertyIdentifier))
                    .findFirst()
                    .orElse(null);

            if (property == null) {
                log.warn("未定义的属性: {}, 产品: {}", propertyIdentifier, productId);
                continue; // 允许未定义的属性（兼容性）
            }

            // 校验数据类型
            validateDataType(property, value);

            // 校验取值范围（如果有定义）
            validateDataRange(property, value);
        }
    }

    /**
     * 校验数据类型
     */
    private void validateDataType(ProductProperty property, Object value) {
        String dataType = property.getDataType();

        try {
            switch (dataType) {
                case "int":
                    if (!(value instanceof Integer || value instanceof Long)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 int，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                case "float":
                    if (!(value instanceof Number)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 float，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                case "bool":
                    if (!(value instanceof Boolean)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 bool，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                case "string":
                    if (!(value instanceof String)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 string，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                // JSON, enum, text, date 等类型可以暂时跳过严格校验
                default:
                    log.debug("属性 {} 类型 {} 跳过校验", property.getPropertyIdentifier(), dataType);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("数据类型校验异常: {}", e.getMessage());
        }
    }

    /**
     * 校验数据取值范围
     */
    private void validateDataRange(ProductProperty property, Object value) {
        JsonNode spec = property.getSpec();
        if (spec == null || !value instanceof Number) {
            return;
        }

        double numValue = ((Number) value).doubleValue();

        // 校验 min/max
        if (spec.has("min") && numValue < spec.get("min").asDouble()) {
            log.warn("属性 {} 值 {} 小于最小值 {}",
                    property.getPropertyIdentifier(), numValue, spec.get("min").asDouble());
        }

        if (spec.has("max") && numValue > spec.get("max").asDouble()) {
            log.warn("属性 {} 值 {} 大于最大值 {}",
                    property.getPropertyIdentifier(), numValue, spec.get("max").asDouble());
        }
    }

    /**
     * 验证设备访问权限
     */
    private void validateDeviceAccess(Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        String tenantId = TenantContext.getTenantId();
        if (!device.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该设备");
        }
    }

    /**
     * 发送数据到 Kafka（异步处理）
     */
    private void sendToKafka(Device device, Map<String, Object> data, LocalDateTime dataTime) {
        try {
            // 构造 Kafka 消息
            Map<String, Object> message = Map.of(
                    "deviceId", device.getId(),
                    "deviceCode", device.getDeviceCode(),
                    "productId", device.getProductId(),
                    "data", data,
                    "dataTime", dataTime.toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);

            // 发送到 Kafka topic
            kafkaTemplate.send("device-data", jsonMessage);

            log.debug("设备数据已发送到 Kafka: device={}", device.getDeviceCode());

        } catch (Exception e) {
            log.error("发送数据到 Kafka 失败: {}", e.getMessage());
            // Kafka 发送失败不影响主流程
        }
    }
}

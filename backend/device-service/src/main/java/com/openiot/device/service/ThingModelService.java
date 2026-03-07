package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Product;
import com.openiot.device.entity.ProductEvent;
import com.openiot.device.entity.ProductProperty;
import com.openiot.device.mapper.ProductEventMapper;
import com.openiot.device.mapper.ProductPropertyMapper;
import com.openiot.device.mapper.ProductServiceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 物模型服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThingModelService extends ServiceImpl<ProductPropertyMapper, ProductProperty> {

    private final ProductPropertyMapper propertyMapper;
    private final ProductEventMapper eventMapper;
    private final ProductServiceMapper serviceMapper;
    private final com.openiot.device.service.ProductService productService;
    private final com.openiot.device.mapper.ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    /**
     * 保存产品物模型（JSON格式）
     *
     * @param productId   产品ID
     * @param thingModelJson 物模型JSON字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveThingModel(Long productId, String thingModelJson) {
        // 验证产品是否存在
        Product product = productService.getProductById(productId);

        // 解析物模型JSON
        try {
            JsonNode rootNode = objectMapper.readTree(thingModelJson);

            // 删除旧的物模型定义
            deleteThingModel(productId);

            // 保存属性定义
            if (rootNode.has("properties")) {
                saveProperties(productId, rootNode.get("properties"));
            }

            // 保存事件定义
            if (rootNode.has("events")) {
                saveEvents(productId, rootNode.get("events"));
            }

            // 保存服务定义
            if (rootNode.has("services")) {
                saveServices(productId, rootNode.get("services"));
            }

            // 更新产品表的 thing_model 字段（JSONB）
            product.setThingModel(rootNode);
            productMapper.updateById(product);

            log.info("保存物模型成功: productId={}", productId);

        } catch (JsonProcessingException e) {
            log.error("解析物模型JSON失败: {}", e.getMessage());
            throw BusinessException.badRequest("物模型JSON格式错误: " + e.getMessage());
        }
    }

    /**
     * 删除产品物模型
     *
     * @param productId 产品ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteThingModel(Long productId) {
        // 删除属性
        LambdaQueryWrapper<ProductProperty> propertyWrapper = new LambdaQueryWrapper<>();
        propertyWrapper.eq(ProductProperty::getProductId, productId);
        propertyMapper.delete(propertyWrapper);

        // 删除事件
        LambdaQueryWrapper<ProductEvent> eventWrapper = new LambdaQueryWrapper<>();
        eventWrapper.eq(ProductEvent::getProductId, productId);
        eventMapper.delete(eventWrapper);

        // 删除服务
        LambdaQueryWrapper<com.openiot.device.entity.ProductService> serviceWrapper = new LambdaQueryWrapper<>();
        serviceWrapper.eq(com.openiot.device.entity.ProductService::getProductId, productId);
        serviceMapper.delete(serviceWrapper);

        log.info("删除物模型成功: productId={}", productId);
    }

    /**
     * 查询产品属性列表
     *
     * @param productId 产品ID
     * @return 属性列表
     */
    public List<ProductProperty> getProperties(Long productId) {
        LambdaQueryWrapper<ProductProperty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductProperty::getProductId, productId);
        return propertyMapper.selectList(wrapper);
    }

    /**
     * 查询产品事件列表
     *
     * @param productId 产品ID
     * @return 事件列表
     */
    public List<ProductEvent> getEvents(Long productId) {
        LambdaQueryWrapper<ProductEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductEvent::getProductId, productId);
        return eventMapper.selectList(wrapper);
    }

    /**
     * 查询产品服务列表
     *
     * @param productId 产品ID
     * @return 服务列表
     */
    public List<com.openiot.device.entity.ProductService> getServices(Long productId) {
        LambdaQueryWrapper<com.openiot.device.entity.ProductService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.openiot.device.entity.ProductService::getProductId, productId);
        return serviceMapper.selectList(wrapper);
    }

    /**
     * 保存属性定义
     */
    private void saveProperties(Long productId, JsonNode propertiesNode) {
        List<ProductProperty> properties = new ArrayList<>();
        Long tenantId = getTenantId();

        for (JsonNode propertyNode : propertiesNode) {
            ProductProperty property = new ProductProperty();
            property.setTenantId(tenantId);
            property.setProductId(productId);
            property.setPropertyIdentifier(propertyNode.get("identifier").asText());
            property.setPropertyName(propertyNode.get("name").asText());
            property.setDataType(propertyNode.get("dataType").asText());

            if (propertyNode.has("spec")) {
                property.setSpec(propertyNode.get("spec"));
            }

            if (propertyNode.has("readWriteFlag")) {
                property.setReadWriteFlag(propertyNode.get("readWriteFlag").asText());
            } else {
                property.setReadWriteFlag("r"); // 默认只读
            }

            properties.add(property);
        }

        if (!properties.isEmpty()) {
            properties.forEach(propertyMapper::insert);
        }

        log.info("保存属性定义成功: productId={}, count={}", productId, properties.size());
    }

    /**
     * 保存事件定义
     */
    private void saveEvents(Long productId, JsonNode eventsNode) {
        List<ProductEvent> events = new ArrayList<>();
        Long tenantId = getTenantId();

        for (JsonNode eventNode : eventsNode) {
            ProductEvent event = new ProductEvent();
            event.setTenantId(tenantId);
            event.setProductId(productId);
            event.setEventIdentifier(eventNode.get("identifier").asText());
            event.setEventName(eventNode.get("name").asText());
            event.setEventType(eventNode.get("type").asText());

            if (eventNode.has("level")) {
                event.setEventLevel(eventNode.get("level").asText());
            }

            if (eventNode.has("params")) {
                event.setParams(eventNode.get("params"));
            }

            events.add(event);
        }

        if (!events.isEmpty()) {
            events.forEach(eventMapper::insert);
        }

        log.info("保存事件定义成功: productId={}, count={}", productId, events.size());
    }

    /**
     * 保存服务定义
     */
    private void saveServices(Long productId, JsonNode servicesNode) {
        List<com.openiot.device.entity.ProductService> services = new ArrayList<>();
        Long tenantId = getTenantId();

        for (JsonNode serviceNode : servicesNode) {
            com.openiot.device.entity.ProductService service = new com.openiot.device.entity.ProductService();
            service.setTenantId(tenantId);
            service.setProductId(productId);
            service.setServiceIdentifier(serviceNode.get("identifier").asText());
            service.setServiceName(serviceNode.get("name").asText());
            service.setCallType(serviceNode.get("callType").asText());

            if (serviceNode.has("inputParams")) {
                service.setInputParams(serviceNode.get("inputParams"));
            }

            if (serviceNode.has("outputParams")) {
                service.setOutputParams(serviceNode.get("outputParams"));
            }

            services.add(service);
        }

        if (!services.isEmpty()) {
            services.forEach(serviceMapper::insert);
        }

        log.info("保存服务定义成功: productId={}, count={}", productId, services.size());
    }

    /**
     * 获取当前租户ID
     */
    private Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.unauthorized("未找到租户信息");
        }
        return Long.valueOf(tenantId);
    }
}

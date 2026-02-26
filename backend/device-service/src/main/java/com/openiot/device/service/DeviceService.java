package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 设备服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService extends ServiceImpl<DeviceMapper, Device> {

    /**
     * 创建设备
     */
    public Device createDevice(Device device) {
        // 设置租户ID
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            device.setTenantId(Long.valueOf(tenantId));
        }

        // 检查设备编码是否已存在
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getTenantId, device.getTenantId())
               .eq(Device::getDeviceCode, device.getDeviceCode());
        if (this.count(wrapper) > 0) {
            throw BusinessException.badRequest("设备编码已存在: " + device.getDeviceCode());
        }

        // 生成设备 Token
        device.setDeviceToken(generateToken());

        // 设置默认值
        if (device.getStatus() == null) {
            device.setStatus("1");
        }

        this.save(device);
        log.info("创建设备成功: {} (tenant={})", device.getDeviceCode(), device.getTenantId());
        return device;
    }

    /**
     * 根据设备编码查询
     */
    public Device getByCode(String deviceCode) {
        String tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceCode, deviceCode);
        if (tenantId != null) {
            wrapper.eq(Device::getTenantId, Long.valueOf(tenantId));
        }
        return this.getOne(wrapper);
    }

    /**
     * 分页查询设备
     */
    public Page<Device> page(int pageNum, int pageSize, String status, String protocolType) {
        Page<Device> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();

        // 租户过滤
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.eq(Device::getTenantId, Long.valueOf(tenantId));
        }

        // 条件过滤
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Device::getStatus, status);
        }
        if (protocolType != null && !protocolType.isEmpty()) {
            wrapper.eq(Device::getProtocolType, protocolType);
        }

        wrapper.orderByDesc(Device::getCreateTime);
        return this.page(page, wrapper);
    }

    /**
     * 重新生成设备 Token
     */
    public String regenerateToken(Long deviceId) {
        Device device = this.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        // 权限检查
        checkTenantAccess(device);

        String newToken = generateToken();
        device.setDeviceToken(newToken);
        this.updateById(device);
        log.info("重新生成设备Token: {}", device.getDeviceCode());
        return newToken;
    }

    /**
     * 生成设备 Token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 检查租户访问权限
     */
    private void checkTenantAccess(Device device) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(String.valueOf(device.getTenantId()))) {
            log.warn("跨租户访问被拒绝: current={}, target={}", tenantId, device.getTenantId());
            throw BusinessException.forbidden("无权访问该设备");
        }
    }
}

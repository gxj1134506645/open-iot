package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 设备令牌服务
 * 负责设备 Token 的生成、验证、刷新等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceMapper deviceMapper;

    /**
     * 生成设备 Token
     * 格式：UUID 去除横线
     */
    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证设备 Token
     * 用于 MQTT/HTTP 设备认证
     *
     * @param deviceCode 设备编码
     * @param token 设备 Token
     * @return 设备信息
     */
    public Device validateToken(String deviceCode, String token) {
        if (deviceCode == null || token == null) {
            log.warn("设备认证失败：参数为空 deviceCode={}", deviceCode);
            return null;
        }

        // 查询设备
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceCode, deviceCode)
               .eq(Device::getDeviceToken, token);

        Device device = deviceMapper.selectOne(wrapper);

        if (device == null) {
            log.warn("设备认证失败：设备不存在或 Token 错误 deviceCode={}", deviceCode);
            return null;
        }

        // 检查设备状态
        if (!"1".equals(device.getStatus())) {
            log.warn("设备认证失败：设备已禁用 deviceCode={}", deviceCode);
            return null;
        }

        log.debug("设备认证成功：deviceCode={}, tenantId={}", deviceCode, device.getTenantId());
        return device;
    }

    /**
     * 刷新设备 Token
     *
     * @param deviceId 设备 ID
     * @return 新 Token
     */
    public String refreshToken(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        String newToken = generateToken();
        device.setDeviceToken(newToken);
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        log.info("刷新设备 Token：deviceCode={}, newToken={}", device.getDeviceCode(), newToken);
        return newToken;
    }

    /**
     * 根据设备编码刷新 Token
     *
     * @param deviceCode 设备编码
     * @return 新 Token
     */
    public String refreshTokenByCode(String deviceCode) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceCode, deviceCode);
        Device device = deviceMapper.selectOne(wrapper);

        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceCode);
        }

        return refreshToken(device.getId());
    }

    /**
     * 撤销设备 Token
     * 将 Token 设置为无效值
     *
     * @param deviceId 设备 ID
     */
    public void revokeToken(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        device.setDeviceToken("REVOKED_" + System.currentTimeMillis());
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        log.info("撤销设备 Token：deviceCode={}", device.getDeviceCode());
    }

    /**
     * 检查 Token 是否即将过期（可选功能）
     * 预留扩展：可以根据 Token 创建时间判断是否需要刷新
     *
     * @param device 设备
     * @return true-即将过期，需要刷新；false-无需刷新
     */
    public boolean isTokenExpiringSoon(Device device) {
        // TODO: 根据 updateTime 判断 Token 是否即将过期
        // 示例：Token 有效期 30 天，剩余 7 天时提示刷新
        return false;
    }
}
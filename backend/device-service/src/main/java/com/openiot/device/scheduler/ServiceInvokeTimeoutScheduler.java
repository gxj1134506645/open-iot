package com.openiot.device.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.device.entity.DeviceServiceInvoke;
import com.openiot.device.mapper.DeviceServiceInvokeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务调用超时检测调度器
 *
 * 定期扫描超时的服务调用记录，更新状态为 TIMEOUT
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceInvokeTimeoutScheduler {

    private final DeviceServiceInvokeMapper deviceServiceInvokeMapper;

    /**
     * 超时时间（秒）
     */
    private static final int TIMEOUT_SECONDS = 30;

    /**
     * 状态常量
     */
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_CALLING = "calling";
    private static final String STATUS_TIMEOUT = "timeout";

    /**
     * 每 10 秒执行一次超时检测
     */
    @Scheduled(fixedRate = 10000)
    public void checkTimeoutInvocations() {
        // 计算超时阈值时间
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusSeconds(TIMEOUT_SECONDS);

        // 查询超时的调用记录（状态为 pending 或 calling，且调用时间超过阈值）
        LambdaQueryWrapper<DeviceServiceInvoke> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DeviceServiceInvoke::getStatus, STATUS_PENDING, STATUS_CALLING)
               .lt(DeviceServiceInvoke::getInvokeTime, timeoutThreshold);

        List<DeviceServiceInvoke> timeoutInvocations = deviceServiceInvokeMapper.selectList(wrapper);

        if (timeoutInvocations.isEmpty()) {
            return;
        }

        log.info("检测到 {} 条超时的服务调用", timeoutInvocations.size());

        // 更新超时状态
        for (DeviceServiceInvoke invoke : timeoutInvocations) {
            try {
                invoke.setStatus(STATUS_TIMEOUT);
                invoke.setCompleteTime(LocalDateTime.now());
                invoke.setErrorMessage("调用超时，设备未在 " + TIMEOUT_SECONDS + " 秒内响应");

                deviceServiceInvokeMapper.updateById(invoke);

                log.warn("服务调用超时: invokeId={}, deviceId={}, service={}",
                        invoke.getInvokeId(), invoke.getDeviceId(), invoke.getServiceIdentifier());

            } catch (Exception e) {
                log.error("更新超时状态失败: invokeId={}", invoke.getInvokeId(), e);
            }
        }
    }
}

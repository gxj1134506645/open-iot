package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.DeviceTrajectory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备轨迹 Mapper
 */
@Mapper
public interface DeviceTrajectoryMapper extends BaseMapper<DeviceTrajectory> {
}

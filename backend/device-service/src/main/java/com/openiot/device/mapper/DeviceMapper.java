package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.Device;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备 Mapper
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}

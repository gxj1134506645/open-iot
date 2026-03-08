package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.ProductEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品事件 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface ProductEventMapper extends BaseMapper<ProductEvent> {
}

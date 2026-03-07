package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.ProductProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品属性 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface ProductPropertyMapper extends BaseMapper<ProductProperty> {
}

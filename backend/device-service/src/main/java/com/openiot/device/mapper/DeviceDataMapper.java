package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.DeviceData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备数据 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface DeviceDataMapper extends BaseMapper<DeviceData> {

    /**
     * 查询设备在指定时间范围内的数据
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 数据列表
     */
    @Select("SELECT * FROM device_data " +
            "WHERE device_id = #{deviceId} " +
            "  AND data_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY data_time ASC")
    List<DeviceData> selectByTimeRange(@Param("deviceId") Long deviceId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查询设备最新的 N 条数据
     *
     * @param deviceId 设备ID
     * @param limit    条数
     * @return 数据列表
     */
    @Select("SELECT * FROM device_data " +
            "WHERE device_id = #{deviceId} " +
            "ORDER BY data_time DESC " +
            "LIMIT #{limit}")
    List<DeviceData> selectLatest(@Param("deviceId") Long deviceId,
                                   @Param("limit") int limit);

    /**
     * 查询产品下所有设备的最新数据
     *
     * @param productId 产品ID
     * @param limit     每个设备的返回条数
     * @return 数据列表
     */
    @Select("SELECT DISTINCT ON (device_id) * FROM device_data " +
            "WHERE product_id = #{productId} " +
            "ORDER BY device_id, data_time DESC " +
            "LIMIT #{limit}")
    List<DeviceData> selectLatestByProduct(@Param("productId") Long productId,
                                           @Param("limit") int limit);
}

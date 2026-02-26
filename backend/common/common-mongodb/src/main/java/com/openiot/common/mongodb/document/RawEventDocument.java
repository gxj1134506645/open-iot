package com.openiot.common.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 原始事件文档
 * 存储设备上报的原始数据，用于重放和审计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "raw_events")
@CompoundIndex(name = "tenant_device_time_idx", def = "{'tenantId': 1, 'deviceId': 1, 'timestamp': -1}")
public class RawEventDocument {

    /**
     * MongoDB 主键
     */
    @Id
    private String id;

    /**
     * 事件唯一标识（UUID）
     */
    @Indexed
    @Field("eventId")
    private String eventId;

    /**
     * 租户 ID
     */
    @Field("tenantId")
    @Indexed
    private String tenantId;

    /**
     * 设备 ID
     */
    @Field("deviceId")
    @Indexed
    private String deviceId;

    /**
     * 事件类型
     * TELEMETRY - 遥测数据
     * STATUS - 状态数据
     * ALARM - 告警数据
     */
    @Field("eventType")
    private String eventType;

    /**
     * 协议类型
     * MQTT/TCP/HTTP
     */
    @Field("protocol")
    private String protocol;

    /**
     * 原始载荷（Base64 编码）
     */
    @Field("rawPayload")
    private String rawPayload;

    /**
     * 解析后的载荷（可选）
     */
    @Field("parsedPayload")
    private Object parsedPayload;

    /**
     * 事件时间戳
     */
    @Field("timestamp")
    private LocalDateTime timestamp;

    /**
     * 是否已处理
     */
    @Field("processed")
    @Builder.Default
    private Boolean processed = false;

    /**
     * 处理结果
     * SUCCESS/FAILED
     */
    @Field("processResult")
    private String processResult;

    /**
     * 创建时间
     */
    @CreatedDate
    @Field("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Field("updateTime")
    private LocalDateTime updateTime;
}

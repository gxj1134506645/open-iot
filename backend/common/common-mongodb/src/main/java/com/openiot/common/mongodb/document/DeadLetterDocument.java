package com.openiot.common.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 死信队列文档
 * 存储解析失败的事件，支持重试
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dlq_events")
public class DeadLetterDocument {

    /**
     * MongoDB 主键
     */
    @Id
    private String id;

    /**
     * 原始事件 ID
     */
    @Indexed
    @Field("originalEventId")
    private String originalEventId;

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
    private String deviceId;

    /**
     * 原始载荷
     */
    @Field("rawPayload")
    private String rawPayload;

    /**
     * 失败原因
     */
    @Field("failureReason")
    private String failureReason;

    /**
     * 重试次数
     */
    @Field("retryCount")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 状态
     * PENDING - 等待重试
     * RETRYING - 重试中
     * RESOLVED - 已解决
     */
    @Indexed
    @Field("status")
    @Builder.Default
    private String status = "PENDING";

    /**
     * 创建时间
     */
    @CreatedDate
    @Field("createTime")
    private LocalDateTime createTime;

    /**
     * 最后重试时间
     */
    @Field("lastRetryTime")
    private LocalDateTime lastRetryTime;

    /**
     * 解决时间
     */
    @Field("resolvedTime")
    private LocalDateTime resolvedTime;

    // ==================== 状态常量 ====================

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RETRYING = "RETRYING";
    public static final String STATUS_RESOLVED = "RESOLVED";

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryTime = LocalDateTime.now();
    }

    /**
     * 标记为已解决
     */
    public void markResolved() {
        this.status = STATUS_RESOLVED;
        this.resolvedTime = LocalDateTime.now();
    }
}

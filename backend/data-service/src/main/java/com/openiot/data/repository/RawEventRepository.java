package com.openiot.data.repository;

import com.openiot.common.mongodb.document.RawEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原始事件 Repository
 */
@Repository
public interface RawEventRepository extends MongoRepository<RawEventDocument, String> {

    /**
     * 查询未处理的原始事件
     */
    List<RawEventDocument> findByProcessedFalse();

    /**
     * 按时间范围查询
     */
    List<RawEventDocument> findByTenantIdAndDeviceIdAndTimestampBetween(
            String tenantId, String deviceId, LocalDateTime start, LocalDateTime end);

    /**
     * 按租户查询未处理事件
     */
    List<RawEventDocument> findByTenantIdAndProcessedFalse(String tenantId);
}

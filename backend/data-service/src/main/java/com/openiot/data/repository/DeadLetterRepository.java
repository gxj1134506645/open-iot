package com.openiot.data.repository;

import com.openiot.common.mongodb.document.DeadLetterDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 死信队列 Repository
 */
@Repository
public interface DeadLetterRepository extends MongoRepository<DeadLetterDocument, String> {

    /**
     * 按状态查询
     */
    List<DeadLetterDocument> findByStatus(String status);

    /**
     * 按原始事件 ID 查询
     */
    DeadLetterDocument findByOriginalEventId(String originalEventId);
}

package com.openiot.common.mongodb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 配置类
 * 启用 MongoDB 审计和 Repository
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.openiot.*.repository")
public class MongoConfig {
}

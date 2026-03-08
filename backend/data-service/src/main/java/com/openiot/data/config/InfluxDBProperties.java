package com.openiot.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 配置属性
 *
 * @author OpenIoT Team
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "influxdb")
public class InfluxDBProperties {

    /**
     * InfluxDB 服务器地址
     */
    private String url = "http://localhost:8086";

    /**
     * 组织名称
     */
    private String org = "openiot";

    /**
     * 存储桶名称
     */
    private String bucket = "device-data";

    /**
     * 认证令牌
     */
    private String token = "admin-token";

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 写入超时时间（毫秒）
     */
    private int writeTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;

    /**
     * 批量写入大小
     */
    private int batchSize = 1000;

    /**
     * 刷间隔（毫秒）
     */
    private int flushInterval = 5000;

    /**
     * 是否启用 gzip 压缩
     */
    private boolean gzipEnabled = true;
}

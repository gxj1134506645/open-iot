-- ========================================
-- 设备数据表
-- Version: 1.3.0
-- Description: 存储设备上报的历史数据
-- ========================================

-- 创建设备数据表
CREATE TABLE IF NOT EXISTS device_data (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    data JSONB NOT NULL,              -- 设备上报数据（JSON格式）
    data_time TIMESTAMP NOT NULL,     -- 数据时间戳（设备上报时间）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 索引优化查询
    CONSTRAINT idx_device_data_time UNIQUE (device_id, data_time)
);

COMMENT ON TABLE device_data IS '设备历史数据表';
COMMENT ON COLUMN device_data.id IS '主键';
COMMENT ON COLUMN device_data.tenant_id IS '租户ID';
COMMENT ON COLUMN device_data.product_id IS '产品ID';
COMMENT ON COLUMN device_data.device_id IS '设备ID';
COMMENT ON COLUMN device_data.data IS '设备上报数据（JSON格式，根据物模型定义）';
COMMENT ON COLUMN device_data.data_time IS '数据时间戳（设备上报的时间）';
COMMENT ON COLUMN device_data.create_time IS '入库时间';

-- ========================================
-- 创建索引（优化查询性能）
-- ========================================

-- 按设备ID和时间范围查询（最常用）
CREATE INDEX IF NOT EXISTS idx_device_data_device_time
    ON device_data(device_id, data_time DESC);

-- 按产品ID和时间范围查询（用于产品维度统计）
CREATE INDEX IF NOT EXISTS idx_device_data_product_time
    ON device_data(product_id, data_time DESC);

-- 按租户查询（多租户隔离）
CREATE INDEX IF NOT EXISTS idx_device_data_tenant
    ON device_data(tenant_id, data_time DESC);

-- 按时间范围查询（用于全局数据统计）
CREATE INDEX IF NOT EXISTS idx_device_data_time
    ON device_data(data_time DESC);

-- ========================================
-- 数据分区策略（可选，生产环境建议使用）
-- ========================================
-- 按月分区可以提高大数据量下的查询性能
-- 示例（PostgreSQL 10+）：
-- CREATE TABLE device_data_2026_03 PARTITION OF device_data
--     FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- ========================================
-- 数据清理策略
-- ========================================
-- 建议使用定时任务清理历史数据（如保留90天）
-- DELETE FROM device_data WHERE data_time < NOW() - INTERVAL '90 days';

-- ========================================
-- 使用示例
-- ========================================
-- 插入设备数据：
-- INSERT INTO device_data (tenant_id, product_id, device_id, data, data_time)
-- VALUES (1, 2, 3, '{"temperature": 25.5, "humidity": 60.2}', '2026-03-07 12:00:00');

-- 查询设备最新10条数据：
-- SELECT * FROM device_data
-- WHERE device_id = 3
-- ORDER BY data_time DESC
-- LIMIT 10;

-- 查询时间范围内的数据：
-- SELECT * FROM device_data
-- WHERE device_id = 3
--   AND data_time BETWEEN '2026-03-07 00:00:00' AND '2026-03-07 23:59:59'
-- ORDER BY data_time ASC;

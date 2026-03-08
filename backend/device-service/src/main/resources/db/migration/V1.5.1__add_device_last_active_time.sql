-- ========================================
-- 添加设备最后活跃时间字段
-- Version: 1.5.1
-- Description: device 表缺少 last_active_time 列，导致 MyBatis Plus 查询报错
-- ========================================

ALTER TABLE device ADD COLUMN IF NOT EXISTS last_active_time TIMESTAMP;

COMMENT ON COLUMN device.last_active_time IS '设备最后活跃时间（最近一次心跳或数据上报时间）';

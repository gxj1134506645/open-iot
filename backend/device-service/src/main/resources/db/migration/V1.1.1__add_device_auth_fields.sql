-- ========================================
-- 添加设备认证字段和产品关联
-- Version: 1.1.1
-- Description: 添加 device_key, device_secret, product_id 字段
-- ========================================

-- 添加设备密钥字段（用于设备三段式认证：productKey/deviceName/deviceSecret）
ALTER TABLE device ADD COLUMN IF NOT EXISTS device_key VARCHAR(50) UNIQUE;
ALTER TABLE device ADD COLUMN IF NOT EXISTS device_secret VARCHAR(100);

-- 添加产品关联字段
ALTER TABLE device ADD COLUMN IF NOT EXISTS product_id BIGINT;

-- 添加注释
COMMENT ON COLUMN device.device_key IS '设备密钥（全局唯一，用于设备认证）';
COMMENT ON COLUMN device.device_secret IS '设备密钥（BCrypt哈希，用于设备认证）';
COMMENT ON COLUMN device.product_id IS '产品ID';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_device_product ON device(product_id);
CREATE INDEX IF NOT EXISTS idx_device_key ON device(device_key);

-- 为现有设备生成默认值（可选）
-- UPDATE device SET device_key = 'DK' || substring(id::text, 1, 6) || substring(md5(random()::text), 1, 8) WHERE device_key IS NULL;
-- UPDATE device SET device_secret = '$2a$10$' || substring(md5(random()::text), 1, 53) WHERE device_secret IS NULL;

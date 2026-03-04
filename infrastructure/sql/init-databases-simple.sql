-- ========================================
-- Open-IoT 独立数据库初始化脚本（精简版）
-- Version: 1.0.1
-- Description: 为每个微服务创建独立数据库
-- ========================================

-- ========================================
-- 1. 创建数据库
-- ========================================

CREATE DATABASE openiot_tenant
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

COMMENT ON DATABASE openiot_tenant IS '租户服务数据库';

CREATE DATABASE openiot_device
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

COMMENT ON DATABASE openiot_device IS '设备服务数据库';

CREATE DATABASE openiot_data
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

COMMENT ON DATABASE openiot_data IS '数据服务数据库';

CREATE DATABASE openiot_connect
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

COMMENT ON DATABASE openiot_connect IS '连接服务数据库';

-- ========================================
-- 2. 创建用户（如果不存在）或更新密码
-- ========================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'openiot') THEN
        CREATE USER openiot WITH PASSWORD 'openiot123';
        RAISE NOTICE '用户 openiot 已创建';
    ELSE
        ALTER USER openiot WITH PASSWORD 'openiot123';
        RAISE NOTICE '用户 openiot 已存在，密码已更新';
    END IF;
END
$$;

-- ========================================
-- 3. 授予数据库权限
-- ========================================

GRANT ALL PRIVILEGES ON DATABASE openiot_tenant TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_device TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_data TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_connect TO openiot;

-- ========================================
-- 4. Schema 权限授权
-- ========================================
-- ⚠️ 重要：必须在每个数据库中执行以下授权语句
-- Flyway 和应用程序需要 Schema 权限才能创建表和读写数据

-- \connect openiot_tenant
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- \connect openiot_device
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- \connect openiot_data
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- \connect openiot_connect
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- ========================================
-- 执行说明
-- ========================================
-- 1. 执行此脚本（以 postgres 用户）
--    psql -U postgres -f init-databases-simple.sql
--
-- 2. 执行 Schema 授权（复制上面的语句在每个数据库中执行）
--
-- 3. 验证
--    psql -U openiot -d openiot_tenant -c "SELECT current_database(), current_user;"

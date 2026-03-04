-- ========================================
-- Schema 权限授权脚本
-- Version: 1.0.0
-- Description: 为所有数据库授权 Schema 权限
-- ========================================
-- ⚠️ 必须执行此脚本！
-- 仅创建数据库和用户是不够的，Flyway 需要 Schema 权限才能创建表
-- ========================================

-- 租户服务数据库
\connect openiot_tenant

GRANT ALL ON SCHEMA public TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

RAISE NOTICE 'openiot_tenant Schema 权限已授权';

-- 设备服务数据库
\connect openiot_device

GRANT ALL ON SCHEMA public TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

RAISE NOTICE 'openiot_device Schema 权限已授权';

-- 数据服务数据库
\connect openiot_data

GRANT ALL ON SCHEMA public TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

RAISE NOTICE 'openiot_data Schema 权限已授权';

-- 连接服务数据库
\connect openiot_connect

GRANT ALL ON SCHEMA public TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

RAISE NOTICE 'openiot_connect Schema 权限已授权';

-- 验证
\connect openiot_tenant
SELECT
    current_database() as database,
    current_user as user,
    has_schema_privilege('openiot', 'public', 'CREATE') as can_create;

-- 预期输出：
--  database      |  user    | can_create
-- ---------------+---------+------------
--  openiot_tenant | openiot | t

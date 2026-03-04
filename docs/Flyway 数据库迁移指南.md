# Flyway 数据库迁移指南

> Open-IoT 项目 Flyway 实践指南

## 当前架构

项目采用**独立数据库架构**，每个微服务拥有独立数据库和迁移脚本：

```
PostgreSQL 集群
├── openiot_tenant (tenant-service)
├── openiot_device (device-service)
├── openiot_data (data-service)
└── openiot_connect (connect-service)
```

## 迁移脚本组织

```
backend/
├── tenant-service/src/main/resources/db/migration/
│   ├── V1.0.0__init_tenant_schema.sql    # 租户、用户表
│   ├── V1.0.1__init_tenant_data.sql      # 初始数据
│   └── V1.1.0__rbac_tables.sql           # RBAC 权限表
│
├── device-service/src/main/resources/db/migration/
│   └── V1.0.0__init_device_schema.sql    # 设备表
│
├── data-service/src/main/resources/db/migration/
│   └── V1.0.0__init_data_schema.sql      # 设备轨迹表
│
└── connect-service/src/main/resources/db/migration/
    └── V1.0.0__init_connect_schema.sql   # 设备会话、连接日志表
```

**特点**：
- ✅ 每个服务独立管理自己的数据库迁移
- ✅ 版本号可以重复（每个服务独立版本序列）
- ✅ 所有服务都启用 Flyway
- ✅ 使用默认的 `classpath:db/migration` 位置

## Flyway 配置

所有服务的配置相同：

```yaml
# src/main/resources/application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    locations: classpath:db/migration  # 默认值，可省略
```

## Flyway 工作原理

### ⚠️ 核心理解

**Flyway 不会自动检测 Entity 变化！**

```
❌ 错误理解：
Entity 代码更新 → Flyway 自动检测 → 自动生成 SQL → 自动执行

✅ 正确理解：
开发者手动编写 SQL → Flyway 执行 SQL → 更新数据库
```

### 工作流程

```
1. 开发者编写迁移脚本（V1.0.1__xxx.sql）
2. 重启服务
3. Flyway 检测到新脚本 → 执行 SQL → 记录到 flyway_schema_history
4. 数据库更新完成
```

## 迁移脚本命名规范

### 格式

```
V<版本号>__<描述>.sql
```

### 版本号规则

- 格式：`主版本.次版本.修订号`（如 `1.0.0`、`1.1.0`）
- 必须**递增**，不能跳过
- 每个服务独立版本序列
- 使用双下划线 `__` 分隔版本号和描述

### 描述规范

- 使用英文小写 + 下划线（snake_case）
- 简洁明了，描述做了什么
- 示例：`add_alarm_table`、`update_user_status_type`

### 示例

```
✅ 好的命名：
V1.0.0__init_tenant_schema.sql
V1.0.1__init_tenant_data.sql
V1.1.0__rbac_tables.sql
V1.2.0__add_tenant_quota.sql
V1.2.1__add_index_for_tenant_status.sql

❌ 不好的命名：
V1.0__init.sql                      # 版本号不够详细
V1.0.0_update.sql                   # 使用单下划线
v1.0.0__InitSchema.sql              # 大小写混乱
```

## SQL 编写规范

### 推荐写法

```sql
-- ========================================
-- 租户配额表
-- Version: 1.2.0
-- Description: 添加租户配额管理功能
-- ========================================

-- 1. 创建表
CREATE TABLE IF NOT EXISTS tenant_quota (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    quota_type VARCHAR(50) NOT NULL,
    quota_limit INTEGER NOT NULL,
    quota_used INTEGER DEFAULT 0,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_tenant_quota UNIQUE (tenant_id, quota_type)
);

-- 2. 添加注释
COMMENT ON TABLE tenant_quota IS '租户配额表';
COMMENT ON COLUMN tenant_quota.id IS '主键';
COMMENT ON COLUMN tenant_quota.tenant_id IS '租户ID';
COMMENT ON COLUMN tenant_quota.quota_type IS '配额类型：device-设备数，user-用户数';
COMMENT ON COLUMN tenant_quota.quota_limit IS '配额上限';
COMMENT ON COLUMN tenant_quota.quota_used IS '已使用配额';
COMMENT ON COLUMN tenant_quota.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN tenant_quota.delete_flag IS '删除标记：0-正常，1-已删除';

-- 3. 创建索引
CREATE INDEX IF NOT EXISTS idx_tenant_quota_tenant
    ON tenant_quota(tenant_id, delete_flag);

-- 4. 添加外键约束（可选）
ALTER TABLE tenant_quota
    ADD CONSTRAINT fk_tenant_quota_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
    ON DELETE CASCADE;
```

### 最佳实践

1. **幂等性**：使用 `IF NOT EXISTS`、`IF EXISTS`
2. **注释完整**：表注释、字段注释、索引说明
3. **索引优化**：为常用查询字段创建索引
4. **约束明确**：主键、唯一键、外键、默认值
5. **分步执行**：创建表 → 添加注释 → 创建索引 → 添加约束

## 常见操作

### 场景1：添加新表

**需求**：tenant-service 需要添加租户配额表

**步骤**：

1. 确定当前最大版本号
   ```bash
   ls backend/tenant-service/src/main/resources/db/migration/
   # 输出：V1.0.0..., V1.0.1..., V1.1.0...
   # 当前最大版本：1.1.0
   ```

2. 创建新迁移脚本
   ```bash
   cd backend/tenant-service/src/main/resources/db/migration/
   touch V1.2.0__add_tenant_quota.sql
   ```

3. 编写 SQL（参考上面的推荐写法）

4. 重启服务
   ```bash
   cd backend/tenant-service
   mvn spring-boot:run
   ```

5. 验证
   ```bash
   psql -U openiot -d openiot_tenant -c "\d tenant_quota"
   ```

### 场景2：修改表结构

**需求**：device 表添加 `last_online_time` 字段

**步骤**：

1. 创建迁移脚本
   ```bash
   cd backend/device-service/src/main/resources/db/migration/
   touch V1.0.1__add_device_last_online_time.sql
   ```

2. 编写 SQL
   ```sql
   -- ========================================
   -- 添加设备最后在线时间字段
   -- Version: 1.0.1
   -- ========================================

   ALTER TABLE device
       ADD COLUMN IF NOT EXISTS last_online_time TIMESTAMP;

   COMMENT ON COLUMN device.last_online_time IS '最后在线时间';
   ```

3. 重启服务并验证

### 场景3：添加索引

**需求**：为 device 表的 `tenant_id` 和 `status` 字段创建复合索引

**步骤**：

1. 创建迁移脚本
   ```bash
   cd backend/device-service/src/main/resources/db/migration/
   touch V1.0.2__add_index_for_device.sql
   ```

2. 编写 SQL
   ```sql
   -- ========================================
   -- 添加设备表索引
   -- Version: 1.0.2
   -- ========================================

   CREATE INDEX IF NOT EXISTS idx_device_tenant_status
       ON device(tenant_id, status, delete_flag);
   ```

3. 重启服务并验证

## Flyway 历史记录查询

### 查看已执行的迁移

```sql
-- 连接数据库
psql -U openiot -d openiot_tenant

-- 查询迁移历史
SELECT
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
ORDER BY installed_rank;
```

**示例输出**：
```
 installed_rank | version |      description       | type |              script              |        installed_on         | execution_time | success
----------------+---------+------------------------+------+----------------------------------+-----------------------------+----------------+---------
              1 | 1.0.0   | init tenant schema     | SQL  | V1.0.0__init_tenant_schema.sql   | 2026-03-03 16:49:23.456789  |            245 | t
              2 | 1.0.1   | init tenant data       | SQL  | V1.0.1__init_tenant_data.sql     | 2026-03-03 16:49:23.789012  |             32 | t
              3 | 1.1.0   | rbac tables            | SQL  | V1.1.0__rbac_tables.sql          | 2026-03-03 16:50:12.345678  |            156 | t
```

## 常见问题

### 1. 版本号冲突

**错误**：
```
Found more than one migration with version 1.0.0
```

**原因**：存在重复的版本号

**解决**：检查迁移脚本，确保版本号唯一且递增

### 2. 校验失败

**错误**：
```
Migration checksum mismatch for migration version 1.0.0
```

**原因**：已执行的脚本被修改

**解决**：
- **开发环境**：删除 `flyway_schema_history` 表并清空相关表，重新执行
- **生产环境**：创建新脚本修复，不要修改已执行的脚本

### 3. 脚本执行失败

**错误**：
```
Migration V1.1.0__xxx.sql failed
```

**解决**：
1. 检查 SQL 语法
2. 手动修复数据库
3. 删除 `flyway_schema_history` 中失败的记录
4. 重新启动服务

### 4. 跨数据库外键

**问题**：想在 `device` 表添加 `tenant_id` 外键

**解决**：
- ❌ 不能使用数据库外键（跨数据库）
- ✅ 使用应用层校验

```java
// device-service 创建设备时
public void createDevice(DeviceDTO dto) {
    // 调用 tenant-service API 检查租户是否存在
    Tenant tenant = tenantClient.getTenant(dto.getTenantId());
    if (tenant == null) {
        throw new BusinessException("租户不存在");
    }

    // 创建设备
    deviceMapper.insert(dto);
}
```

## 数据库初始化

### 方式1：使用数据库客户端（推荐）

```sql
-- 以 postgres 超级用户连接
psql -U postgres

-- 创建数据库
CREATE DATABASE openiot_tenant;
CREATE DATABASE openiot_device;
CREATE DATABASE openiot_data;
CREATE DATABASE openiot_connect;

-- 创建用户
CREATE USER openiot WITH PASSWORD 'openiot123';

-- 授予数据库权限
GRANT ALL PRIVILEGES ON DATABASE openiot_tenant TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_device TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_data TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_connect TO openiot;

-- 在每个数据库中执行
\connect openiot_tenant
GRANT ALL ON SCHEMA public TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- 重复其他 3 个数据库
```

### 方式2：启动服务自动创建

启动服务后，Flyway 会自动执行迁移脚本创建表结构：

```bash
cd backend/tenant-service
mvn spring-boot:run
```

## 最佳实践

### 1. 版本号管理

- ✅ 使用语义化版本号（1.0.0、1.1.0、1.2.0）
- ✅ 每次迁移递增版本号
- ✅ 每个服务独立版本序列

### 2. 脚本编写

- ✅ 使用幂等性语句（IF NOT EXISTS、IF EXISTS）
- ✅ 添加详细注释
- ✅ 创建必要的索引
- ✅ 先在开发环境测试

### 3. 团队协作

- ✅ 提交代码前先拉取最新代码，避免版本号冲突
- ✅ 新增迁移脚本后及时通知团队成员
- ✅ 使用代码审查确保 SQL 质量

### 4. 生产环境

- ✅ 备份数据库后再执行迁移
- ✅ 在测试环境先验证
- ✅ 保留回滚方案
- ✅ 监控迁移执行时间

## 参考资料

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [PostgreSQL 官方文档](https://www.postgresql.org/docs/)
- [数据库架构说明](./数据库架构说明.md)

---

**文档版本**：v2.0.0
**最后更新**：2026-03-04
**维护者**：OpenIoT Team

# Flyway 数据库迁移实践指南

> 本指南结合 open-iot 项目，详细说明 Flyway 的使用方法、最佳实践和完整工作流程

## 目录

- [1. Flyway 核心概念](#1-flyway-核心概念)
- [2. 项目架构说明](#2-项目架构说明)
- [3. 迁移脚本编写规范](#3-迁移脚本编写规范)
- [4. 完整工作流程](#4-完整工作流程)
- [5. 代码与数据库同步策略](#5-代码与数据库同步策略)
- [6. 数据备份策略](#6-数据备份策略)
- [7. 常见场景实践](#7-常见场景实践)
- [8. 故障处理](#8-故障处理)

---

## 1. Flyway 核心概念

### 1.1 什么是 Flyway？

Flyway 是一个开源的数据库版本管理工具，核心思想是：
- **版本化管理**：每次数据库变更都是一个版本
- **增量迁移**：只执行未执行过的迁移脚本
- **可追溯**：通过 `flyway_schema_history` 表记录所有迁移历史

### 1.2 核心命令

| 命令 | 说明 | 使用场景 |
|------|------|---------|
| `migrate` | 执行迁移 | 应用启动时自动执行 |
| `info` | 查看迁移状态 | 检查哪些脚本已执行/未执行 |
| `validate` | 校验迁移脚本 | 生产环境启动前校验 |
| `repair` | 修复迁移历史 | 修复 checksum 不匹配等问题 |
| `clean` | 清空数据库 | **仅开发环境**，删除所有对象 |

### 1.3 迁移脚本类型

```
V1.0.0__init_schema.sql          # Versioned Migration（版本化迁移）
V1.0.1__init_data.sql            # Versioned Migration（带数据）
R__truncate_temp_table.sql       # Repeatable Migration（可重复执行）
U1.0.1__drop_user_table.sql      # Undo Migration（回滚，企业版功能）
```

---

## 2. 项目架构说明

### 2.1 共享数据库架构

```
┌──────────────────────────────────────────────┐
│          PostgreSQL (openiot)                │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │ flyway_schema_history                  │ │
│  │ (记录所有迁移历史)                      │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │ tenant、sys_user、device、             │ │
│  │ device_trajectory 等业务表             │ │
│  └────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
                     ▲
                     │ Flyway Migrate
                     │
          ┌──────────┴──────────┐
          │  tenant-service     │ ✅ 唯一启用 Flyway
          │  (端口: 8086)       │    管理所有表 DDL
          └─────────────────────┘

          ┌─────────────────────┐
          │  device-service     │ ❌ Flyway 禁用
          │  (端口: 8081)       │    仅 CRUD 操作
          └─────────────────────┘

          ┌─────────────────────┐
          │  data-service       │ ❌ Flyway 禁用
          │  (端口: 8085)       │    仅 CRUD 操作
          └─────────────────────┘
```

### 2.2 迁移脚本位置

```
backend/tenant-service/src/main/resources/db/migration/
├── V1.0.0__init_schema.sql          # 初始化表结构
├── V1.0.1__init_data.sql            # 初始化数据
├── V1.1.0__add_device_config.sql    # 新增设备配置表
├── V1.1.1__add_index_for_device.sql # 新增索引
└── ...
```

---

## 3. 迁移脚本编写规范

### 3.1 命名规范

**格式**：`V<版本号>__<描述>.sql`

**版本号规则**：
- 主版本.次版本.修订号（如 `1.0.0`、`1.1.0`）
- 必须递增，不能跳过
- 使用双下划线 `__` 分隔版本号和描述

**描述规范**：
- 使用英文小写 + 下划线（snake_case）
- 简洁明了，描述做了什么
- 示例：`add_alarm_table`、`update_user_status_type`

**示例**：
```
✅ 好的命名：
V1.0.0__init_schema.sql
V1.1.0__add_device_config_table.sql
V1.1.1__add_index_for_device_status.sql
V1.2.0__modify_device_token_length.sql

❌ 不好的命名：
V1.0__init.sql                      # 版本号不够详细
V1.0.0_update.sql                   # 使用单下划线
v1.0.0__InitSchema.sql              # 大小写混乱
V1.0.0__add-table.sql               # 使用连字符
```

### 3.2 SQL 编写规范

#### ✅ 推荐写法

```sql
-- ========================================
-- 设备配置表
-- Version: 1.1.0
-- Description: 添加设备配置表，支持设备参数配置
-- ========================================

-- 1. 创建表
CREATE TABLE IF NOT EXISTS device_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_device_config UNIQUE (device_id, config_key)
);

-- 2. 添加注释
COMMENT ON TABLE device_config IS '设备配置表';
COMMENT ON COLUMN device_config.id IS '主键';
COMMENT ON COLUMN device_config.tenant_id IS '租户ID';
COMMENT ON COLUMN device_config.device_id IS '设备ID';
COMMENT ON COLUMN device_config.config_key IS '配置键';
COMMENT ON COLUMN device_config.config_value IS '配置值';
COMMENT ON COLUMN device_config.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN device_config.delete_flag IS '删除标记：0-正常，1-已删除';

-- 3. 创建索引
CREATE INDEX IF NOT EXISTS idx_device_config_tenant
    ON device_config(tenant_id, delete_flag);
CREATE INDEX IF NOT EXISTS idx_device_config_device
    ON device_config(device_id, status);

-- 4. 添加外键约束（可选）
ALTER TABLE device_config
    ADD CONSTRAINT fk_device_config_device
    FOREIGN KEY (device_id) REFERENCES device(id)
    ON DELETE CASCADE;
```

#### ❌ 避免的写法

```sql
-- ❌ 不要使用 DROP TABLE（会丢失数据）
DROP TABLE IF EXISTS device_config;
CREATE TABLE device_config (...);

-- ❌ 不要遗漏 IF NOT EXISTS（重复执行会报错）
CREATE TABLE device_config (...);

-- ❌ 不要硬编码数据（使用参数化）
INSERT INTO sys_user VALUES (1, 'admin', '123456');

-- ❌ 不要遗漏注释（影响可维护性）
CREATE TABLE device_config (...);  -- 没有注释
```

### 3.3 数据迁移脚本示例

```sql
-- ========================================
-- 初始化管理员数据
-- Version: 1.0.1
-- ========================================

-- 插入平台管理员（密码：admin123，使用 BCrypt 加密）
INSERT INTO sys_user (tenant_id, username, password, real_name, role, status, delete_flag, create_time)
VALUES
    (NULL, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'ADMIN', '1', '0', CURRENT_TIMESTAMP);

-- 插入测试租户
INSERT INTO tenant (tenant_code, tenant_name, contact_email, status, delete_flag, create_time)
VALUES
    ('TEST001', '测试租户', 'test@example.com', '1', '0', CURRENT_TIMESTAMP);

-- 插入租户管理员
INSERT INTO sys_user (tenant_id, username, password, real_name, role, status, delete_flag, create_time)
SELECT
    id,
    'tenant_admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
    '租户管理员',
    'TENANT_ADMIN',
    '1',
    '0',
    CURRENT_TIMESTAMP
FROM tenant
WHERE tenant_code = 'TEST001';
```

---

## 4. 完整工作流程

### 4.1 开发环境工作流程

#### 场景 1：新增业务表

**步骤 1：创建迁移脚本**

```bash
# 1. 进入迁移脚本目录
cd backend/tenant-service/src/main/resources/db/migration

# 2. 查看当前最新版本号
ls -la | grep V

# 假设最新版本是 V1.0.1__init_data.sql
# 新版本号应该是 V1.1.0（次版本号递增）

# 3. 创建新的迁移脚本
touch V1.1.0__add_alarm_config_table.sql
```

**步骤 2：编写 SQL**

```sql
-- V1.1.0__add_alarm_config_table.sql
CREATE TABLE IF NOT EXISTS alarm_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    alarm_name VARCHAR(100) NOT NULL,
    alarm_level VARCHAR(20) NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE alarm_config IS '告警配置表';
-- ... 其他注释

CREATE INDEX IF NOT EXISTS idx_alarm_config_tenant
    ON alarm_config(tenant_id, delete_flag);
```

**步骤 3：编写 Java 代码**

```bash
# 1. 创建 Entity 类（在对应的 service 中）
# 例如：backend/device-service/src/main/java/com/openiot/device/entity/AlarmConfig.java
```

```java
@Data
@TableName("alarm_config")
public class AlarmConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String alarmName;
    private String alarmLevel;

    @TableField(fill = FieldFill.INSERT)
    private String status;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private String deleteFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;
}
```

```bash
# 2. 创建 Mapper
# backend/device-service/src/main/java/com/openiot/device/mapper/AlarmConfigMapper.java
```

```java
@Mapper
public interface AlarmConfigMapper extends BaseMapper<AlarmConfig> {
}
```

```bash
# 3. 创建 DTO/VO 类（如果需要）
# backend/device-service/src/main/java/com/openiot/device/dto/AlarmConfigDTO.java
```

**步骤 4：测试迁移**

```bash
# 方式 1：重启 tenant-service（自动执行迁移）
cd backend/tenant-service
mvn spring-boot:run

# 方式 2：手动执行 Flyway 命令（不推荐，建议让 Spring Boot 自动执行）
mvn flyway:migrate
```

**步骤 5：验证结果**

```sql
-- 1. 查看迁移历史
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
ORDER BY installed_rank DESC
LIMIT 5;

-- 2. 验证表是否创建成功
\d alarm_config

-- 3. 验证索引
\di alarm_config*

-- 4. 验证注释
SELECT
    tablename,
    colname,
    description
FROM pg_catalog.pg_description
JOIN pg_catalog.pg_class ON pg_description.objoid = pg_class.oid
WHERE tablename = 'alarm_config';
```

---

#### 场景 2：修改表结构（新增字段）

**步骤 1：创建迁移脚本**

```bash
# 版本号：V1.1.1（修订号递增）
touch V1.1.1__add_alarm_description_field.sql
```

**步骤 2：编写 SQL**

```sql
-- V1.1.1__add_alarm_description_field.sql

-- 添加字段
ALTER TABLE alarm_config
    ADD COLUMN IF NOT EXISTS description TEXT;

-- 添加注释
COMMENT ON COLUMN alarm_config.description IS '告警描述';

-- 如果需要初始化数据
UPDATE alarm_config
SET description = '默认告警配置'
WHERE description IS NULL;
```

**步骤 3：更新 Java 代码**

```java
// AlarmConfig.java
@Data
@TableName("alarm_config")
public class AlarmConfig {
    // ... 其他字段

    private String description;  // 新增字段

    // ... 其他字段
}
```

**步骤 4：更新 DTO/VO**

```java
// AlarmConfigDTO.java
@Data
public class AlarmConfigDTO {
    // ... 其他字段

    @Schema(description = "告警描述")
    private String description;

    // ... 其他字段
}
```

**步骤 5：测试并验证**

```bash
# 重启 tenant-service
mvn spring-boot:run

# 验证字段是否添加成功
\d alarm_config
```

---

#### 场景 3：修改字段类型

**步骤 1：创建迁移脚本**

```bash
touch V1.1.2__modify_alarm_level_type.sql
```

**步骤 2：编写 SQL**

```sql
-- V1.1.2__modify_alarm_level_type.sql

-- ⚠️ 修改字段类型可能影响现有数据，需要谨慎处理

-- 方式 1：直接修改（如果数据兼容）
ALTER TABLE alarm_config
    ALTER COLUMN alarm_level TYPE VARCHAR(50);

-- 方式 2：添加临时列，迁移数据，删除旧列（如果类型不兼容）
-- Step 1: 添加临时列
ALTER TABLE alarm_config
    ADD COLUMN alarm_level_new VARCHAR(50);

-- Step 2: 迁移数据（使用 USING 子句转换）
UPDATE alarm_config
SET alarm_level_new = alarm_level::VARCHAR(50);

-- Step 3: 删除旧列
ALTER TABLE alarm_config
    DROP COLUMN alarm_level;

-- Step 4: 重命名新列
ALTER TABLE alarm_config
    RENAME COLUMN alarm_level_new TO alarm_level;

-- 更新注释
COMMENT ON COLUMN alarm_config.alarm_level IS '告警级别：INFO-信息，WARNING-警告，ERROR-错误，CRITICAL-严重';
```

**步骤 3：更新 Java 代码**

```java
// AlarmConfig.java - 字段类型可能需要调整
@Data
@TableName("alarm_config")
public class AlarmConfig {
    // ... 其他字段

    private String alarmLevel;  // 长度从 20 增加到 50

    // ... 其他字段
}
```

---

### 4.2 生产环境工作流程

#### ⚠️ 重要：生产环境迁移流程

**步骤 1：代码审查和测试**

```bash
# 1. 在开发环境完整测试
# 2. 在测试环境验证
# 3. 代码审查（Code Review）
# 4. DBA 审查 SQL 脚本（如果有 DBA）
```

**步骤 2：数据库备份（关键步骤）**

```bash
# 方式 1：使用 pg_dump（推荐）
pg_dump -h 127.0.0.1 -p 5432 -U openiot -d openiot -F c -f backup_$(date +%Y%m%d_%H%M%S).dump

# 方式 2：SQL 格式备份
pg_dump -h 127.0.0.1 -p 5432 -U openiot -d openiot > backup_$(date +%Y%m%d_%H%M%S).sql

# 方式 3：仅备份表结构
pg_dump -h 127.0.0.1 -p 5432 -U openiot -d openiot --schema-only > schema_backup.sql

# 方式 4：仅备份数据
pg_dump -h 127.0.0.1 -p 5432 -U openiot -d openiot --data-only > data_backup.sql
```

**步骤 3：执行迁移（建议低峰期）**

```bash
# 方式 1：通过应用启动自动执行（推荐）
# 停止所有服务 -> 启动 tenant-service -> 验证 -> 启动其他服务

# 方式 2：手动执行 Flyway（需要停机）
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://prod-db:5432/openiot \
                   -Dflyway.user=openiot \
                   -Dflyway.password=***

# 方式 3：蓝绿部署（零停机）
# 1. 部署新版本到新环境
# 2. 执行数据库迁移
# 3. 切换流量到新环境
```

**步骤 4：验证迁移结果**

```sql
-- 1. 检查迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- 2. 验证表结构
\d alarm_config

-- 3. 验证数据完整性
SELECT COUNT(*) FROM alarm_config;

-- 4. 验证应用功能
-- （通过应用日志、监控等验证）
```

**步骤 5：回滚计划（如果失败）**

```bash
# 如果迁移失败，需要回滚到备份版本

# 方式 1：恢复完整备份
pg_restore -h 127.0.0.1 -p 5432 -U openiot -d openiot backup_20260303_150000.dump

# 方式 2：手动回滚（需要提前编写回滚脚本）
# 创建回滚脚本：U1.1.0__rollback_alarm_config_table.sql（企业版功能）

# 方式 3：删除 flyway_schema_history 中的失败记录
DELETE FROM flyway_schema_history WHERE success = false;
-- 然后修复迁移脚本，重新执行
```

---

## 5. 代码与数据库同步策略

### 5.1 Flyway 的职责边界

| 变更类型 | Flyway 管理 | 手动维护 | AI 辅助 |
|---------|------------|---------|---------|
| **DDL（表结构）** | ✅ | ❌ | 🤖 辅助生成 SQL |
| **DML（初始数据）** | ✅ | ❌ | 🤖 辅助生成 SQL |
| **Entity 类** | ❌ | ✅ | 🤖 辅助生成代码 |
| **DTO/VO 类** | ❌ | ✅ | 🤖 辅助生成代码 |
| **Mapper 接口** | ❌ | ✅ | 🤖 辅助生成代码 |
| **业务逻辑** | ❌ | ✅ | 🤖 辅助编写 |

### 5.2 同步工作流程

#### 完整流程图

```
┌─────────────────────────────────────────────────────────────┐
│                  需求：新增告警配置功能                        │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │  1. 数据库层（Flyway 管理）            │
        │  ✅ 创建迁移脚本                       │
        │  ✅ 定义表结构、索引、约束             │
        │  ✅ 插入初始数据                       │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │  2. 持久层（手动维护 + AI 辅助）       │
        │  🤖 生成 Entity 类                     │
        │  🤖 生成 Mapper 接口                   │
        │  ✅ 手动调整和优化                     │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │  3. 业务层（手动维护 + AI 辅助）       │
        │  🤖 生成 DTO/VO 类                     │
        │  🤖 生成 Service 接口和实现            │
        │  ✅ 手动编写业务逻辑                   │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │  4. 控制层（手动维护 + AI 辅助）       │
        │  🤖 生成 Controller                    │
        │  ✅ 手动调整接口设计                   │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │  5. 测试（手动维护 + AI 辅助）         │
        │  🤖 生成单元测试                       │
        │  ✅ 手动编写集成测试                   │
        └───────────────────────────────────────┘
```

### 5.3 实战示例：新增告警配置功能

#### 步骤 1：创建 Flyway 迁移脚本（数据库层）

```sql
-- V1.1.0__add_alarm_config_table.sql
CREATE TABLE IF NOT EXISTS alarm_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    alarm_name VARCHAR(100) NOT NULL,
    alarm_level VARCHAR(20) NOT NULL,
    description TEXT,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

-- 注释、索引等...
```

#### 步骤 2：使用 AI 生成 Entity 类

**给 AI 的提示词**：
```
根据以下数据库表结构，生成 MyBatis Plus 的 Entity 类：

表名：alarm_config
字段：
- id: BIGSERIAL PRIMARY KEY
- tenant_id: BIGINT NOT NULL
- alarm_name: VARCHAR(100) NOT NULL
- alarm_level: VARCHAR(20) NOT NULL
- description: TEXT
- status: CHAR(1) DEFAULT '1'
- delete_flag: CHAR(1) DEFAULT '0'
- create_time: TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- update_time: TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- create_by: BIGINT
- update_by: BIGINT

要求：
1. 使用 Lombok 注解
2. 使用 MyBatis Plus 注解（@TableName、@TableId、@TableField 等）
3. 自动填充 create_time、update_time、create_by、update_by
4. 逻辑删除字段：delete_flag
5. 包含完整的 JavaDoc 注释
```

**AI 生成的代码**：
```java
package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警配置实体类
 *
 * @author OpenIoT Team
 * @since 1.1.0
 */
@Data
@TableName("alarm_config")
public class AlarmConfig {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 告警名称
     */
    private String alarmName;

    /**
     * 告警级别：INFO-信息，WARNING-警告，ERROR-错误，CRITICAL-严重
     */
    private String alarmLevel;

    /**
     * 告警描述
     */
    private String description;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField(fill = FieldFill.INSERT)
    private String status;

    /**
     * 删除标记：0-正常，1-已删除
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private String deleteFlag;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;
}
```

#### 步骤 3：生成 DTO/VO 类

**给 AI 的提示词**：
```
根据 AlarmConfig Entity 类，生成：
1. AlarmConfigDTO（数据传输对象，用于接收前端参数）
2. AlarmConfigVO（视图对象，用于返回给前端）

要求：
1. 使用 Lombok 注解
2. 使用 Spring Doc 注解（@Schema）
3. 添加数据校验注解（@NotNull、@NotBlank、@Size 等）
4. 排除 deleteFlag、createBy、updateBy 等内部字段
```

#### 步骤 4：生成 Mapper、Service、Controller

**Mapper**：
```java
@Mapper
public interface AlarmConfigMapper extends BaseMapper<AlarmConfig> {
    // MyBatis Plus 提供基础 CRUD，无需手写
}
```

**Service**：
```java
public interface AlarmConfigService extends IService<AlarmConfig> {
    // 自定义业务方法
}

@Service
@RequiredArgsConstructor
public class AlarmConfigServiceImpl
        extends ServiceImpl<AlarmConfigMapper, AlarmConfig>
        implements AlarmConfigService {

    // 实现业务逻辑
}
```

**Controller**：
```java
@RestController
@RequestMapping("/api/alarm-configs")
@RequiredArgsConstructor
public class AlarmConfigController {

    private final AlarmConfigService alarmConfigService;

    @PostMapping
    public ApiResponse<AlarmConfigVO> create(@RequestBody @Valid AlarmConfigDTO dto) {
        // 实现创建逻辑
    }

    @GetMapping("/{id}")
    public ApiResponse<AlarmConfigVO> getById(@PathVariable Long id) {
        // 实现查询逻辑
    }

    // 其他接口...
}
```

### 5.4 字段更新策略

#### 场景 1：数据库新增字段

```bash
# 1. 创建 Flyway 迁移脚本
V1.1.1__add_alarm_threshold_field.sql
```

```sql
ALTER TABLE alarm_config
    ADD COLUMN IF NOT EXISTS threshold DECIMAL(10, 2);

COMMENT ON COLUMN alarm_config.threshold IS '告警阈值';
```

```bash
# 2. 更新 Entity 类（手动或 AI 辅助）
```

```java
@Data
@TableName("alarm_config")
public class AlarmConfig {
    // ... 其他字段

    /**
     * 告警阈值
     */
    private BigDecimal threshold;  // 新增字段

    // ... 其他字段
}
```

```bash
# 3. 更新 DTO/VO 类（手动或 AI 辅助）
```

```java
@Data
public class AlarmConfigDTO {
    // ... 其他字段

    @Schema(description = "告警阈值")
    @DecimalMin(value = "0.0", message = "告警阈值不能小于0")
    private BigDecimal threshold;

    // ... 其他字段
}
```

#### 场景 2：Entity 新增字段（反向同步）

**注意**：不推荐先修改代码，再同步数据库。建议先修改数据库，再更新代码。

如果必须反向同步：

```bash
# 1. 先修改 Entity 类
```

```java
@Data
@TableName("alarm_config")
public class AlarmConfig {
    // ... 其他字段

    private Integer notifyCount;  // 新增字段

    // ... 其他字段
}
```

```bash
# 2. 创建 Flyway 迁移脚本
V1.1.2__add_alarm_notify_count_field.sql
```

```sql
ALTER TABLE alarm_config
    ADD COLUMN IF NOT EXISTS notify_count INTEGER DEFAULT 0;

COMMENT ON COLUMN alarm_config.notify_count IS '通知次数';
```

---

## 6. 数据备份策略

### 6.1 备份时机

| 时机 | 备份类型 | 保留时长 | 存储位置 |
|------|---------|---------|---------|
| **每次迁移前** | 完整备份 | 7 天 | 本地 + 云存储 |
| **每日定时备份** | 完整备份 | 30 天 | 云存储 |
| **每周定时备份** | 完整备份 | 90 天 | 云存储 |
| **每月定时备份** | 完整备份 | 1 年 | 冷存储 |

### 6.2 备份脚本

#### 开发环境备份脚本

```bash
#!/bin/bash
# backup_dev_database.sh

# 配置
DB_HOST="127.0.0.1"
DB_PORT="5432"
DB_NAME="openiot"
DB_USER="openiot"
DB_PASSWORD="openiot123"
BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
echo "开始备份数据库: $DATE"
pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -F c \
    -f "$BACKUP_DIR/backup_$DATE.dump"

# 检查备份是否成功
if [ $? -eq 0 ]; then
    echo "✅ 备份成功: $BACKUP_DIR/backup_$DATE.dump"

    # 删除 7 天前的备份
    find $BACKUP_DIR -name "backup_*.dump" -mtime +7 -delete
    echo "✅ 已清理 7 天前的备份"
else
    echo "❌ 备份失败"
    exit 1
fi
```

#### 生产环境备份脚本

```bash
#!/bin/bash
# backup_prod_database.sh

# 配置（从环境变量读取）
DB_HOST=${DB_HOST:-"prod-db.example.com"}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-"openiot"}
DB_USER=${DB_USER:-"openiot"}
DB_PASSWORD=${DB_PASSWORD}
BACKUP_DIR="/data/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
S3_BUCKET="s3://your-bucket/postgresql-backups"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 开始备份生产数据库..."
PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER \
    -d $DB_NAME -F c -f "$BACKUP_DIR/backup_$DATE.dump"

# 检查备份是否成功
if [ $? -eq 0 ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ 备份成功"

    # 上传到 S3
    aws s3 cp "$BACKUP_DIR/backup_$DATE.dump" "$S3_BUCKET/backup_$DATE.dump"
    if [ $? -eq 0 ]; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ 已上传到 S3"
    else
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ⚠️ S3 上传失败"
    fi

    # 删除本地 7 天前的备份
    find $BACKUP_DIR -name "backup_*.dump" -mtime +7 -delete
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ 已清理本地旧备份"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ 备份失败"
    # 发送告警（需要配置告警系统）
    # curl -X POST "https://your-alert-webhook" -d "数据库备份失败"
    exit 1
fi
```

#### 定时备份（Crontab）

```bash
# 编辑 crontab
crontab -e

# 添加定时任务
# 每天凌晨 2 点备份
0 2 * * * /path/to/backup_prod_database.sh >> /var/log/db_backup.log 2>&1

# 每周日凌晨 3 点备份
0 3 * * 0 /path/to/backup_prod_database.sh >> /var/log/db_backup.log 2>&1
```

### 6.3 恢复数据

```bash
# 恢复完整备份
pg_restore -h 127.0.0.1 -p 5432 -U openiot -d openiot backup_20260303_150000.dump

# 恢复 SQL 格式备份
psql -h 127.0.0.1 -p 5432 -U openiot -d openiot < backup_20260303_150000.sql

# 恢复到新数据库（避免覆盖现有数据）
createdb -h 127.0.0.1 -p 5432 -U openiot openiot_restore
pg_restore -h 127.0.0.1 -p 5432 -U openiot -d openiot_restore backup_20260303_150000.dump
```

---

## 7. 常见场景实践

### 7.1 场景 1：新建业务表

**完整流程**：

```bash
# 1. 创建迁移脚本
cd backend/tenant-service/src/main/resources/db/migration
touch V1.2.0__add_product_table.sql

# 2. 编写 SQL（参考第 3.2 节）

# 3. 备份数据库（生产环境）
./backup_prod_database.sh

# 4. 重启 tenant-service
cd backend/tenant-service
mvn spring-boot:run

# 5. 验证迁移结果
psql -h 127.0.0.1 -U openiot -d openiot -c "\d product"

# 6. 使用 AI 生成代码
# 参考 5.3 节的提示词

# 7. 编写单元测试和集成测试

# 8. 提交代码和迁移脚本
git add .
git commit -m "feat: 添加产品管理功能"
git push
```

### 7.2 场景 2：添加索引（性能优化）

```sql
-- V1.1.3__add_index_for_device_query.sql

-- 为常用查询添加复合索引
CREATE INDEX IF NOT EXISTS idx_device_tenant_status_delete
    ON device(tenant_id, status, delete_flag);

-- 为时间范围查询添加索引
CREATE INDEX IF NOT EXISTS idx_device_create_time
    ON device(create_time DESC);

-- 为文本搜索添加 GIN 索引
CREATE INDEX IF NOT EXISTS idx_device_name_gin
    ON device USING gin(to_tsvector('simple', device_name));
```

**注意**：
- ✅ 在低峰期执行（索引创建可能耗时）
- ✅ 使用 `CONCURRENTLY` 选项（PostgreSQL，避免锁表）
- ✅ 监控数据库性能

```sql
-- 推荐写法（避免锁表）
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_device_tenant_status
    ON device(tenant_id, status);
```

### 7.3 场景 3：数据迁移（批量更新）

```sql
-- V1.1.4__migrate_device_status_data.sql

-- ⚠️ 大批量数据迁移可能耗时，建议分批处理

-- 方式 1：直接更新（小批量）
UPDATE device
SET status = '0'
WHERE last_online_time < NOW() - INTERVAL '30 days'
  AND status = '1'
  AND delete_flag = '0';

-- 方式 2：分批更新（大批量）
DO $$
DECLARE
    batch_size INTEGER := 1000;
    affected_rows INTEGER;
BEGIN
    LOOP
        UPDATE device
        SET status = '0'
        WHERE id IN (
            SELECT id FROM device
            WHERE last_online_time < NOW() - INTERVAL '30 days'
              AND status = '1'
              AND delete_flag = '0'
            LIMIT batch_size
        );

        GET DIAGNOSTICS affected_rows = ROW_COUNT;
        EXIT WHEN affected_rows = 0;

        COMMIT;
        RAISE NOTICE '已更新 % 行', affected_rows;
    END LOOP;
END $$;
```

### 7.4 场景 4：删除字段（谨慎操作）

```sql
-- V1.1.5__remove_device_legacy_field.sql

-- ⚠️ 删除字段是不可逆操作，确保：
-- 1. 已备份
-- 2. 代码中不再使用该字段
-- 3. 业务低峰期执行

-- Step 1: 检查字段是否被使用
SELECT COUNT(*) FROM device WHERE legacy_field IS NOT NULL;

-- Step 2: 如果有数据，先迁移或备份
-- CREATE TABLE device_legacy_backup AS SELECT id, legacy_field FROM device;

-- Step 3: 删除字段
ALTER TABLE device DROP COLUMN IF EXISTS legacy_field;
```

---

## 8. 故障处理

### 8.1 常见错误

#### 错误 1：Checksum 不匹配

**错误信息**：
```
Flyway validation error: CRC mismatch for migration V1.1.0
```

**原因**：已执行的迁移脚本被修改

**解决方案**：

```bash
# 方式 1：恢复原始脚本（推荐）
git checkout V1.1.0__add_alarm_config_table.sql

# 方式 2：使用 repair 修复（不推荐，可能掩盖问题）
mvn flyway:repair

# 方式 3：手动修复 flyway_schema_history
DELETE FROM flyway_schema_history WHERE version = '1.1.0';
# 然后重新执行迁移
```

#### 错误 2：版本号冲突

**错误信息**：
```
Found non-empty schema without schema history table
```

**解决方案**：

```bash
# 使用 baseline 初始化
mvn flyway:baseline -Dflyway.baselineVersion=1.0.0
mvn flyway:migrate
```

#### 错误 3：迁移失败

**错误信息**：
```
Migration V1.1.0 failed
```

**解决方案**：

```sql
-- 1. 查看失败记录
SELECT * FROM flyway_schema_history WHERE success = false;

-- 2. 手动修复数据库（如果需要）
-- 执行回滚 SQL

-- 3. 删除失败记录
DELETE FROM flyway_schema_history WHERE version = '1.1.0' AND success = false;

-- 4. 修复迁移脚本

-- 5. 重新执行迁移
```

### 8.2 回滚策略

#### 方式 1：恢复完整备份

```bash
# 1. 停止所有服务

# 2. 恢复数据库
pg_restore -h 127.0.0.1 -p 5432 -U openiot -d openiot backup_20260303_150000.dump

# 3. 回滚代码
git revert <commit-hash>

# 4. 重启服务
```

#### 方式 2：创建反向迁移脚本

```sql
-- U1.1.0__rollback_alarm_config_table.sql（企业版功能）

-- 或手动创建回滚脚本
DROP TABLE IF EXISTS alarm_config;
```

---

## 9. 最佳实践总结

### 9.1 Do's（推荐）

✅ **每次迁移前备份数据库**
✅ **使用版本控制管理迁移脚本**
✅ **脚本命名清晰，版本号递增**
✅ **添加完整的注释和索引**
✅ **在开发/测试环境充分测试**
✅ **生产环境在低峰期执行迁移**
✅ **保留迁移历史，不删除已执行的脚本**
✅ **使用 `IF NOT EXISTS`、`IF EXISTS` 避免重复执行错误**
✅ **大数据量迁移分批处理**
✅ **监控迁移执行时间和性能**

### 9.2 Don'ts（禁止）

❌ **修改已执行的迁移脚本**
❌ **删除 `flyway_schema_history` 表中的记录**
❌ **在生产环境使用 `flyway:clean`**
❌ **在代码中直接执行 DDL**
❌ **跳过版本号（如从 V1.0.0 直接到 V1.0.2）**
❌ **在多个服务中启用 Flyway（本项目）**
❌ **迁移脚本中硬编码敏感信息**
❌ **在业务高峰期执行迁移**
❌ **忘记备份数据库**
❌ **不测试就直接在生产环境执行**

### 9.3 检查清单

**迁移前检查**：
- [ ] 迁移脚本版本号正确且递增
- [ ] 迁移脚本已通过代码审查
- [ ] 在开发环境测试通过
- [ ] 在测试环境验证通过
- [ ] 已备份数据库
- [ ] 已准备好回滚方案

**迁移后检查**：
- [ ] `flyway_schema_history` 表中有新记录
- [ ] 表结构/数据符合预期
- [ ] 应用启动正常
- [ ] 业务功能正常
- [ ] 性能指标正常

---

## 10. 参考资料

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [PostgreSQL 官方文档](https://www.postgresql.org/docs/)
- [MyBatis Plus 官方文档](https://baomidou.com/)
- [项目 CLAUDE.md - 数据库迁移规范](../CLAUDE.md)

---

**文档版本**：v1.0.0
**最后更新**：2026-03-03
**维护者**：OpenIoT Team

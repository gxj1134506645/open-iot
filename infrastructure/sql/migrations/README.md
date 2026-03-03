# SQL Migrations（已废弃）

> ⚠️ **架构变更说明**
>
> 本项目已采用**微服务独立数据库架构**，迁移脚本已迁移到各服务内部。
>
> - ❌ 此目录**不再使用**
> - ✅ 迁移脚本已移动到各服务的 `src/main/resources/db/migration/` 目录

## 当前架构（独立数据库）

### 数据库列表

| 服务 | 数据库 | 迁移脚本位置 |
|------|--------|-------------|
| tenant-service | `openiot_tenant` | `backend/tenant-service/src/main/resources/db/migration/` |
| device-service | `openiot_device` | `backend/device-service/src/main/resources/db/migration/` |
| data-service | `openiot_data` | `backend/data-service/src/main/resources/db/migration/` |
| connect-service | `openiot_connect` | `backend/connect-service/src/main/resources/db/migration/` |

### 迁移脚本组织

```
backend/
├── tenant-service/src/main/resources/db/migration/
│   ├── V1.0.0__init_tenant_schema.sql
│   ├── V1.0.1__init_tenant_data.sql
│   └── V1.1.0__rbac_tables.sql
│
├── device-service/src/main/resources/db/migration/
│   └── V1.0.0__init_device_schema.sql
│
├── data-service/src/main/resources/db/migration/
│   └── V1.0.0__init_data_schema.sql
│
└── connect-service/src/main/resources/db/migration/
    └── V1.0.0__init_connect_schema.sql
```

### 命名规范

- `V{version}__{description}.sql`
- 版本号格式: `1.0.0`, `1.0.1`, `1.1.0` 等
- 描述使用下划线分隔单词
- **每个服务独立管理版本号**（可以重复）

### 执行顺序

每个服务启动时，Flyway 自动执行各自的迁移脚本：

1. **tenant-service**
   - `V1.0.0__init_tenant_schema.sql` - 租户、用户表
   - `V1.0.1__init_tenant_data.sql` - 初始数据
   - `V1.1.0__rbac_tables.sql` - RBAC 权限表

2. **device-service**
   - `V1.0.0__init_device_schema.sql` - 设备表

3. **data-service**
   - `V1.0.0__init_data_schema.sql` - 设备轨迹表

4. **connect-service**
   - `V1.0.0__init_connect_schema.sql` - 设备会话、连接日志表

## 旧架构（已废弃）

### 共享数据库架构

之前的架构使用单一共享数据库 `openiot`，所有迁移脚本集中在此目录：

```
infrastructure/sql/migrations/
├── V1.0.0__init_schema.sql          # 包含所有表
├── V1.0.1__init_data.sql            # 初始数据
└── V1.1.0__rbac_tables.sql          # RBAC 表
```

**废弃原因**：
- ❌ 不符合微服务架构原则
- ❌ 服务耦合度高
- ❌ 数据隔离性差
- ❌ 无法独立部署和扩展

## 参考资料

- [数据库架构说明](../../backend/docs/数据库架构说明.md)
- [Flyway 实践指南](../../backend/docs/flyway-实践指南.md)
- [Flyway 迁移脚本组织方案对比](../../backend/docs/flyway-迁移脚本组织方案对比.md)

---

**最后更新**：2026-03-03
**维护者**：OpenIoT Team

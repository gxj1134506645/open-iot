# SQL Migrations

此目录存放 Flyway 数据库迁移脚本。

## 命名规范

- `V{version}__{description}.sql`
- 版本号格式: `1.0.0`, `1.0.1`, `1.1.0` 等
- 描述使用下划线分隔单词

## 执行顺序

1. `V1.0.0__init_schema.sql` - 初始化数据库表结构
2. `V1.0.1__init_data.sql` - 初始化基础数据

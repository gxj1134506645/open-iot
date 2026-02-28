# open-iot Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-25

## Active Technologies

### 后端
- JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.x, Spring Cloud Alibaba, Kafka, Netty, MyBatis Plus, Sa-Token (001-mvp-core)

### 前端
- Vue 3 + Vite + Element Plus
- UI设计工具：UI/UX Pro Max skill（已安装）
- 状态管理：Pinia

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

### 后端命令
# Add commands for JDK 21 (LTS，支持虚拟线程)

### 前端命令
# UI设计：使用UI/UX Pro Max skill进行前端界面设计和代码生成
# 启动前端开发服务器：cd frontend && npm run dev

## Code Style

JDK 21 (LTS，支持虚拟线程): Follow standard conventions

## Recent Changes

- 001-mvp-core: Added JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.x, Spring Cloud Alibaba, Kafka, Netty, MyBatis Plus, Sa-Token

<!-- MANUAL ADDITIONS START -->
## Git 提交规范

### 提交信息格式

所有 Git 提交信息 MUST 使用**中文简体**，遵循以下格式：

```
<类型>: <简短描述>

<详细说明>（可选）
```

### 提交类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | feat: 添加设备管理模块 |
| `fix` | Bug 修复 | fix: 修复租户登录失败问题 |
| `docs` | 文档更新 | docs: 更新 API 接口文档 |
| `refactor` | 代码重构 | refactor: 优化设备查询性能 |
| `test` | 测试相关 | test: 添加设备服务单元测试 |
| `chore` | 构建/工具变更 | chore: 更新依赖版本 |
| `style` | 代码格式调整 | style: 格式化代码缩进 |
| `perf` | 性能优化 | perf: 优化数据库查询性能 |

### 提交规范要求

- ✅ **必须使用中文简体**描述提交信息
- ✅ 标题行不超过 50 个字符
- ✅ 使用祈使语气（"添加"而非"添加了"）
- ✅ 提交内容应聚焦单一改动
- ✅ 复杂改动需添加详细说明
- ❌ 禁止使用英文提交信息
- ❌ 禁止一次性提交过多不相关改动

### 示例

**好的提交：**
```
feat: 添加设备管理列表页面

- 实现设备列表查询功能
- 添加设备新增、编辑、删除操作
- 集成 Element Plus 表格组件
```

**不好的提交：**
```
update code
fix bug
修改了一些东西
```
<!-- MANUAL ADDITIONS END -->

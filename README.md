# Open-IoT 开放物联网平台

[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=http://jenkins.example.com/job/open-iot)](https://jenkins.example.com/job/open-iot)
[![Docker Build](https://img.shields.io/badge/docker-ready-blue)](https://hub.docker.com/r/example/open-iot)
[![License](https://img.shields.io/github/license/gxj1134506645/open-iot)](LICENSE)

**Open-IoT** 是一个学习型开源物联网平台，旨在系统化学习 IoT 后端核心能力。

## 🎯 核心能力

- ✅ **微服务治理**: Nacos 注册中心 + 配置中心 + Spring Cloud Gateway 网关
- ✅ **Kafka 消息驱动**: 实时 + 异步双通道数据流
- ✅ **Netty 高并发接入**: TCP 私有协议 + MQTT (EMQX) + HTTP 多协议支持
- ✅ **多租户数据库设计**: 全链路租户隔离
- ✅ **严格 RBAC 权限模型**: 用户-角色-权限动态配置
- ✅ **分布式能力**: Redisson 分布式锁 + Seata AT 分布式事务
- ✅ **可观测性**: LGTM Stack (Grafana + Prometheus + Loki + Tempo) 全链路监控

## 🏗️ 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                            │
│              设备监控 │ 轨迹展示 │ 租户管理                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                 API Gateway (Spring Cloud Gateway)           │
└──────────────────────────┬──────────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
    ▼                      ▼                      ▼
┌──────────┐         ┌──────────┐         ┌──────────┐
│ device-  │         │  data-   │         │ tenant-  │
│ service  │         │ service  │         │ service  │
└──────────┘         └──────────┘         └──────────┘

┌─────────────────────────────────────────────────────────────┐
│                        设备接入层                            │
│   EMQX (MQTT)    │    connect-service (Netty TCP)    │ HTTP  │
└─────────────────────────────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │    Kafka    │
                    └──────┬──────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    ▼                      ▼                      ▼
┌──────────┐         ┌──────────┐         ┌──────────┐
│PostgreSQL│         │  Redis   │         │ MongoDB  │
└──────────┘         └──────────┘         └──────────┘
```

## 🚀 快速开始

### 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| Docker | 24+ |
| Docker Compose | 2.20+ |

### 1. 克隆项目

```bash
git clone https://github.com/gxj1134506645/open-iot.git
cd open-iot
```

### 2. 启动基础设施

```bash
# 仅启动基础设施（Nacos、PostgreSQL、Redis、MongoDB、Kafka、EMQX）
./scripts/deploy.sh --infra

# 或手动启动
cd infrastructure/docker
docker-compose up -d
```

等待所有服务启动（约 2-3 分钟）。

### 3. 初始化数据库

数据库表会在首次启动时通过 Flyway 自动创建。如需手动执行：

```bash
# 连接 PostgreSQL
psql -h localhost -U openiot -d openiot -f infrastructure/sql/migrations/*.sql
```

### 4. 构建项目

```bash
# 构建全部
./scripts/build.sh

# 仅构建后端
./scripts/build.sh -b

# 仅构建前端
./scripts/build.sh -f

# 跳过测试构建
./scripts/build.sh -s
```

### 5. 启动后端服务

```bash
cd backend

# 启动各服务（按顺序）
java -jar gateway-service/target/gateway-service.jar &
java -jar tenant-service/target/tenant-service.jar &
java -jar device-service/target/device-service.jar &
java -jar connect-service/target/connect-service.jar &
java -jar data-service/target/data-service.jar &
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 7. 访问服务

| 服务 | 地址 | 账号 |
|------|------|------|
| 前端 | http://localhost:5173 | - |
| API 网关 | http://localhost:8080 | - |
| Nacos | http://localhost:8848/nacos | nacos/nacos |
| EMQX | http://localhost:18083 | admin/public |
| Kafka UI | http://localhost:9000 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Alertmanager | http://localhost:9093 | - |

### 常用命令

```bash
# 查看服务日志
./scripts/deploy.sh --logs

# 停止服务
./scripts/deploy.sh --stop

# 查看服务状态
docker-compose ps
```

## 📦 项目结构

```
open-iot/
├── backend/                    # 后端服务
│   ├── common/                 # 公共模块
│   │   ├── common-core/        # 核心工具类
│   │   ├── common-redis/       # Redis 配置 + Redisson 分布式锁
│   │   ├── common-kafka/       # Kafka 配置
│   │   ├── common-mongodb/     # MongoDB 配置
│   │   ├── common-security/    # Sa-Token 安全认证
│   │   └── common-observability/ # 可观测性 (Micrometer + OTel)
│   ├── gateway-service/        # API 网关 (8080)
│   ├── tenant-service/         # 租户管理 (8081)
│   ├── device-service/         # 设备管理 (8082)
│   ├── connect-service/        # 设备接入 - Netty TCP (8083)
│   └── data-service/           # 数据处理 (8085)
├── frontend/                   # 前端 (Vue 3 + Vite + Element Plus)
├── infrastructure/             # 基础设施
│   ├── docker/                 # Docker Compose 配置
│   ├── emqx/                   # EMQX 配置
│   ├── kafka/                  # Kafka 配置
│   └── sql/                    # 数据库迁移脚本 (Flyway)
├── scripts/                    # 构建部署脚本
│   ├── build.sh                # 构建脚本
│   └── deploy.sh               # 部署脚本
├── specs/                      # 功能规格文档
│   └── 001-mvp-core/           # MVP 核心功能规格
├── .specify/                   # 项目宪法和模板
├── Jenkinsfile                 # CI/CD 流水线
└── README.md
```

## 🔐 权限模型

采用严格 RBAC（Role-Based Access Control）模型：

```
用户(sys_user) ←→ 角色(sys_role) ←→ 权限(sys_permission)
        │                    │
    sys_user_role       sys_role_permission
```

**预置角色：**

| 角色 | 权限范围 |
|------|----------|
| ADMIN | 全部权限（平台级） |
| TENANT_ADMIN | 用户、设备、数据、监控（租户级） |
| TENANT_USER | 设备、数据、监控（仅查看） |

**前端权限控制：**

```vue
<!-- 按钮级权限 -->
<el-button v-permission="'device:create'">创建设备</el-button>

<!-- 角色控制 -->
<div v-role="'ADMIN'">仅管理员可见</div>
```

## 🔧 CI/CD

### Jenkins 流水线

项目使用 Jenkins + Docker 实现 CI/CD：

1. **代码提交** → GitHub Webhook 触发 Jenkins
2. **构建** → Maven 构建后端，npm 构建前端
3. **测试** → 运行单元测试
4. **镜像构建** → 根据 Dockerfile 构建镜像
5. **部署** → 推送镜像，docker-compose 部署

### Jenkins 配置

1. 安装插件: Docker Pipeline, Kubernetes (可选)
2. 添加凭证: `docker-registry` (镜像仓库登录)
3. 创建流水线任务，指向 Jenkinsfile

## 📊 可观测性 (Observability)

Open-IoT 采用 **LGTM Stack** 实现全链路可观测性：

### 架构组件

| 组件 | 用途 | 端口 |
|------|------|------|
| Prometheus | 指标采集和存储 | 9090 |
| Loki | 日志聚合 | 3100 |
| Tempo | 分布式链路追踪 | 4317 (OTLP) |
| Grafana | 统一可视化 | 3000 |
| Alertmanager | 告警管理 | 9093 |

### 启动可观测性服务

```bash
# 启动 LGTM Stack
cd infrastructure/docker
docker-compose -f docker-compose.observability.yml up -d

# 访问 Grafana
open http://localhost:3000
# 默认账号: admin/admin
```

### 预置仪表盘

- **服务概览**: 所有微服务运行状态、请求量、错误率
- **JVM 详情**: 堆内存、GC、线程状态
- **业务指标**: 设备在线数、消息吞吐量、消费延迟

### 告警规则

内置告警规则覆盖：

- 🔴 服务宕机 (1 分钟内告警)
- 🟡 5xx 错误率 > 1%
- 🟡 P95 延迟 > 3s
- 🔴 JVM 堆内存 > 85%
- 🔴 数据库连接池即将耗尽
- 🟡 Kafka 消费积压 > 10000
- 🟡 设备离线率 > 20%

### 业务指标

```java
// 设备指标
openiot_device_total_count          // 设备总数
openiot_device_online_count         // 在线设备数

// 消息指标
openiot_message_received_total      // 接收消息数
openiot_message_processed_total     // 处理成功数
openiot_message_failed_total        // 处理失败数
openiot_message_processing_seconds  // 处理延迟

// Kafka 指标
kafka_consumer_group_lag            // 消费积压
kafka_consumer_group_members        // 消费者成员数
```

### 链路追踪

使用 OpenTelemetry + W3C Trace Context 标准：

```bash
# 在日志中获取 Trace ID
curl -H "X-Trace-Id: custom-trace-123" http://localhost:8080/api/devices

# 在 Grafana Tempo 中查询
# 通过 Trace ID 查看完整调用链
```

## 📚 文档

- [功能规格](./specs/001-mvp-core/spec.md)
- [实施计划](./specs/001-mvp-core/plan.md)
- [API 契约](./specs/001-mvp-core/contracts/api.yaml)
- [事件契约](./specs/001-mvp-core/contracts/events.yaml)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

[MIT License](LICENSE)

---

**Version**: 1.0.0-SNAPSHOT | **Author**: [gxj1134506645](https://github.com/gxj1134506645)

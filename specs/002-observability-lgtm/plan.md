# Implementation Plan: 可观测性系统 (LGTM Stack)

**Branch**: `002-observability-lgtm` | **Date**: 2026-03-01 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-observability-lgtm/spec.md`

## Summary

实现基于 LGTM Stack (Grafana + Prometheus + Loki + Tempo) 的完整可观测性系统，为 OpenIoT 平台提供 Metrics、Logs、Traces 三支柱监控能力，并配置生产级告警规则。

## Technical Context

**Language/Version**: Java 21 (LTS), Spring Boot 3.x
**Primary Dependencies**: Micrometer, OpenTelemetry, Logback, Spring Actuator
**Storage**: Prometheus (Metrics), Loki (Logs), Tempo (Traces)
**Testing**: 手动验证 + Grafana 仪表盘测试
**Target Platform**: Docker Compose / Kubernetes
**Project Type**: 微服务后端 + 基础设施
**Performance Goals**: 指标采集开销 < 5% CPU/内存，仪表盘加载 < 3s
**Constraints**: 无数据库变更，仅基础设施和应用配置
**Scale/Scope**: 5 个微服务 + 1 个网关

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原则 | 状态 | 说明 |
|------|------|------|
| I. 多租户强制原则 | ✅ 通过 | 日志包含 tenantId，支持按租户查询 |
| II. 数据演进安全原则 | ✅ 通过 | 无数据库变更 |
| III. 渐进式微服务实践原则 | ✅ 通过 | 增强可观测性，符合"可观测性才是目标" |
| IV. 协议可扩展原则 | ✅ 通过 | 不涉及设备协议 |
| V. 垂直数据链路优先原则 | ✅ 通过 | 可观测性支持数据链路监控 |
| VI. 学习范围强制原则 | ✅ 通过 | 属于微服务治理必要能力 |
| VII. 认证边界与职责划分原则 | ✅ 通过 | 不涉及认证变更 |
| VIII. 严格 RBAC 权限模型原则 | ✅ 通过 | 不涉及权限变更 |
| IX. 可观测性强制原则 | ✅ 目标 | 本功能即实现此原则 |

**Gate 结果**: ✅ 全部通过，无需例外申请

## Project Structure

### Documentation (this feature)

```text
specs/002-observability-lgtm/
├── spec.md              # 功能规格
├── plan.md              # 本文件
├── research.md          # 技术研究
├── data-model.md        # 数据模型
├── quickstart.md        # 快速启动指南
└── tasks.md             # 任务列表 (/speckit.tasks 生成)
```

### Source Code (repository root)

```text
backend/
├── common/
│   └── common-core/
│       └── src/main/java/com/openiot/common/core/
│           └── config/
│               └── ObservabilityConfig.java    # 可观测性配置
│           └── metrics/
│               └── BusinessMetrics.java        # 业务指标
├── gateway-service/
│   └── src/main/resources/
│       └── logback-spring.xml                  # 日志配置
├── tenant-service/
│   └── src/main/resources/
│       └── logback-spring.xml
├── device-service/
│   └── src/main/resources/
│       └── logback-spring.xml
├── data-service/
│   └── src/main/resources/
│       └── logback-spring.xml
├── connect-service/
│   └── src/main/resources/
│       └── logback-spring.xml
└── pom.xml                                    # 父 POM 依赖管理

infrastructure/
├── docker/
│   ├── docker-compose.yml                     # 现有配置
│   ├── docker-compose.observability.yml       # 可观测性服务
│   ├── prometheus/
│   │   └── prometheus.yml                     # Prometheus 配置
│   ├── loki/
│   │   └── loki-config.yaml                   # Loki 配置
│   ├── tempo/
│   │   └── tempo.yaml                         # Tempo 配置
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/
│       │   │   └── datasources.yml            # 数据源配置
│       │   └── dashboards/
│       │       ├── dashboard.yml              # 仪表盘配置
│       │       ├── service-overview.json      # 服务概览仪表盘
│       │       ├── jvm-details.json           # JVM 详情仪表盘
│       │       └── business-metrics.json      # 业务指标仪表盘
│       └── alerting/
│           └── alert-rules.yml                # 告警规则
└── ...
```

**Structure Decision**: 在现有微服务结构上添加可观测性配置，不新增服务模块。基础设施配置放在 `infrastructure/docker/` 目录下。

## Complexity Tracking

> 无 Constitution 违规，无需记录。

## Phase 0: Research Summary

详细研究结果见 [research.md](./research.md)

### 关键决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| Metrics 采集 | Micrometer + Prometheus | Spring Boot 原生支持，云原生标准 |
| Logs 采集 | Logback + Loki | 资源消耗低，与 Grafana 原生集成 |
| Traces 采集 | Micrometer Tracing + Tempo | CNCF 标准，存储成本低 |
| 可视化 | Grafana OSS | 统一界面，开源免费 |
| 告警 | Grafana Alerting | 原生集成，功能完整 |

## Phase 1: Design Summary

### 数据模型

详细数据模型见 [data-model.md](./data-model.md)

- **Metrics**: 时序指标，通过 Prometheus 存储和查询
- **Logs**: JSON 结构化日志，通过 Loki 存储和查询
- **Traces**: 分布式链路，通过 Tempo 存储和查询

### 契约定义

本功能为基础设施增强，不涉及 API 契约变更。主要契约包括：

1. **指标端点**: 所有服务暴露 `/actuator/prometheus`
2. **日志格式**: 统一 JSON 格式，包含 `traceId`、`tenantId`、`service` 字段
3. **健康检查**: 所有服务暴露 `/actuator/health/liveness` 和 `/actuator/health/readiness`

### 快速启动

详细指南见 [quickstart.md](./quickstart.md)

## Implementation Phases

### Phase 2: 基础设施配置

1. 添加 Docker Compose 可观测性服务配置
2. 配置 Prometheus 抓取规则
3. 配置 Loki 日志采集
4. 配置 Tempo 链路存储
5. 配置 Grafana 数据源和仪表盘

### Phase 3: 应用集成

1. 父 POM 添加可观测性依赖管理
2. 各服务添加 Micrometer + Prometheus 依赖
3. 各服务添加 OpenTelemetry 依赖
4. 各服务配置 Logback JSON 日志
5. 各服务配置 application.yml 可观测性参数
6. 创建业务指标收集组件

### Phase 4: 仪表盘和告警

1. 创建服务概览仪表盘
2. 创建 JVM 详情仪表盘
3. 创建业务指标仪表盘
4. 配置告警规则
5. 配置告警通知渠道

### Phase 5: 验证和文档

1. 端到端功能验证
2. 性能开销验证
3. 更新项目文档

## Dependencies

### 外部依赖

| 组件 | 版本 | 用途 |
|------|------|------|
| Prometheus | v2.48.0 | 指标存储和查询 |
| Loki | 2.9.0 | 日志存储和查询 |
| Tempo | 2.3.0 | 链路存储和查询 |
| Grafana | 10.2.0 | 可视化平台 |

### 内部依赖

| 依赖 | 说明 |
|------|------|
| Spring Boot Actuator | 健康检查和指标暴露 |
| Micrometer | 指标门面 |
| OpenTelemetry | 链路追踪标准 |
| Logback | 日志框架 |

## Risks & Mitigations

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 高 cardinality 指标导致 Prometheus 性能下降 | 中 | 高 | 控制标签基数，避免无界标签（如 deviceId） |
| 日志量过大导致 Loki 存储压力 | 中 | 中 | 配置日志级别，生产环境 DEBUG 关闭 |
| Trace 采样率过低影响排查 | 低 | 中 | 开发环境 100% 采样，生产环境 10% 采样 |
| 告警风暴导致通知疲劳 | 中 | 中 | 配置告警分组和静默规则 |
| Docker 资源不足导致服务启动失败 | 中 | 高 | 建议 16GB 内存，提供资源调优指南 |

## Success Metrics

| 指标 | 目标 | 验证方式 |
|------|------|----------|
| 指标采集延迟 | < 15s | Prometheus 抓取间隔 |
| 日志查询响应 | < 1s | Grafana Explore 测试 |
| 链路查询响应 | < 2s | Grafana Explore 测试 |
| 仪表盘加载 | < 3s | 浏览器加载时间 |
| 可观测性开销 | < 5% CPU/内存 | 资源监控对比 |
| 告警触发时间 | < 1min | 模拟故障测试 |

## Next Steps

运行 `/speckit.tasks` 生成详细任务列表。

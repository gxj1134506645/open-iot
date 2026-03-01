# Research: 可观测性系统 (LGTM Stack)

**Feature**: 002-observability-lgtm
**Date**: 2026-03-01
**Purpose**: 解决技术选型和最佳实践问题

## 技术决策

### 1. Metrics 采集方案

**Decision**: Micrometer + Prometheus

**Rationale**:
- Spring Boot 3.x 原生支持 Micrometer，无需额外集成成本
- Micrometer 作为指标门面（类似 SLF4J），可无缝切换后端
- Prometheus 是云原生事实标准，社区活跃，文档丰富
- 与 Grafana 原生集成，支持 PromQL 查询

**Alternatives Considered**:
| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| Dropwizard Metrics | 成熟稳定 | 不支持 Prometheus 格式 | 排除 |
| OpenTelemetry Metrics | 统一标准 | 生态不如 Micrometer 成熟 | 备选 |
| 直接暴露 Prometheus | 简单 | 无门面抽象，切换困难 | 排除 |

**Implementation**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

### 2. Logs 采集方案

**Decision**: Logback + Logstash Logback Encoder → Loki

**Rationale**:
- 项目已使用 SLF4J + Logback，只需添加 JSON encoder
- logstash-logback-encoder 提供完整的结构化日志支持
- Loki 与 Grafana 原生集成，支持 LogQL
- 相比 ELK，资源消耗低 10 倍以上

**Alternatives Considered**:
| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| ELK Stack | 功能强大 | 资源消耗大（ES 吃内存） | 排除 |
| Fluent Bit | 轻量 | 日志解析复杂 | 排除 |
| 直接文件采集 | 简单 | 不支持结构化查询 | 排除 |

**Implementation**:
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Logback 配置**:
```xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>tenantId</includeMdcKeyName>
        <customFields>{"service":"${SERVICE_NAME:-unknown}"}</customFields>
    </encoder>
</appender>
```

---

### 3. Traces 采集方案

**Decision**: Micrometer Tracing + OpenTelemetry + Tempo

**Rationale**:
- Micrometer Tracing 是 Spring Boot 3.x 推荐的追踪方案
- OpenTelemetry 是 CNCF 标准，避免厂商锁定
- Tempo 兼容 Jaeger 协议，存储成本比 Jaeger 低 90%
- 与 Grafana 原生集成，支持 Trace ID 跳转

**Alternatives Considered**:
| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| Jaeger | 功能完整 | 存储成本高（Cassandra/ES） | 排除 |
| Zipkin | 简单 | 社区活跃度下降 | 排除 |
| SkyWalking | APM 功能丰富 | 重型，资源消耗大 | 排除 |

**Implementation**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

---

### 4. 可视化方案

**Decision**: Grafana OSS

**Rationale**:
- 统一界面访问 Prometheus、Loki、Tempo
- 丰富的预置仪表盘模板
- 支持 Explore 模式进行即席查询
- 原生告警功能
- 开源免费

**Dashboard 规划**:
| 仪表盘 | 用途 | 数据源 |
|--------|------|--------|
| 服务概览 | 所有服务健康状态 | Prometheus |
| JVM 详情 | 内存、GC、线程 | Prometheus |
| 业务指标 | 设备、消息、延迟 | Prometheus |
| 日志查询 | 按服务/租户查询日志 | Loki |
| 链路追踪 | 请求调用链分析 | Tempo |

---

### 5. 告警方案

**Decision**: Grafana Alerting

**Rationale**:
- 与 Grafana 原生集成，无需额外组件
- 支持多数据源告警（Prometheus + Loki）
- 支持告警分组、静默、抑制
- 支持多种通知渠道（Email、Webhook、Slack 等）

**Alert Rules**:
| 告警名称 | 条件 | 级别 |
|----------|------|------|
| ServiceDown | up == 0 for 1m | Critical |
| HighErrorRate | 5xx rate > 1% | Warning |
| HighLatency | P95 > 3s | Warning |
| KafkaLag | lag > 10000 | Warning |
| HighMemory | memory > 85% | Warning |

---

## 最佳实践

### Metrics 命名规范

```
{namespace}_{subsystem}_{metric_name}_{unit}
```

示例:
- `openiot_device_connected_count`
- `openiot_message_received_total`
- `openiot_message_processing_seconds`
- `openiot_error_total`

### 日志字段规范

```json
{
  "@timestamp": "2026-03-01T12:00:00.000Z",
  "@version": "1",
  "level": "INFO",
  "logger_name": "com.openiot.device.DeviceService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "Device connected",
  "traceId": "abc123",
  "tenantId": "tenant-001",
  "service": "device-service",
  "context": {
    "deviceId": "device-001"
  }
}
```

### Trace 采样策略

```yaml
# 开发环境：100% 采样
management.tracing.sampling.probability: 1.0

# 生产环境：10% 采样
management.tracing.sampling.probability: 0.1
```

### 数据保留策略

| 数据类型 | 开发环境 | 生产环境 |
|----------|----------|----------|
| Metrics | 7 天 | 15 天 |
| Logs | 3 天 | 7 天 |
| Traces | 1 天 | 3 天 |

---

## 依赖清单

### 后端依赖

```xml
<!-- Metrics -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- Structured Logging -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 基础设施组件

```yaml
# docker-compose.observability.yml
services:
  prometheus:
    image: prom/prometheus:v2.48.0

  loki:
    image: grafana/loki:2.9.0

  tempo:
    image: grafana/tempo:2.3.0

  grafana:
    image: grafana/grafana:10.2.0
```

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 高 cardinality 指标 | Prometheus 性能下降 | 控制标签基数，避免无界标签 |
| 日志量过大 | Loki 存储压力 | 配置日志级别，敏感日志脱敏 |
| Trace 存储成本高 | 预算超支 | 生产环境使用采样策略 |
| 告警风暴 | 通知疲劳 | 配置告警分组和静默规则 |

---

## 结论

LGTM Stack (Grafana + Prometheus + Loki + Tempo) 是当前最适合本项目的可观测性方案：

1. **统一生态**: Grafana 作为单一入口，降低学习成本
2. **轻量高效**: 相比 ELK，资源消耗低 5-10 倍
3. **云原生标准**: 与 Kubernetes 和 Spring Boot 完美集成
4. **开源免费**: 无许可成本，社区活跃

所有技术决策均基于项目现有技术栈（Spring Boot 3.x、Docker Compose）和 Constitution 要求（可观测性强制原则）。

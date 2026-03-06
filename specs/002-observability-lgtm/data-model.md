# Data Model: 可观测性系统 (LGTM Stack)

**Feature**: 002-observability-lgtm
**Date**: 2026-03-01

## 概述

可观测性系统主要使用时序数据库（Prometheus、Loki、Tempo）存储数据，不需要关系型数据库表结构。本文档定义数据模型的概念结构和标签规范。

---

## 实体关系图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Observability Data                          │
├─────────────────┬─────────────────┬─────────────────────────────────┤
│    Metrics      │     Logs        │           Traces                │
│   (Prometheus)  │    (Loki)       │          (Tempo)                │
├─────────────────┼─────────────────┼─────────────────────────────────┤
│ - name          │ - timestamp     │ - traceId                       │
│ - labels        │ - labels        │ - spans[]                       │
│ - value         │ - message       │   - spanId                      │
│ - timestamp     │ - level         │   - parentSpanId                │
│                 │ - traceId       │   - operationName               │
│                 │ - tenantId      │   - startTime                   │
│                 │ - service       │   - duration                    │
│                 │ - context       │   - tags                        │
│                 │                 │   - logs                        │
└─────────────────┴─────────────────┴─────────────────────────────────┘
```

---

## 1. Metric (指标)

### 概念定义

指标是带时间戳的数值测量，用于描述系统在特定时间点的状态。

### 标签规范

| 标签名 | 类型 | 必需 | 说明 | 示例 |
|--------|------|------|------|------|
| `service` | string | ✅ | 服务名称 | `device-service` |
| `tenant_id` | string | ❌ | 租户 ID（业务指标） | `tenant-001` |
| `instance` | string | ✅ | 实例地址 | `10.0.0.1:8080` |
| `job` | string | ✅ | 抓取任务名 | `openiot-services` |
| `error_type` | string | ❌ | 错误类型 | `database_timeout` |
| `method` | string | ❌ | HTTP 方法 | `POST` |
| `uri` | string | ❌ | 请求路径 | `/api/devices` |
| `status` | string | ❌ | HTTP 状态码 | `500` |

### 预置指标

#### 系统指标 (JVM)

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `jvm_memory_used_bytes` | Gauge | JVM 已用内存 |
| `jvm_memory_max_bytes` | Gauge | JVM 最大内存 |
| `jvm_gc_pause_seconds_sum` | Counter | GC 暂停总时间 |
| `jvm_gc_pause_seconds_count` | Counter | GC 暂停次数 |
| `jvm_threads_live_threads` | Gauge | 活跃线程数 |

#### 连接池指标

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `hikaricp_connections_active` | Gauge | 活跃数据库连接数 |
| `hikaricp_connections_idle` | Gauge | 空闲数据库连接数 |
| `hikaricp_connections_pending` | Gauge | 等待获取连接的线程数 |
| `lettuce_command_active` | Gauge | Redis 活跃命令数 |

#### Kafka 指标

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `kafka_consumer_lag` | Gauge | 消费延迟 |
| `kafka_consumer_records_consumed_total` | Counter | 已消费消息总数 |

#### 业务指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `openiot_device_connected_count` | Gauge | tenant_id | 在线设备数 |
| `openiot_message_received_total` | Counter | tenant_id, protocol | 接收消息总数 |
| `openiot_message_processed_total` | Counter | tenant_id, status | 处理消息总数 |
| `openiot_message_processing_seconds` | Timer | tenant_id | 消息处理延迟 |
| `openiot_error_total` | Counter | service, error_type | 错误总数 |

---

## 2. LogEntry (日志条目)

### 概念定义

日志条目记录系统运行时发生的事件，包含时间戳、级别、消息和上下文信息。

### 结构定义

```json
{
  "@timestamp": "2026-03-01T12:00:00.000+08:00",
  "@version": "1",
  "level": "INFO",
  "logger_name": "com.openiot.device.service.DeviceService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "Device connected successfully",
  "traceId": "a1b2c3d4e5f6",
  "tenantId": "tenant-001",
  "service": "device-service",
  "context": {
    "deviceId": "device-001",
    "protocol": "MQTT"
  },
  "stack_trace": null
}
```

### 标签规范 (Loki)

| 标签名 | 类型 | 必需 | 说明 | 示例 |
|--------|------|------|------|------|
| `service` | string | ✅ | 服务名称 | `device-service` |
| `level` | string | ✅ | 日志级别 | `ERROR`, `WARN`, `INFO`, `DEBUG` |
| `tenant_id` | string | ❌ | 租户 ID | `tenant-001` |

### 日志级别定义

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 异常、错误、需立即处理 | 数据库连接失败 |
| WARN | 潜在问题、需关注 | 慢查询警告 |
| INFO | 关键业务事件 | 设备上下线、用户登录 |
| DEBUG | 调试信息（生产关闭） | 请求参数详情 |

### 敏感信息脱敏

以下字段必须在日志中脱敏或排除：

- `password`
- `token`
- `secret`
- `apiKey`
- `idCard` (身份证号)
- `phone` (手机号)

---

## 3. Trace (链路追踪)

### 概念定义

Trace 表示一个请求在分布式系统中的完整调用路径，由多个 Span 组成。

### 结构定义

```json
{
  "traceId": "a1b2c3d4e5f67890",
  "spans": [
    {
      "spanId": "span-001",
      "parentSpanId": null,
      "operationName": "POST /api/devices/data",
      "startTime": "2026-03-01T12:00:00.000Z",
      "duration": 150000000,
      "tags": {
        "http.method": "POST",
        "http.url": "/api/devices/data",
        "http.status_code": 200,
        "service": "gateway-service"
      },
      "logs": [
        {
          "timestamp": "2026-03-01T12:00:00.050Z",
          "fields": {"event": "request_received"}
        }
      ]
    },
    {
      "spanId": "span-002",
      "parentSpanId": "span-001",
      "operationName": "device-service.processData",
      "startTime": "2026-03-01T12:00:00.010Z",
      "duration": 80000000,
      "tags": {
        "service": "device-service",
        "db.type": "postgresql",
        "db.statement": "INSERT INTO device_trajectory..."
      }
    }
  ]
}
```

### Span 类型

| 类型 | 说明 | 示例 |
|------|------|------|
| HTTP | HTTP 请求/响应 | `POST /api/devices` |
| RPC | 远程过程调用 | `device-service.createDevice` |
| DB | 数据库操作 | `INSERT INTO device` |
| CACHE | 缓存操作 | `redis.get` |
| MESSAGE | 消息队列 | `kafka.send` |

### 标签规范

| 标签名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| `service` | string | ✅ | 服务名称 |
| `http.method` | string | ❌ | HTTP 方法 |
| `http.url` | string | ❌ | 请求 URL |
| `http.status_code` | int | ❌ | HTTP 状态码 |
| `db.type` | string | ❌ | 数据库类型 |
| `db.statement` | string | ❌ | SQL 语句（脱敏） |
| `messaging.system` | string | ❌ | 消息系统 |
| `messaging.destination` | string | ❌ | 消息主题 |

---

## 4. AlertRule (告警规则)

### 概念定义

告警规则定义触发告警的条件和通知方式。

### 配置结构

```yaml
groups:
  - name: openiot-alerts
    rules:
      - alert: ServiceDown
        expr: up{job="openiot-services"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.service }} 宕机"
          description: "服务 {{ $labels.instance }} 已经宕机超过 1 分钟"

      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          /
          sum(rate(http_server_requests_seconds_count[5m])) > 0.01
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "高错误率告警"
          description: "服务 {{ $labels.service }} 5xx 错误率超过 1%"
```

### 告警级别

| 级别 | 含义 | 响应时间 | 通知方式 |
|------|------|----------|----------|
| Critical | 严重故障，服务不可用 | 立即 | 电话 + 短信 + 邮件 |
| Warning | 潜在问题，需关注 | 1 小时内 | 短信 + 邮件 |
| Info | 信息通知 | 24 小时内 | 邮件 |

---

## 5. AlertEvent (告警事件)

### 概念定义

告警事件记录告警的触发和恢复。

### 结构定义

```json
{
  "status": "firing",
  "labels": {
    "alertname": "ServiceDown",
    "service": "device-service",
    "severity": "critical"
  },
  "annotations": {
    "summary": "服务 device-service 宕机",
    "description": "服务 10.0.0.1:8080 已经宕机超过 1 分钟"
  },
  "startsAt": "2026-03-01T12:00:00.000Z",
  "endsAt": "0001-01-01T00:00:00.000Z",
  "generatorURL": "http://prometheus:9090/graph?g0.expr=..."
}
```

---

## 数据流向

```
┌──────────────────────────────────────────────────────────────────────┐
│                           Application Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │   Gateway   │  │   Device    │  │    Data     │  │   Tenant    │ │
│  │   Service   │  │   Service   │  │   Service   │  │   Service   │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘ │
│         │                │                │                │        │
│         └────────────────┴────────────────┴────────────────┘        │
│                                   │                                  │
│                          /actuator/prometheus                        │
│                          stdout (JSON logs)                          │
│                          OTLP (traces)                               │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        Collection Layer                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │ Prometheus  │  │    Loki     │  │    Tempo    │                  │
│  │  (Metrics)  │  │   (Logs)    │  │  (Traces)   │                  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘                  │
└─────────┼────────────────┼────────────────┼──────────────────────────┘
          │                │                │
          └────────────────┴────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       Visualization Layer                             │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │                        Grafana                                   ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              ││
│  │  │ Dashboards  │  │   Explore   │  │   Alerts    │              ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘              ││
│  └─────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────┘
```

---

## 存储容量估算

假设场景：10 个微服务，1000 设备在线，10000 消息/分钟

| 数据类型 | 每秒数据量 | 日均存储 | 保留期 | 总存储 |
|----------|-----------|----------|--------|--------|
| Metrics | ~500 samples/s | ~40 MB | 15 天 | ~600 MB |
| Logs | ~100 entries/s | ~500 MB | 7 天 | ~3.5 GB |
| Traces (10% 采样) | ~10 traces/s | ~100 MB | 3 天 | ~300 MB |

**总计**: ~4.4 GB（可观测性数据存储）

---

## 无数据库变更

本功能**不需要**创建新的数据库表或执行 Flyway 迁移。所有可观测性数据存储在：

- **Prometheus**: 时序指标数据
- **Loki**: 结构化日志数据
- **Tempo**: 分布式链路数据

这些存储由基础设施组件管理，不需要应用层数据库支持。

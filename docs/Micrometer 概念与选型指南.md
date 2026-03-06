# Micrometer 概念与选型指南

## 目录
- [1. Micrometer 是什么](#1-micrometer-是什么)
- [2. 数据形态与仪表类型](#2-数据形态与仪表类型)
- [3. 如何选择指标类型](#3-如何选择指标类型)
- [4. 与 Prometheus 的映射关系](#4-与-prometheus-的映射关系)
- [5. Micrometer、OpenTelemetry、Prometheus 的分层关系](#5-micrometeropentelemetryprometheus-的分层关系)
- [6. 跨语言可观测选型](#6-跨语言可观测选型)
- [7. 什么时候选 Micrometer，什么时候选 OTel Metrics](#7-什么时候选-micrometer什么时候选-otel-metrics)
- [8. 指标模型设计规范](#8-指标模型设计规范)
- [9. 常见误区](#9-常见误区)

## 1. Micrometer 是什么

Micrometer 是 Java 生态常用的指标门面（Metrics Facade）。

- 代码层通过 Micrometer API 注册指标（`Counter/Gauge/Timer` 等）
- Spring Boot Actuator 暴露 `/actuator/prometheus`
- Prometheus 定时抓取并存储
- Grafana 查询并展示

一句话：
- Micrometer 负责“在应用里定义和输出指标”
- Prometheus 负责“采集、存储、查询指标”

> Micrometer 主要面向 Java。
> Node.js、Go、Python 等项目不使用 Micrometer，也可以正常接入 Prometheus。

常见语言对应库：
- Java: Micrometer (+ Actuator Prometheus)
- Node.js: `prom-client`
- Go: `prometheus/client_golang`
- Python: `prometheus_client`

---

## 2. 数据形态与仪表类型

工程上常见 5~6 类数据形态，对应 Micrometer 的 Meter 类型。

### 2.1 最常用 4 类

1. `Counter`
- 单调递增累计值
- 场景：请求总数、错误总数、拒绝任务总数
- PromQL 常配合：`rate(counter[5m])`

2. `Gauge`
- 当前瞬时值，可增可减
- 场景：在线设备数、线程池活跃线程、队列长度、内存占用

3. `Timer`
- 耗时统计（次数 + 总时长 + 最大值 + 分位）
- 场景：接口耗时、任务耗时、消息处理耗时

4. `DistributionSummary`
- 数值分布统计（非时间）
- 场景：消息大小、批处理数量、payload 字节数

### 2.2 扩展类型

5. `LongTaskTimer`
- 记录进行中的长任务时长
- 场景：导入、同步、批处理长任务

6. `FunctionCounter` / `FunctionTimer`
- 函数式读取对象状态
- 场景：不便主动 `increment/record` 的对象指标

---

## 3. 如何选择指标类型

快速决策：
- “总共发生多少次” -> `Counter`
- “当前有多少” -> `Gauge`
- “耗时快慢与分位数” -> `Timer`
- “数值分布（非时间）” -> `DistributionSummary`
- “长任务执行时长” -> `LongTaskTimer`

---

## 4. 与 Prometheus 的映射关系

Micrometer 常用点号命名：
- `openiot.device.online.count`

Prometheus 暴露通常为下划线：
- `openiot_device_online_count`

因此在 Grafana/PromQL 里通常看到下划线风格。

---

## 5. Micrometer、OpenTelemetry、Prometheus 的分层关系

### 5.1 应用埋点层
- Java 常用 Micrometer
- 其他语言用 Prometheus SDK 或 OTel SDK

职责：定义指标语义并记录事件。

### 5.2 导出层
- `/metrics` 或 `/actuator/prometheus`
- OTel Push（可选）到 Collector

职责：输出可采集格式。

### 5.3 后端层
- Prometheus（可 remote_write 到 Mimir/Thanos/Cortex）

职责：抓取、存储、查询、告警评估。

### 5.4 可视化运维层
- Grafana

职责：仪表盘、探索、告警管理。

结论：Micrometer 与 Prometheus 是协作关系，不是替代关系。

---

## 6. 跨语言可观测选型

### 6.1 常见语言与库

| 语言 | 推荐指标库 | 常见端点 | 备注 |
|------|------------|----------|------|
| Java/Spring | Micrometer + Actuator | `/actuator/prometheus` | 集成成本低 |
| Go | `prometheus/client_golang` | `/metrics` | 云原生事实标准 |
| Node.js | `prom-client` | `/metrics` | 注意标签基数 |
| Python | `prometheus_client` | `/metrics` | 多进程需单独处理 |
| .NET | `prometheus-net` 或 OTel SDK | `/metrics` | 生态成熟 |

### 6.2 统一治理建议

1. 统一命名规范（前缀、单位、后缀）
2. 统一标签规范（`service/env/version/instance`）
3. 统一高基数治理（禁止 `requestId/userId/deviceId` 直出标签）
4. 统一 SLI/SLO 口径（成功率、延迟、吞吐、饱和度）
5. 统一 dashboard 模板

---

## 7. 什么时候选 Micrometer，什么时候选 OTel Metrics

### 7.1 优先 Micrometer
- Spring Boot 为主
- 需要快速接入 Prometheus + Grafana
- 团队熟悉 Actuator / `MeterRegistry`

### 7.2 考虑 OTel Metrics
- 多语言统一治理诉求强
- 已部署 OTel Collector
- 指标/日志/链路希望统一走 OTel 语义和管道

### 7.3 常见混合实践
- Java: Micrometer + Prometheus
- 其他语言: Prometheus SDK 或 OTel SDK
- 展示层统一 Grafana

---

## 8. 指标模型设计规范

### 8.1 命名规范

推荐结构：`{domain}_{subsystem}_{metric}_{unit}`

示例：
- `openiot_device_online_count`
- `openiot_message_processing_seconds`
- `openiot_kafka_consumer_lag`

### 8.2 标签规范

保留：
- `service`
- `env`
- `version`
- `tenant`（仅在基数可控时）

避免：
- `request_id`
- `trace_id`
- `device_id`（高基数风险）

### 8.3 维度控制原则

1. 先定义要回答的问题，再加标签
2. 标签尽量少且稳定
3. 高基数维度交给日志/Trace，不进指标标签

---

## 9. 常见误区

1. “Prometheus 能自动知道我的业务指标”
- 错。必须先埋点并暴露。

2. “Gauge 可以累计总数”
- 不推荐。累计总数应使用 `Counter`。

3. “只有 JVM 指标就够了”
- 不够。还需要业务指标。

4. “只有 Java 才能接 Prometheus”
- 错。Prometheus 是跨语言方案。

---

## 下一步阅读

继续阅读落地篇：
- [Micrometer 实战落地指南](./Micrometer%20实战落地指南.md)

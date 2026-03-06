# Micrometer 实战落地指南

## 目录
- [1. Open-IoT 指标代码映射](#1-open-iot-指标代码映射)
- [2. 线程池指标落地模板](#2-线程池指标落地模板)
- [3. Dashboard 与查询表达式治理](#3-dashboard-与查询表达式治理)
- [4. 告警与 SLO 落地建议](#4-告警与-slo-落地建议)
- [5. 从可用到可运维的实施路线图](#5-从可用到可运维的实施路线图)
- [6. Open-IoT 当前状态对照（2026-03）](#6-open-iot-当前状态对照2026-03)
- [7. 常见问题排查清单](#7-常见问题排查清单)

## 1. Open-IoT 指标代码映射

### 1.1 关键指标来源

- 公共业务指标封装：
  - `backend/common/common-observability/src/main/java/com/openiot/common/observability/metrics/BusinessMetrics.java`

- 设备指标采集：
  - `backend/device-service/src/main/java/com/openiot/device/metrics/DeviceMetricsCollector.java`

- 消息与 Kafka 指标采集：
  - `backend/data-service/src/main/java/com/openiot/data/metrics/MessageMetricsCollector.java`
  - `backend/data-service/src/main/java/com/openiot/data/metrics/KafkaLagMetrics.java`

### 1.2 关系说明

- `DeviceMetricsCollector`、`MessageMetricsCollector` 调用 `BusinessMetrics`
- `KafkaLagMetrics` 是并行采集器（不依赖 `BusinessMetrics`）

### 1.3 典型指标

- 设备：`openiot_device_total_count`、`openiot_device_online_count`
- 消息：`openiot_message_received_total`、`openiot_message_processed_total`、`openiot_message_failed_total`
- Kafka：`kafka_consumer_group_lag`、`kafka_consumer_group_members`

---

## 2. 线程池指标落地模板

JVM 全局线程指标不等于业务线程池观测。建议给关键线程池补充指标。

### 2.1 推荐指标

- `thread_pool_active_threads` (`Gauge`)
- `thread_pool_pool_size` (`Gauge`)
- `thread_pool_core_size` (`Gauge`)
- `thread_pool_max_size` (`Gauge`)
- `thread_pool_queue_size` (`Gauge`)
- `thread_pool_queue_remaining_capacity` (`Gauge`)
- `thread_pool_completed_tasks_total` (`Counter` 或函数式累计)
- `thread_pool_rejected_tasks_total` (`Counter`)
- `thread_pool_task_duration_seconds` (`Timer`, 可选)

### 2.2 推荐标签

- `service`
- `pool`
- `module`（可选：`kafka`、`biz`、`io`）

### 2.3 实现提示（Java）

1. 给每个 `ThreadPoolExecutor` 命名并集中注册指标
2. 在拒绝策略中显式计数 `rejected_total`
3. 任务执行用 `Timer` 记录，关注 p95/p99

### 2.4 常见坑

1. 把业务 key 做标签，导致高基数
2. 只看 pool size，不看 queue/reject
3. 只看平均值，不看分位数

---

## 3. Dashboard 与查询表达式治理

### 3.1 NaN 与 No data 的常见原因

- `NaN`：除零（如 `online/total` 且 total=0）
- `No data`：指标未上报或时间窗内无样本

### 3.2 查询防护模式

- 防除零：
  - `a / clamp_min(b, 1)`
- 无数据回退：
  - `metric or vector(0)`

### 3.3 Open-IoT 已落地优化

- 在线率、防除零
- Hikari 时间指标、防除零
- 租户统计指标无数据回退 0

---

## 4. 告警与 SLO 落地建议

### 4.1 先定义四类核心 SLI

1. 可用性：`up`、成功率
2. 延迟：p95/p99
3. 吞吐：QPS/消费速率
4. 饱和度：线程池、队列、连接池、Kafka lag

### 4.2 告警分级建议

- `Critical`：服务不可用、积压爆炸、错误率高
- `Warning`：延迟上升、资源逼近上限
- `Info`：发布变更、轻微抖动

### 4.3 告警表达式示例

- 服务宕机：`up{job="spring-boot-services"} == 0`
- 错误率：
  - `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))`
- Kafka 积压：`sum(kafka_consumer_group_lag) by (group,topic)`

---

## 5. 从可用到可运维的实施路线图

### 阶段 1：基础可见性

- 接入 `up`、HTTP、JVM、连接池
- 打通 Prometheus -> Grafana

### 阶段 2：业务可见性

- 设备在线、消息吞吐、失败率
- 仪表盘和告警联动

### 阶段 3：容量与稳定性

- 线程池/队列/reject 观测
- 构建容量模型与扩缩容策略

### 阶段 4：平台化治理

- 指标规范制度化
- SLO 与错误预算体系
- 新服务接入准入清单

---

## 6. Open-IoT 当前状态对照（2026-03）

已具备：
- Prometheus 抓取链路打通（核心服务 + Nacos）
- Grafana 预置 dashboard 可用
- 已修复关键 `NaN/No data` 表达式问题

待增强：
1. 落地线程池级指标
2. 租户统计真实指标补齐（当前有回退 0）
3. 指标命名与标签规范纳入 CI 检查
4. 增加 SLO 面板（可用性/延迟/错误预算）

---

## 7. 常见问题排查清单

### 7.1 面板无数据

1. 看 `http://localhost:9090/targets` 是否全 `UP`
2. 检查服务 `/actuator/prometheus` 是否 `200`
3. 检查 dashboard 查询指标名是否存在

### 7.2 出现 NaN

1. 检查是否存在除法表达式
2. 使用 `clamp_min(..., 1)` 防止除零

### 7.3 指标名对不上

1. Micrometer 点号命名会映射为下划线
2. 优先在 Prometheus 用 label `__name__` 搜索确认

### 7.4 服务都 UP 但面板仍空

1. 可能只是无业务流量
2. 造数验证：发送真实请求/消息，再观察 5~10 分钟

---

## 关联阅读

- [Micrometer 概念与选型指南](./Micrometer%20概念与选型指南.md)
- [LGTM Stack 指南](./LGTM%20Stack%20指南.md)

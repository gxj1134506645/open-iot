# Feature Specification: 可观测性系统 (LGTM Stack)

**Feature Branch**: `002-observability-lgtm`
**Created**: 2026-03-01
**Status**: Draft
**Input**: 实现可观测性系统，使用 LGTM Stack (Grafana + Prometheus + Loki + Tempo)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 实时监控仪表盘 (Priority: P1)

作为运维人员，我需要一个统一的仪表盘来实时查看所有微服务的运行状态，包括服务健康、资源使用、业务指标等，以便快速发现和定位问题。

**Why this priority**: 可观测性的核心价值是"看见"系统状态，统一仪表盘是实现这一目标的基础，是所有其他功能的前提。

**Independent Test**: 启动 Grafana 后，访问预置仪表盘，能够看到所有微服务的实时指标（CPU、内存、请求量、错误率）。

**Acceptance Scenarios**:

1. **Given** 所有微服务已启动并接入监控，**When** 打开 Grafana 仪表盘，**Then** 能看到所有服务的健康状态（绿色/红色指示）
2. **Given** 某服务出现异常，**When** 查看仪表盘，**Then** 该服务状态显示为异常并标红
3. **Given** 仪表盘已配置，**When** 选择特定时间范围，**Then** 指标图表正确显示该时间段的数据

---

### User Story 2 - 分布式链路追踪 (Priority: P1)

作为开发人员，当用户报告请求超时时，我需要通过链路追踪快速定位是哪个服务、哪个操作导致了延迟，以便修复性能问题。

**Why this priority**: 微服务架构下，请求跨越多个服务，没有链路追踪几乎无法定位性能瓶颈和故障点。

**Independent Test**: 发起一个跨服务请求，在 Grafana/Tempo 中通过 Trace ID 查看完整的调用链路和各节点耗时。

**Acceptance Scenarios**:

1. **Given** 用户发起设备数据上报请求，**When** 请求经过 Gateway → Device Service → Kafka → Data Service，**Then** 能在追踪系统中看到完整的调用链
2. **Given** 某服务处理耗时过长，**When** 查看链路追踪，**Then** 能准确识别是哪个服务的哪个操作耗时异常
3. **Given** 日志中包含 Trace ID，**When** 在 Grafana 中点击 Trace ID，**Then** 能直接跳转到对应的链路详情

---

### User Story 3 - 结构化日志查询 (Priority: P2)

作为运维人员，当收到告警后，我需要通过日志快速定位问题原因，使用结构化字段（如 traceId、tenantId、service）进行精确查询。

**Why this priority**: 日志是故障排查的最后手段，结构化日志大幅提升查询效率，但相对于仪表盘和链路追踪，优先级略低。

**Independent Test**: 在 Grafana/Loki 中使用 LogQL 查询特定租户、特定服务的日志，并在日志中看到 Trace ID 关联。

**Acceptance Scenarios**:

1. **Given** 系统产生日志，**When** 查询日志，**Then** 日志以 JSON 格式展示，包含 timestamp、level、traceId、tenantId、service、message 字段
2. **Given** 需要排查特定租户问题，**When** 使用 `{tenantId="tenant-001"}` 查询，**Then** 只返回该租户相关日志
3. **Given** 从链路追踪获得 Trace ID，**When** 使用该 Trace ID 查询日志，**Then** 返回该请求相关的所有日志

---

### User Story 4 - 智能告警 (Priority: P2)

作为运维负责人，我需要配置告警规则，当系统出现异常（如服务宕机、错误率飙升、延迟过高）时自动通知相关人员，以便及时处理。

**Why this priority**: 告警是可观测性的"最后一公里"，确保问题被及时感知，但需要先有指标和日志作为基础。

**Independent Test**: 模拟服务故障（如停止某服务），在 1 分钟内收到告警通知。

**Acceptance Scenarios**:

1. **Given** 配置了服务健康检查告警，**When** 某服务停止超过 1 分钟，**Then** 触发 Critical 级别告警
2. **Given** 配置了错误率告警（阈值 1%），**When** 某服务 5xx 错误率超过 1%，**Then** 触发 Warning 级别告警
3. **Given** 配置了延迟告警（P95 > 3s），**When** 请求延迟持续超过阈值，**Then** 触发告警
4. **Given** 告警已触发，**When** 问题恢复，**Then** 发送恢复通知

---

### User Story 5 - 业务指标监控 (Priority: P3)

作为产品经理，我需要查看业务相关的指标（如设备在线数、消息吞吐量、租户活跃度），以便了解平台运营状况。

**Why this priority**: 业务指标对运营决策有价值，但对系统稳定性来说，技术指标优先级更高。

**Independent Test**: 在 Grafana 业务仪表盘中查看实时设备在线数、今日消息总量等指标。

**Acceptance Scenarios**:

1. **Given** 设备连接到平台，**When** 查看设备监控仪表盘，**Then** 能看到实时在线设备数量
2. **Given** 设备上报数据，**When** 查看消息吞吐仪表盘，**Then** 能看到每秒消息接收/处理/失败数量
3. **Given** 多个租户使用平台，**When** 查看租户活跃度仪表盘，**Then** 能看到各租户的设备数、消息量分布

---

### Edge Cases

- 当 Prometheus 无法抓取某服务指标时，仪表盘应显示"数据不可用"而非空白或报错
- 当 Trace 采样率设为 10% 时，90% 的请求没有 Trace ID，日志查询应仍能通过其他字段定位
- 当 Loki 存储满时，旧日志应自动清理，不影响新日志写入
- 当告警风暴发生时（短时间内大量告警），应进行告警聚合和静默，避免通知轰炸
- 当 Grafana 无法连接数据源时，应显示友好的错误提示，而非空白页面

## Requirements *(mandatory)*

### Functional Requirements

**指标采集 (Metrics)**
- **FR-001**: 所有微服务 MUST 通过 `/actuator/prometheus` 端点暴露 Prometheus 格式指标
- **FR-002**: 系统 MUST 采集 JVM 指标（堆内存、GC 次数/时间、线程数）
- **FR-003**: 系统 MUST 采集数据库连接池指标（活跃连接数、等待队列长度）
- **FR-004**: 系统 MUST 采集 Redis 连接池指标（活跃连接数、等待命令数）
- **FR-005**: 系统 MUST 采集 Kafka 消费者指标（消费延迟、消费速率）
- **FR-006**: 系统 MUST 暴露自定义业务指标（设备连接数、消息吞吐量、处理延迟 P50/P95/P99、错误率）

**日志采集 (Logs)**
- **FR-007**: 所有日志 MUST 使用 JSON 格式输出，包含 timestamp、level、traceId、tenantId、service、message 字段
- **FR-008**: 日志 MUST NOT 包含敏感信息（密码、Token、身份证号等）
- **FR-009**: 日志级别 MUST 正确使用（ERROR=异常/错误、WARN=潜在问题、INFO=关键业务事件、DEBUG=调试信息）
- **FR-010**: 日志 MUST 输出到标准输出，由基础设施采集到 Loki

**链路追踪 (Traces)**
- **FR-011**: 所有 HTTP 请求 MUST 生成或传播 Trace ID
- **FR-012**: 所有跨服务调用（Feign、Kafka 消息）MUST 传播 Trace ID
- **FR-013**: 数据库操作、Redis 操作、Kafka 消息 MUST 作为 Span 记录
- **FR-014**: 生产环境 SHOULD 使用采样策略（如 10%）降低存储成本

**可视化 (Visualization)**
- **FR-015**: 系统 MUST 提供预置的 Grafana 仪表盘（服务概览、JVM 详情、业务指标）
- **FR-016**: 仪表盘 MUST 支持从 Metrics 跳转到 Traces、从 Traces 跳转到 Logs
- **FR-017**: 仪表盘 MUST 支持按服务名、租户 ID 筛选

**告警 (Alerting)**
- **FR-018**: 系统 MUST 配置服务健康告警（服务宕机 > 1min 触发 Critical）
- **FR-019**: 系统 MUST 配置错误率告警（5xx > 1% 触发 Warning）
- **FR-020**: 系统 MUST 配置延迟告警（P95 > 3s 触发 Warning）
- **FR-021**: 系统 MUST 配置消息积压告警（Kafka Lag > 10000 触发 Warning）
- **FR-022**: 系统 MUST 配置资源告警（内存 > 85%、磁盘 > 90% 触发告警）

**健康检查 (Health Check)**
- **FR-023**: 所有服务 MUST 提供 `/actuator/health/liveness` 存活探针
- **FR-024**: 所有服务 MUST 提供 `/actuator/health/readiness` 就绪探针
- **FR-025**: 健康检查 MUST 包含依赖组件状态（PostgreSQL、Redis、MongoDB、Kafka）

### Key Entities

- **Metric**: 指标数据，包含名称、标签（服务名、租户ID等）、值、时间戳
- **LogEntry**: 日志条目，包含时间戳、级别、Trace ID、租户 ID、服务名、消息内容、上下文
- **Trace**: 链路追踪，包含 Trace ID、Span 列表、总耗时
- **Span**: 追踪片段，包含 Span ID、父 Span ID、操作名、开始时间、耗时、标签
- **AlertRule**: 告警规则，包含名称、条件表达式、严重级别、通知渠道
- **AlertEvent**: 告警事件，包含规则名称、触发时间、当前值、状态（firing/resolved）

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 运维人员能在 30 秒内从 Grafana 首页找到任意服务的健康状态
- **SC-002**: 开发人员能通过 Trace ID 在 1 分钟内定位到跨服务调用的性能瓶颈
- **SC-003**: 90% 的故障排查能在 5 分钟内通过日志查询定位到根本原因
- **SC-004**: 服务故障发生后 1 分钟内触发告警通知
- **SC-005**: 可观测性系统自身开销 < 5% 的 CPU 和内存
- **SC-006**: 指标数据保留 15 天，日志数据保留 7 天，链路数据保留 3 天
- **SC-007**: 所有仪表盘加载时间 < 3 秒

## Assumptions

- 项目已使用 Spring Boot 3.x，可直接集成 Micrometer 和 Spring Actuator
- 基础设施使用 Docker Compose 部署，可添加 Prometheus、Loki、Tempo、Grafana 容器
- 团队对 PromQL 和 LogQL 有基本了解，或愿意学习
- 生产环境会根据实际负载调整采样率和数据保留策略
- 告警通知初期使用 Grafana 内置通知渠道（邮件/Webhook），后续可对接企业 IM

## Out of Scope

- 商业可观测性方案（Datadog、Splunk、Dynatrace）的集成
- APM 的高级功能（代码级热点分析、内存泄漏检测）
- 前端性能监控（RUM）
- 日志的全文索引（仅使用标签索引）
- AI 驱动的异常检测和根因分析

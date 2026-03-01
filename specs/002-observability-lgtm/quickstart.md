# Quick Start: 可观测性系统 (LGTM Stack)

**Feature**: 002-observability-lgtm
**Date**: 2026-03-01

## 前置条件

- Docker Desktop 已安装并运行
- 后端服务已构建 (`./scripts/build.sh -b`)
- 至少 8GB 可用内存（推荐 16GB）

## 快速启动

### 1. 启动可观测性基础设施

```bash
# 启动 Prometheus + Loki + Tempo + Grafana
cd infrastructure/docker
docker-compose -f docker-compose.yml -f docker-compose.observability.yml up -d

# 查看服务状态
docker-compose ps
```

### 2. 启动应用服务

```bash
# 返回项目根目录
cd ../..

# 启动基础设施（Nacos、PostgreSQL、Redis、MongoDB、Kafka、EMQX）
./scripts/deploy.sh --infra

# 等待基础设施就绪（约 30 秒）
sleep 30

# 启动后端服务（开发模式）
cd backend
./mvnw spring-boot:run -pl gateway-service &
./mvnw spring-boot:run -pl tenant-service &
./mvnw spring-boot:run -pl device-service &
./mvnw spring-boot:run -pl data-service &
./mvnw spring-boot:run -pl connect-service &
```

### 3. 访问 Grafana

```bash
# 打开浏览器访问
open http://localhost:3000

# 默认登录
用户名: admin
密码: admin
```

## 验证清单

### ✅ 指标采集验证

1. 访问 http://localhost:9090 (Prometheus UI)
2. 执行查询: `up{job="openiot-services"}`
3. 预期结果: 所有服务状态为 `1`

```bash
# 或通过命令行验证
curl -s http://localhost:9090/api/v1/query?query=up | jq .
```

### ✅ 日志采集验证

1. 访问 Grafana → Explore → Loki
2. 执行查询: `{service="device-service"}`
3. 预期结果: 看到 JSON 格式日志，包含 `traceId`、`tenantId` 字段

```bash
# 或通过命令行验证
curl -s "http://localhost:3100/loki/api/v1/query_range" \
  -G --data-urlencode 'query={service="device-service"}' \
  --data-urlencode 'limit=10' | jq .
```

### ✅ 链路追踪验证

1. 访问 Grafana → Explore → Tempo
2. 发起一个跨服务请求:
   ```bash
   curl -X POST http://localhost:8080/api/devices/data \
     -H "Content-Type: application/json" \
     -H "X-Device-Token: test-token" \
     -d '{"lat": 39.9, "lng": 116.4, "timestamp": 1709251200}'
   ```
3. 在 Tempo 中搜索 Trace ID
4. 预期结果: 看到完整的调用链（Gateway → Device Service → Kafka → Data Service）

### ✅ 仪表盘验证

1. 访问 Grafana → Dashboards
2. 打开 "OpenIoT - 服务概览" 仪表盘
3. 预期结果: 看到所有服务的健康状态、CPU、内存、请求量

### ✅ 告警验证

1. 访问 Grafana → Alerting → Alert rules
2. 查看 "openiot-alerts" 规则组
3. 预期结果: 看到预配置的告警规则

```bash
# 模拟服务故障（停止某服务）
# 在另一个终端观察告警触发
docker stop device-service
# 等待 1 分钟，应触发 ServiceDown 告警
```

## 服务地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Grafana | http://localhost:3000 | 可视化平台 (admin/admin) |
| Prometheus | http://localhost:9090 | 指标查询 |
| Loki | http://localhost:3100 | 日志查询 API |
| Tempo | http://localhost:3200 | 链路查询 API |

## 常用查询

### PromQL 查询 (指标)

```promql
# 服务健康状态
up{job="openiot-services"}

# 请求速率 (QPS)
sum(rate(http_server_requests_seconds_count[5m])) by (service)

# 错误率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
/
sum(rate(http_server_requests_seconds_count[5m])) by (service)

# P95 延迟
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service))

# JVM 堆内存使用
jvm_memory_used_bytes{area="heap"}

# 数据库连接池使用率
hikaricp_connections_active / hikaricp_connections_max

# Kafka 消费延迟
kafka_consumer_lag
```

### LogQL 查询 (日志)

```logql
# 按服务查询日志
{service="device-service"}

# 按租户查询日志
{service="device-service"} |= `tenantId="tenant-001"`

# 错误日志
{level="ERROR"}

# 按 Trace ID 查询
{service="device-service"} |= "traceId=a1b2c3d4"

# 慢请求日志 (>1s)
{service="device-service"} | json | duration > 1000
```

### TraceQL 查询 (链路)

```traceql
# 按服务查询
{.service="device-service"}

# 慢请求 (>100ms)
{duration > 100ms}

# 错误请求
{status="error"}

# 按 Trace ID 查询
<trace-id>
```

## 故障排查

### 问题: Prometheus 无法抓取指标

```bash
# 检查服务是否暴露指标端点
curl http://localhost:8080/actuator/prometheus

# 检查 Prometheus 配置
curl http://localhost:9090/api/v1/targets
```

### 问题: Loki 无法采集日志

```bash
# 检查日志是否为 JSON 格式
docker logs device-service | head -5

# 检查 Loki 健康状态
curl http://localhost:3100/ready
```

### 问题: Tempo 无法接收 Trace

```bash
# 检查 OTLP 端点
curl -v http://localhost:4318/v1/traces

# 检查 Tempo 健康状态
curl http://localhost:3200/ready
```

### 问题: Grafana 数据源连接失败

1. 检查数据源配置: Grafana → Configuration → Data sources
2. 点击 "Test" 按钮验证连接
3. 检查网络: `docker network ls`

## 清理

```bash
# 停止可观测性服务
cd infrastructure/docker
docker-compose -f docker-compose.yml -f docker-compose.observability.yml down

# 清理数据卷（可选，会删除历史数据）
docker-compose -f docker-compose.yml -f docker-compose.observability.yml down -v
```

## 下一步

1. 根据实际需求调整告警阈值
2. 配置通知渠道（邮件、Webhook、Slack）
3. 创建自定义仪表盘
4. 调整采样率和数据保留策略

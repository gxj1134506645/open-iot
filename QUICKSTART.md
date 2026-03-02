# Open-IoT 快速启动指南

**适用场景**: 本地开发环境快速启动和功能验证
**预计时间**: 15-20 分钟

---

## 🎯 前置条件

确保以下软件已安装：

| 软件 | 版本要求 | 验证命令 |
|------|---------|---------|
| JDK | 21+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 18+ | `node -version` |
| Docker | 24+ | `docker -version` |
| Docker Compose | 2.20+ | `docker-compose -version` |

---

## 🚀 快速启动步骤

### 步骤 1: 克隆项目 (如果还没有)

```powershell
git clone https://github.com/gxj1134506645/open-iot.git
cd open-iot
```

### 步骤 2: 启动基础设施 (5-10 分钟)

```powershell
# 进入 Docker 目录
cd infrastructure/docker

# 启动所有基础设施服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 返回项目根目录
cd ../..
```

**验证服务启动**:

访问以下地址确认服务正常：

- ✅ Nacos: http://localhost:8848/nacos (账号: nacos/nacos)
- ✅ EMQX: http://localhost:18083 (账号: admin/public)

### 步骤 3: 构建后端项目 (3-5 分钟)

```powershell
# 进入后端目录
cd backend

# 编译项目（跳过测试）
mvn clean package -DskipTests

# 返回项目根目录
cd ..
```

**预期结果**: 所有模块编译成功，生成 JAR 包在 `target/` 目录

### 步骤 4: 启动后端服务 (2-3 分钟)

**方式一：使用脚本（推荐）**

```powershell
# Windows PowerShell
.\scripts\deploy.ps1 -Services
```

**方式二：手动启动**

```powershell
# 打开 5 个 PowerShell 窗口，分别执行：

# 窗口 1: 网关服务
cd backend
java -jar gateway-service/target/gateway-service.jar

# 窗口 2: 租户服务
cd backend
java -jar tenant-service/target/tenant-service.jar

# 窗口 3: 设备服务
cd backend
java -jar device-service/target/device-service.jar

# 窗口 4: 接入服务
cd backend
java -jar connect-service/target/connect-service.jar

# 窗口 5: 数据服务
cd backend
java -jar data-service/target/data-service.jar
```

**验证服务启动**:

1. 查看各服务控制台日志，确认无错误
2. 访问 Nacos 控制台，确认 5 个服务已注册

### 步骤 5: 启动前端 (1-2 分钟)

```powershell
# 进入前端目录
cd frontend

# 安装依赖（首次启动）
npm install

# 启动开发服务器
npm run dev
```

**访问前端**: http://localhost:5173

---

## ✅ 功能验证清单

### 1. 验证微服务注册 (1 分钟)

访问 Nacos 控制台: http://localhost:8848/nacos

**预期结果**:
- ✅ gateway-service
- ✅ tenant-service
- ✅ device-service
- ✅ connect-service
- ✅ data-service

### 2. 验证网关路由 (1 分钟)

```powershell
# 测试网关健康检查
curl http://localhost:8080/actuator/health

# 测试租户服务路由
curl http://localhost:8080/api/v1/tenants
```

**预期结果**: 返回 JSON 响应，状态码 200

### 3. 验证数据库初始化 (1 分钟)

```powershell
# 连接 PostgreSQL
psql -h localhost -U openiot -d openiot

# 查询表结构
\dt

# 退出
\q
```

**预期结果**:
- ✅ tenant 表
- ✅ sys_user 表
- ✅ device 表
- ✅ device_trajectory 表

### 4. 验证用户登录 (2 分钟)

```powershell
# 使用预置的平台管理员账号登录
curl -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"admin123"}'
```

**预期结果**: 返回 Token

### 5. 创建测试租户 (2 分钟)

```powershell
# 创建租户
curl -X POST http://localhost:8080/api/v1/tenants `
  -H "Content-Type: application/json" `
  -H "Authorization: <token>" `
  -d '{
    "tenantCode": "test-tenant",
    "tenantName": "测试租户",
    "contactEmail": "test@example.com"
  }'
```

**预期结果**: 返回租户 ID

### 6. 创建测试设备 (2 分钟)

```powershell
# 创建设备
curl -X POST http://localhost:8080/api/v1/devices `
  -H "Content-Type: application/json" `
  -H "Authorization: <token>" `
  -d '{
    "deviceCode": "test-device-001",
    "deviceName": "测试设备001",
    "protocolType": "MQTT"
  }'
```

**预期结果**: 返回设备 ID 和 Token

### 7. 验证 MQTT 设备接入 (3 分钟)

**使用 MQTTX 工具**:

1. 下载并安装 [MQTTX](https://mqttx.app/)
2. 创建连接:
   - Name: open-iot-test
   - Host: localhost
   - Port: 1883
   - Username: test-device-001 (设备编码)
   - Password: <device-token> (设备 Token)
3. 发布消息:
   - Topic: `device/telemetry`
   - Payload:
     ```json
     {
       "deviceId": "test-device-001",
       "eventType": "TELEMETRY",
       "payload": {
         "latitude": 30.5,
         "longitude": 120.1,
         "speed": 25.5
       },
       "timestamp": 1709012345678
     }
     ```

**验证数据流**:

```powershell
# 1. 查看 Kafka Topic
docker exec -it open-iot-kafka kafka-console-consumer.sh `
  --bootstrap-server localhost:9092 `
  --topic device-events `
  --from-beginning

# 2. 查看 Redis 实时数据
docker exec -it open-iot-redis redis-cli -a openiot123
> GET device:status:<tenantId>:test-device-001

# 3. 查看 MongoDB 原始数据
docker exec -it open-iot-mongo mongosh
> use openiot
> db.raw_events.find().limit(10)

# 4. 查看 PostgreSQL 解析结果
psql -h localhost -U openiot -d openiot
SELECT * FROM device_trajectory ORDER BY event_time DESC LIMIT 10;
```

### 8. 验证前端实时展示 (2 分钟)

1. 打开浏览器访问: http://localhost:5173
2. 使用租户管理员账号登录
3. 进入"设备监控"页面
4. 观察设备在线状态和轨迹更新

**预期结果**:
- ✅ 设备在线状态显示为"在线"
- ✅ 轨迹点在地图上正确显示
- ✅ 数据更新延迟 < 5 秒

---

## 🔧 故障排查

### 问题 1: Docker 服务启动失败

**症状**: `docker-compose up -d` 报错

**解决方案**:
```powershell
# 查看日志
docker-compose logs

# 重启特定服务
docker-compose restart <service-name>

# 完全清理并重启
docker-compose down
docker-compose up -d
```

### 问题 2: 后端服务无法连接 Nacos

**症状**: 服务启动报错 `Connection refused: nacos`

**解决方案**:
1. 确认 Nacos 已启动: `docker ps | grep nacos`
2. 等待 Nacos 完全启动（约 1-2 分钟）
3. 重启后端服务

### 问题 3: 数据库连接失败

**症状**: 服务启动报错 `Connection refused: postgres`

**解决方案**:
```powershell
# 检查 PostgreSQL 容器状态
docker ps | grep postgres

# 重启 PostgreSQL
docker-compose restart postgres

# 等待 30 秒后重启后端服务
```

### 问题 4: 前端无法访问后端 API

**症状**: 前端控制台报错 `Network Error` 或 `CORS`

**解决方案**:
1. 确认网关服务已启动
2. 检查前端配置的 API 地址
3. 检查浏览器控制台的具体错误信息

### 问题 5: Kafka 消费无数据

**症状**: Kafka Topic 无消息

**解决方案**:
```powershell
# 检查 Kafka 状态
docker-compose ps kafka

# 检查 Topic 列表
docker exec -it open-iot-kafka kafka-topics.sh `
  --bootstrap-server localhost:9092 --list

# 手动创建 Topic
docker exec -it open-iot-kafka kafka-topics.sh `
  --bootstrap-server localhost:9092 `
  --create --topic device-events `
  --partitions 3 --replication-factor 1
```

---

## 🛑 停止服务

### 停止后端服务

```powershell
# 方式一：使用脚本
.\scripts\deploy.ps1 -Stop

# 方式二：手动停止
# 在各服务窗口按 Ctrl+C
```

### 停止前端服务

```powershell
# 在前端窗口按 Ctrl+C
```

### 停止基础设施

```powershell
cd infrastructure/docker
docker-compose down

# 如需删除数据卷
docker-compose down -v
```

---

## 📊 性能验证 (可选)

### 实时延迟测试

```powershell
# 使用 JMeter 或自定义脚本
# 1. 模拟 100 个设备上报数据
# 2. 记录上报时间和前端接收时间
# 3. 计算 P95 延迟

# 预期结果: P95 延迟 <= 3 秒
```

### 并发连接测试

```powershell
# 使用 JMeter + MQTT 插件
# 1. 模拟 1000 个 MQTT 设备同时连接
# 2. 每个设备每秒上报 1 条数据
# 3. 持续运行 10 分钟

# 预期结果:
# - 连接成功率 >= 99%
# - 消息不丢失
# - 系统稳定运行 10 分钟
```

---

## 🎉 验证完成

恭喜！如果以上验证全部通过，说明 Open-IoT MVP 核心功能已成功部署。

### 下一步建议

1. **深入体验**: 尝试更多设备和场景
2. **监控查看**: 访问 Grafana 查看监控指标
3. **日志分析**: 使用 Loki 查询日志
4. **链路追踪**: 在 Tempo 中查看调用链

---

**文档版本**: 1.0
**更新日期**: 2026-03-02

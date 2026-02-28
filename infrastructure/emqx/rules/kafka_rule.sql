-- =====================================================
-- EMQX Rule Engine 配置：MQTT 消息转发到 Kafka
-- =====================================================
-- 功能说明：
-- 1. 当设备通过 MQTT 上报数据时，EMQX 自动将消息转发到 Kafka
-- 2. Kafka Topic 格式：device.events.{tenant_id}
-- 3. 消息格式：EventEnvelope JSON
-- =====================================================

-- 规则 1：设备数据上报（所有租户）
-- 监听 Topic: device/+/data
-- 转发到 Kafka Topic: device.events.{tenant_id}

SELECT
  -- 基础信息
  clientid as clientId,
  username as deviceCode,
  topic as mqttTopic,
  qos as qos,

  -- 租户 ID（从 clientid 中提取，格式：tenant_{tenantId}_device_{deviceCode}）
  -- 或者从设备认证结果中获取（EMQX 5.x 支持）
  json_decode(payload).tenant_id as tenantId,

  -- 设备数据
  payload as payload,

  -- 时间戳
  timestamp as timestamp,

  -- 元数据
  metadata as metadata
FROM
  "device/+/data"
WHERE
  -- 只处理 JSON 格式的消息
  is_json_payload(payload)

-- Kafka 动作配置（需要在 EMQX 管理后台配置）
-- FORWARDED TO:
-- Kafka Topic: device.events.${tenant_id}
-- Partition Key: ${deviceCode}
-- Message Key: ${deviceCode}
-- Message Value: JSON String


-- =====================================================
-- 规则 2：设备轨迹数据上报（专用 Topic）
-- 监听 Topic: device/+/trajectory
-- 转发到 Kafka Topic: device.trajectory.{tenant_id}
-- =====================================================

SELECT
  clientid as clientId,
  username as deviceCode,
  json_decode(payload).tenant_id as tenantId,

  -- 轨迹数据
  json_decode(payload).latitude as latitude,
  json_decode(payload).longitude as longitude,
  json_decode(payload).altitude as altitude,
  json_decode(payload).speed as speed,
  json_decode(payload).direction as direction,
  json_decode(payload).timestamp as eventTime,

  payload as payload,
  timestamp as timestamp
FROM
  "device/+/trajectory"
WHERE
  is_json_payload(payload)

-- FORWARDED TO:
-- Kafka Topic: device.trajectory.${tenant_id}


-- =====================================================
-- 规则 3：设备状态上报（在线/离线/告警）
-- 监听 Topic: device/+/status
-- 转发到 Kafka Topic: device.status.{tenant_id}
-- =====================================================

SELECT
  clientid as clientId,
  username as deviceCode,
  json_decode(payload).tenant_id as tenantId,

  -- 状态数据
  json_decode(payload).status as status,
  json_decode(payload).errorCode as errorCode,
  json_decode(payload).errorMessage as errorMessage,

  payload as payload,
  timestamp as timestamp
FROM
  "device/+/status"
WHERE
  is_json_payload(payload)

-- FORWARDED TO:
-- Kafka Topic: device.status.${tenant_id}


-- =====================================================
-- EMQX Webhook 认证配置
-- =====================================================
-- 认证接口：POST http://device-service:8083/api/device/mqtt/auth
-- 请求体：{"username": "deviceCode", "password": "deviceToken", "clientid": "clientId"}
-- 返回值：{"result": "allow"} 或 {"result": "deny"}

-- 认证器配置示例（EMQX 5.x）：
-- authentication.1.mechanism = password_based
-- authentication.1.backend = http
-- authentication.1.method = post
-- authentication.1.url = http://device-service:8083/api/device/mqtt/auth
-- authentication.1.headers.content-type = application/json
-- authentication.1.body.username = ${username}
-- authentication.1.body.password = ${password}
-- authentication.1.body.clientid = ${clientid}


-- =====================================================
-- EMQX Webhook 连接/断开回调配置
-- =====================================================
-- 连接成功回调：POST http://device-service:8083/api/device/mqtt/connected
-- 断开连接回调：POST http://device-service:8083/api/device/mqtt/disconnected


-- =====================================================
-- 设备上报数据格式示例
-- =====================================================

-- 示例 1：设备数据上报
-- Topic: device/DEVICE001/data
-- Payload:
-- {
--   "tenant_id": "1001",
--   "device_code": "DEVICE001",
--   "event_type": "sensor_data",
--   "data": {
--     "temperature": 25.5,
--     "humidity": 60.0
--   },
--   "timestamp": 1640000000000
-- }

-- 示例 2：设备轨迹上报
-- Topic: device/DEVICE001/trajectory
-- Payload:
-- {
--   "tenant_id": "1001",
--   "device_code": "DEVICE001",
--   "latitude": 39.9042,
--   "longitude": 116.4074,
--   "altitude": 50.0,
--   "speed": 60.0,
--   "direction": 180,
--   "timestamp": 1640000000000
-- }

-- 示例 3：设备状态上报
-- Topic: device/DEVICE001/status
-- Payload:
-- {
--   "tenant_id": "1001",
--   "device_code": "DEVICE001",
--   "status": "online",
--   "timestamp": 1640000000000
-- }

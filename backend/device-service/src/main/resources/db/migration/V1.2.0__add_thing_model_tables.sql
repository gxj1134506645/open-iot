-- ========================================
-- 物模型功能表
-- Version: 1.2.0
-- Description: 创建产品属性、事件、服务定义表
-- ========================================

-- ========================================
-- 1. 产品属性表
-- ========================================
CREATE TABLE IF NOT EXISTS product_property (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    property_identifier VARCHAR(50) NOT NULL,  -- 属性标识符（如 temperature）
    property_name VARCHAR(100) NOT NULL,        -- 属性名称（如 温度）
    data_type VARCHAR(20) NOT NULL,             -- 数据类型（int/float/enum/bool/string/text/date/json）
    spec JSONB,                                  -- 属性规格（取值范围、单位等）
    read_write_flag CHAR(1) DEFAULT 'r',         -- 读写标识：r-只读，rw-读写，w-只写
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE product_property IS '产品属性定义表';
COMMENT ON COLUMN product_property.id IS '主键';
COMMENT ON COLUMN product_property.tenant_id IS '租户ID';
COMMENT ON COLUMN product_property.product_id IS '产品ID';
COMMENT ON COLUMN product_property.property_identifier IS '属性标识符（英文，用于设备通信）';
COMMENT ON COLUMN product_property.property_name IS '属性名称（中文，用于展示）';
COMMENT ON COLUMN product_property.data_type IS '数据类型：int/float/enum/bool/string/text/date/json';
COMMENT ON COLUMN product_property.spec IS '属性规格定义（JSON格式）：{min, max, unit, enumValues}';
COMMENT ON COLUMN product_property.read_write_flag IS '读写标识：r-只读，rw-读写，w-只写';
COMMENT ON COLUMN product_property.delete_flag IS '删除标记：0-正常，1-已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_property_product ON product_property(product_id, delete_flag);
CREATE UNIQUE INDEX IF NOT EXISTS uk_property_identifier ON product_property(product_id, property_identifier, delete_flag);

-- ========================================
-- 2. 产品事件表
-- ========================================
CREATE TABLE IF NOT EXISTS product_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    event_identifier VARCHAR(50) NOT NULL,      -- 事件标识符（如 high_temperature_alert）
    event_name VARCHAR(100) NOT NULL,           -- 事件名称（如 高温告警）
    event_type VARCHAR(20) NOT NULL,            -- 事件类型：info-信息，warn-告警，error-故障
    event_level VARCHAR(20),                     -- 事件级别（用于告警分级）
    params JSONB,                                -- 事件参数定义（JSON格式）
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE product_event IS '产品事件定义表';
COMMENT ON COLUMN product_event.id IS '主键';
COMMENT ON COLUMN product_event.tenant_id IS '租户ID';
COMMENT ON COLUMN product_event.product_id IS '产品ID';
COMMENT ON COLUMN product_event.event_identifier IS '事件标识符（英文）';
COMMENT ON COLUMN product_event.event_name IS '事件名称（中文）';
COMMENT ON COLUMN product_event.event_type IS '事件类型：info-信息，warn-告警，error-故障';
COMMENT ON COLUMN product_event.event_level IS '事件级别（用于告警策略）';
COMMENT ON COLUMN product_event.params IS '事件参数定义（JSON）：[{identifier, name, dataType}]';
COMMENT ON COLUMN product_event.delete_flag IS '删除标记：0-正常，1-已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_event_product ON product_event(product_id, delete_flag);
CREATE UNIQUE INDEX IF NOT EXISTS uk_event_identifier ON product_event(product_id, event_identifier, delete_flag);

-- ========================================
-- 3. 产品服务表
-- ========================================
CREATE TABLE IF NOT EXISTS product_service (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    service_identifier VARCHAR(50) NOT NULL,    -- 服务标识符（如 switch）
    service_name VARCHAR(100) NOT NULL,         -- 服务名称（如 开关）
    call_type VARCHAR(20) NOT NULL,             -- 调用方式：sync-同步，async-异步
    input_params JSONB,                         -- 输入参数定义（JSON）
    output_params JSONB,                        -- 输出参数定义（JSON）
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE product_service IS '产品服务定义表';
COMMENT ON COLUMN product_service.id IS '主键';
COMMENT ON COLUMN product_service.tenant_id IS '租户ID';
COMMENT ON COLUMN product_service.product_id IS '产品ID';
COMMENT ON COLUMN product_service.service_identifier IS '服务标识符（英文）';
COMMENT ON COLUMN product_service.service_name IS '服务名称（中文）';
COMMENT ON COLUMN product_service.call_type IS '调用方式：sync-同步，async-异步';
COMMENT ON COLUMN product_service.input_params IS '输入参数定义（JSON）：[{identifier, name, dataType}]';
COMMENT ON COLUMN product_service.output_params IS '输出参数定义（JSON）：[{identifier, name, dataType}]';
COMMENT ON COLUMN product_service.delete_flag IS '删除标记：0-正常，1-已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_service_product ON product_service(product_id, delete_flag);
CREATE UNIQUE INDEX IF NOT EXISTS uk_service_identifier ON product_service(product_id, service_identifier, delete_flag);

-- ========================================
-- 4. 物模型示例数据（JSON格式存储在 product.thing_model）
-- ========================================
-- 示例：温度传感器物模型
/*
{
  "properties": [
    {
      "identifier": "temperature",
      "name": "温度",
      "dataType": "float",
      "spec": {
        "min": -40,
        "max": 120,
        "unit": "℃",
        "step": 0.1
      },
      "readWriteFlag": "r"
    },
    {
      "identifier": "humidity",
      "name": "湿度",
      "dataType": "float",
      "spec": {
        "min": 0,
        "max": 100,
        "unit": "%",
        "step": 0.1
      },
      "readWriteFlag": "r"
    }
  ],
  "events": [
    {
      "identifier": "high_temperature_alert",
      "name": "高温告警",
      "type": "warn",
      "level": "critical",
      "params": [
        {
          "identifier": "temperature",
          "name": "当前温度",
          "dataType": "float"
        },
        {
          "identifier": "threshold",
          "name": "告警阈值",
          "dataType": "float"
        }
      ]
    }
  ],
  "services": [
    {
      "identifier": "setReportInterval",
      "name": "设置上报间隔",
      "callType": "sync",
      "inputParams": [
        {
          "identifier": "interval",
          "name": "上报间隔（秒）",
          "dataType": "int",
          "spec": {
            "min": 1,
            "max": 3600
          }
        }
      ],
      "outputParams": [
        {
          "identifier": "code",
          "name": "返回码",
          "dataType": "int"
        },
        {
          "identifier": "message",
          "name": "返回消息",
          "dataType": "string"
        }
      ]
    }
  ]
}
*/

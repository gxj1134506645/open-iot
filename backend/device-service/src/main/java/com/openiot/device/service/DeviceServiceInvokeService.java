package com.openiot.device.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.DeviceServiceInvoke;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.DeviceServiceInvokeMapper;
import com.openiot.device.vo.ServiceInvokeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 设备服务调用服务
 *
 * 提供设备服务的同步/异步调用功能：
 * - 同步调用：发起调用后阻塞等待响应，最多 30 秒
 * - 异步调用：立即返回 invokeId，通过轮询查询结果
 * - 离线设备：直接拒绝调用
 * - 超时处理：30 秒未响应标记为 TIMEOUT
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceInvokeService extends ServiceImpl<DeviceServiceInvokeMapper, DeviceServiceInvoke> {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final ThingModelService thingModelService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 调用状态常量
     */
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_CALLING = "calling";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_TIMEOUT = "timeout";

    /**
     * 调用类型常量
     */
    private static final String INVOKE_TYPE_SYNC = "sync";
    private static final String INVOKE_TYPE_ASYNC = "async";

    /**
     * Kafka 主题
     */
    private static final String TOPIC_SERVICE_INVOKE = "device-service-invoke";
    private static final String TOPIC_SERVICE_RESPONSE = "device-service-response";

    /**
     * Redis Key 前缀
     */
    private static final String REDIS_KEY_PREFIX = "device:service:invoke:";

    /**
     * 存储等待响应的 CompletableFuture（用于同步调用）
     * Key: invokeId
     * Value: CompletableFuture<DeviceServiceInvoke>
     */
    private final ConcurrentHashMap<String, CompletableFuture<DeviceServiceInvoke>> pendingInvocations = new ConcurrentHashMap<>();

    /**
     * 同步调用设备服务
     *
     * @param deviceId          设备 ID
     * @param serviceIdentifier 服务标识符
     * @param inputParams       输入参数
     * @return 调用结果（包含设备响应）
     */
    public ServiceInvokeVO invokeServiceSync(Long deviceId, String serviceIdentifier, Map<String, Object> inputParams) {
        return invokeServiceSync(deviceId, serviceIdentifier, inputParams, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 同步调用设备服务（指定超时时间）
     *
     * @param deviceId          设备 ID
     * @param serviceIdentifier 服务标识符
     * @param inputParams       输入参数
     * @param timeoutSeconds    超时时间（秒）
     * @return 调用结果（包含设备响应）
     */
    public ServiceInvokeVO invokeServiceSync(Long deviceId, String serviceIdentifier,
                                             Map<String, Object> inputParams, int timeoutSeconds) {
        log.info("同步调用设备服务: deviceId={}, service={}, timeout={}s", deviceId, serviceIdentifier, timeoutSeconds);

        // 1. 创建调用记录（异步模式，但会同步等待）
        DeviceServiceInvoke invoke = createInvocation(deviceId, serviceIdentifier, inputParams, INVOKE_TYPE_SYNC);

        // 2. 检查设备在线状态
        checkDeviceOnline(deviceId);

        // 3. 发送调用命令
        sendServiceInvokeCommand(invoke);

        // 4. 创建 CompletableFuture 等待响应
        CompletableFuture<DeviceServiceInvoke> future = new CompletableFuture<>();
        pendingInvocations.put(invoke.getInvokeId(), future);

        try {
            // 5. 阻塞等待响应（最多 timeoutSeconds 秒）
            DeviceServiceInvoke result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return convertToVO(result);
        } catch (TimeoutException e) {
            // 超时处理
            log.warn("服务调用超时: invokeId={}", invoke.getInvokeId());
            handleTimeout(invoke.getInvokeId());
            throw BusinessException.badRequest("服务调用超时，设备未在 " + timeoutSeconds + " 秒内响应");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw BusinessException.badRequest("服务调用被中断");
        } catch (ExecutionException e) {
            log.error("服务调用异常: invokeId={}", invoke.getInvokeId(), e);
            throw BusinessException.badRequest("服务调用失败: " + e.getMessage());
        } finally {
            // 清理
            pendingInvocations.remove(invoke.getInvokeId());
        }
    }

    /**
     * 异步调用设备服务
     *
     * @param deviceId          设备 ID
     * @param serviceIdentifier 服务标识符
     * @param inputParams       输入参数
     * @return 调用 ID（用于查询调用状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceInvokeVO invokeServiceAsync(Long deviceId, String serviceIdentifier, Map<String, Object> inputParams) {
        log.info("异步调用设备服务: deviceId={}, service={}", deviceId, serviceIdentifier);

        // 1. 创建调用记录
        DeviceServiceInvoke invoke = createInvocation(deviceId, serviceIdentifier, inputParams, INVOKE_TYPE_ASYNC);

        // 2. 检查设备在线状态
        checkDeviceOnline(deviceId);

        // 3. 发送调用命令
        sendServiceInvokeCommand(invoke);

        // 4. 立即返回调用 ID
        return convertToVO(invoke);
    }

    /**
     * 查询调用状态
     *
     * @param invokeId 调用 ID
     * @return 调用记录
     */
    public ServiceInvokeVO queryInvocationStatus(String invokeId) {
        DeviceServiceInvoke invoke = lambdaQuery()
                .eq(DeviceServiceInvoke::getInvokeId, invokeId)
                .one();

        if (invoke == null) {
            throw BusinessException.notFound("调用记录不存在: " + invokeId);
        }

        // 检查租户权限
        checkTenantAccess(invoke);

        return convertToVO(invoke);
    }

    /**
     * 处理设备服务响应（由 Kafka 消费者或 MQTT 处理器调用）
     *
     * @param invokeId    调用 ID
     * @param outputParams 输出参数（设备响应）
     * @param success     是否成功
     * @param errorMessage 错误消息（失败时）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleDeviceServiceResponse(String invokeId, Map<String, Object> outputParams,
                                            boolean success, String errorMessage) {
        log.info("处理设备服务响应: invokeId={}, success={}", invokeId, success);

        DeviceServiceInvoke invoke = lambdaQuery()
                .eq(DeviceServiceInvoke::getInvokeId, invokeId)
                .one();

        if (invoke == null) {
            log.warn("调用记录不存在: invokeId={}", invokeId);
            return;
        }

        // 更新状态
        invoke.setStatus(success ? STATUS_SUCCESS : STATUS_FAILED);
        invoke.setCompleteTime(LocalDateTime.now());

        if (outputParams != null) {
            try {
                invoke.setOutputData(objectMapper.writeValueAsString(outputParams));
            } catch (Exception e) {
                log.error("序列化输出参数失败", e);
            }
        }

        if (!success && errorMessage != null) {
            invoke.setErrorMessage(errorMessage);
        }

        this.updateById(invoke);

        // 如果是同步调用，唤醒等待的线程
        CompletableFuture<DeviceServiceInvoke> future = pendingInvocations.remove(invokeId);
        if (future != null) {
            future.complete(invoke);
        }

        log.info("服务调用完成: invokeId={}, status={}", invokeId, invoke.getStatus());
    }

    /**
     * 处理超时
     *
     * @param invokeId 调用 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeout(String invokeId) {
        DeviceServiceInvoke invoke = lambdaQuery()
                .eq(DeviceServiceInvoke::getInvokeId, invokeId)
                .one();

        if (invoke == null) {
            return;
        }

        // 只有 pending 或 calling 状态才能标记超时
        if (STATUS_PENDING.equals(invoke.getStatus()) || STATUS_CALLING.equals(invoke.getStatus())) {
            invoke.setStatus(STATUS_TIMEOUT);
            invoke.setCompleteTime(LocalDateTime.now());
            invoke.setErrorMessage("调用超时，设备未在规定时间内响应");
            this.updateById(invoke);

            log.warn("服务调用超时: invokeId={}", invokeId);
        }
    }

    /**
     * 创建调用记录
     *
     * @param deviceId          设备 ID
     * @param serviceIdentifier 服务标识符
     * @param inputParams       输入参数
     * @param invokeType        调用类型
     * @return 调用记录
     */
    @Transactional(rollbackFor = Exception.class)
    protected DeviceServiceInvoke createInvocation(Long deviceId, String serviceIdentifier,
                                                   Map<String, Object> inputParams, String invokeType) {
        // 1. 查询设备信息
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        // 2. 检查设备状态
        if ("0".equals(device.getStatus())) {
            throw BusinessException.badRequest("设备已禁用，无法调用服务");
        }

        // 3. 查询产品和服务定义
        Product product = null;
        String serviceName = serviceIdentifier;
        String callType = invokeType;

        if (device.getProductId() != null) {
            product = productService.getById(device.getProductId());
            if (product != null && product.getThingModel() != null) {
                // 从物模型中获取服务定义
                JsonNode serviceDef = thingModelService.getServiceDefinition(product, serviceIdentifier);
                if (serviceDef != null) {
                    if (serviceDef.has("name")) {
                        serviceName = serviceDef.get("name").asText();
                    }
                    if (serviceDef.has("callType")) {
                        callType = serviceDef.get("callType").asText();
                    }

                    // 验证输入参数
                    validateInputParams(serviceDef, inputParams);
                } else {
                    log.warn("服务定义不存在: service={}, product={}", serviceIdentifier, product.getProductKey());
                }
            }
        }

        // 4. 创建调用记录
        DeviceServiceInvoke invoke = new DeviceServiceInvoke();
        invoke.setTenantId(device.getTenantId());
        invoke.setDeviceId(deviceId);
        invoke.setServiceIdentifier(serviceIdentifier);
        invoke.setInvokeId(generateInvokeId());
        invoke.setInvokeType(callType);
        invoke.setStatus(STATUS_PENDING);
        invoke.setInvokeTime(LocalDateTime.now());

        if (inputParams != null) {
            try {
                invoke.setInputData(objectMapper.writeValueAsString(inputParams));
            } catch (Exception e) {
                log.error("序列化输入参数失败", e);
            }
        }

        this.save(invoke);

        log.info("创建服务调用记录: invokeId={}, device={}, service={}",
                invoke.getInvokeId(), device.getDeviceCode(), serviceIdentifier);

        return invoke;
    }

    /**
     * 发送服务调用命令到 Kafka
     *
     * @param invoke 调用记录
     */
    private void sendServiceInvokeCommand(DeviceServiceInvoke invoke) {
        try {
            Device device = deviceService.getById(invoke.getDeviceId());
            Product product = device.getProductId() != null ? productService.getById(device.getProductId()) : null;

            // 更新状态为调用中
            invoke.setStatus(STATUS_CALLING);
            this.updateById(invoke);

            // 构造调用消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "service_invoke");
            message.put("invokeId", invoke.getInvokeId());
            message.put("deviceId", invoke.getDeviceId());
            message.put("deviceKey", device.getDeviceKey());
            message.put("productKey", product != null ? product.getProductKey() : null);
            message.put("serviceIdentifier", invoke.getServiceIdentifier());
            message.put("inputParams", invoke.getInputData() != null ?
                    objectMapper.readValue(invoke.getInputData(), Map.class) : null);
            message.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(TOPIC_SERVICE_INVOKE, invoke.getInvokeId(), jsonMessage);

            log.info("发送服务调用命令: invokeId={}, topic={}", invoke.getInvokeId(), TOPIC_SERVICE_INVOKE);

        } catch (Exception e) {
            log.error("发送服务调用命令失败: invokeId={}", invoke.getInvokeId(), e);

            // 更新状态为失败
            invoke.setStatus(STATUS_FAILED);
            invoke.setErrorMessage("发送调用命令失败: " + e.getMessage());
            invoke.setCompleteTime(LocalDateTime.now());
            this.updateById(invoke);

            throw BusinessException.internalError("发送服务调用命令失败: " + e.getMessage());
        }
    }

    /**
     * 检查设备在线状态
     *
     * @param deviceId 设备 ID
     */
    private void checkDeviceOnline(Long deviceId) {
        // 从 Redis 检查设备在线状态
        String onlineKey = "device:online:" + deviceId;
        Boolean online = redisTemplate.hasKey(onlineKey);

        if (online == null || !online) {
            log.warn("设备离线，拒绝服务调用: deviceId={}", deviceId);
            throw BusinessException.badRequest("设备离线，无法调用服务");
        }
    }

    /**
     * 验证输入参数
     *
     * @param serviceDef  服务定义
     * @param inputParams 输入参数
     */
    private void validateInputParams(JsonNode serviceDef, Map<String, Object> inputParams) {
        JsonNode inputParamsDef = serviceDef.get("inputParams");
        if (inputParamsDef == null || !inputParamsDef.isArray()) {
            return; // 没有定义输入参数，跳过验证
        }

        for (JsonNode paramDef : inputParamsDef) {
            String identifier = paramDef.has("identifier") ? paramDef.get("identifier").asText() : null;
            if (identifier == null) {
                continue;
            }

            // 检查必填参数
            boolean required = paramDef.has("required") && paramDef.get("required").asBoolean();
            if (required && (inputParams == null || !inputParams.containsKey(identifier))) {
                String name = paramDef.has("name") ? paramDef.get("name").asText() : identifier;
                throw BusinessException.badRequest("缺少必填参数: " + name);
            }

            // 验证参数类型（简化验证）
            if (inputParams != null && inputParams.containsKey(identifier)) {
                Object value = inputParams.get(identifier);
                String dataType = paramDef.has("dataType") ? paramDef.get("dataType").asText() : "string";
                validateParamType(identifier, value, dataType);
            }
        }
    }

    /**
     * 验证参数类型
     */
    private void validateParamType(String identifier, Object value, String dataType) {
        if (value == null) {
            return;
        }

        switch (dataType.toLowerCase()) {
            case "int":
            case "long":
                if (!(value instanceof Number)) {
                    throw BusinessException.badRequest("参数 '" + identifier + "' 应为整数类型");
                }
                break;
            case "float":
            case "double":
                if (!(value instanceof Number)) {
                    throw BusinessException.badRequest("参数 '" + identifier + "' 应为浮点类型");
                }
                break;
            case "boolean":
                if (!(value instanceof Boolean)) {
                    throw BusinessException.badRequest("参数 '" + identifier + "' 应为布尔类型");
                }
                break;
            case "string":
                // 字符串类型不做限制
                break;
            default:
                // 其他类型不做验证
                break;
        }
    }

    /**
     * 生成调用 ID
     */
    private String generateInvokeId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 检查租户访问权限
     */
    private void checkTenantAccess(DeviceServiceInvoke invoke) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(String.valueOf(invoke.getTenantId()))) {
            log.warn("跨租户访问被拒绝: current={}, target={}", tenantId, invoke.getTenantId());
            throw BusinessException.forbidden("无权访问该调用记录");
        }
    }

    /**
     * 转换为 VO
     */
    private ServiceInvokeVO convertToVO(DeviceServiceInvoke invoke) {
        ServiceInvokeVO vo = new ServiceInvokeVO();
        vo.setId(invoke.getId());
        vo.setInvokeId(invoke.getInvokeId());
        vo.setDeviceId(invoke.getDeviceId());
        vo.setServiceIdentifier(invoke.getServiceIdentifier());
        vo.setInvokeType(invoke.getInvokeType());
        vo.setStatus(invoke.getStatus());
        vo.setErrorMessage(invoke.getErrorMessage());
        vo.setInvokeTime(invoke.getInvokeTime());
        vo.setCompleteTime(invoke.getCompleteTime());
        vo.setCreateTime(invoke.getInvokeTime()); // 使用 invokeTime 作为创建时间

        // 解析输入参数
        if (invoke.getInputData() != null) {
            try {
                vo.setInputParams(objectMapper.readTree(invoke.getInputData()));
            } catch (Exception e) {
                log.error("解析输入参数失败", e);
            }
        }

        // 解析输出参数
        if (invoke.getOutputData() != null) {
            try {
                vo.setOutputParams(objectMapper.readTree(invoke.getOutputData()));
            } catch (Exception e) {
                log.error("解析输出参数失败", e);
            }
        }

        return vo;
    }
}

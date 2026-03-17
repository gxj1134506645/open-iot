package com.openiot.rule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.redis.util.RedisUtil;
import com.openiot.common.security.context.TenantContext;
import com.openiot.rule.entity.ParseRule;
import com.openiot.rule.mapper.ParseRuleMapper;
import com.openiot.rule.vo.ParseRuleCreateVO;
import com.openiot.rule.vo.ParseTestRequestVO;
import com.openiot.rule.vo.ParseTestResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 解析规则服务
 * 负责解析规则的 CRUD 操作和测试功能
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseRuleService extends ServiceImpl<ParseRuleMapper, ParseRule> {

    private final RedisUtil redisUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis 发布/订阅通道名称（用于通知 connect-service 刷新缓存）
     */
    private static final String PARSE_RULE_UPDATE_CHANNEL = "openiot:parse_rule:update";

    /**
     * 创建解析规则
     *
     * @param vo 创建请求 VO
     * @return 创建的规则实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ParseRule createParseRule(ParseRuleCreateVO vo) {
        // 获取租户ID
        Long tenantId = getTenantId();

        // 验证规则配置格式
        validateRuleConfig(vo.getRuleType(), vo.getRuleConfig());

        // 检查同一产品下规则名称是否重复
        if (existsRuleName(tenantId, vo.getProductId(), vo.getRuleName(), null)) {
            throw BusinessException.badRequest("规则名称已存在: " + vo.getRuleName());
        }

        // 创建规则实体
        ParseRule rule = new ParseRule();
        BeanUtils.copyProperties(vo, rule);
        rule.setTenantId(tenantId);

        // 设置默认值
        if (rule.getPriority() == null) {
            rule.setPriority(0);
        }
        if (rule.getStatus() == null) {
            rule.setStatus("1");
        }

        // 保存规则
        this.save(rule);

        log.info("创建解析规则成功: {} (id={}, productId={}, type={})",
                rule.getRuleName(), rule.getId(), rule.getProductId(), rule.getRuleType());

        return rule;
    }

    /**
     * 更新解析规则
     *
     * @param id 规则ID
     * @param vo 更新请求 VO
     * @return 更新后的规则实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ParseRule updateParseRule(Long id, ParseRuleCreateVO vo) {
        // 查询规则
        ParseRule rule = getRuleById(id);

        // 检查租户权限
        checkTenantAccess(rule);

        // 验证规则配置格式
        validateRuleConfig(vo.getRuleType(), vo.getRuleConfig());

        // 检查规则名称是否重复
        if (existsRuleName(rule.getTenantId(), vo.getProductId(), vo.getRuleName(), id)) {
            throw BusinessException.badRequest("规则名称已存在: " + vo.getRuleName());
        }

        // 更新字段
        rule.setProductId(vo.getProductId());
        rule.setRuleName(vo.getRuleName());
        rule.setRuleType(vo.getRuleType());
        rule.setRuleConfig(vo.getRuleConfig());
        if (vo.getPriority() != null) {
            rule.setPriority(vo.getPriority());
        }
        if (vo.getStatus() != null) {
            rule.setStatus(vo.getStatus());
        }

        // 保存更新
        this.updateById(rule);

        // 发布 Redis 消息通知 connect-service 刷新缓存
        publishRuleUpdate(rule);

        log.info("更新解析规则成功: {} (id={})", rule.getRuleName(), id);

        return rule;
    }

    /**
     * 删除解析规则
     *
     * @param id 规则ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteParseRule(Long id) {
        // 查询规则
        ParseRule rule = getRuleById(id);

        // 检查租户权限
        checkTenantAccess(rule);

        // 软删除
        this.removeById(id);

        // 发布 Redis 消息通知 connect-service 刷新缓存
        publishRuleDelete(rule);

        log.info("删除解析规则成功: {} (id={}, productId={})",
                rule.getRuleName(), id, rule.getProductId());
    }

    /**
     * 测试解析规则
     *
     * @param id 规则ID
     * @param vo 测试请求 VO
     * @return 解析结果
     */
    public ParseTestResultVO testParseRule(Long id, ParseTestRequestVO vo) {
        // 查询规则
        ParseRule rule = getRuleById(id);

        // 检查租户权限
        checkTenantAccess(rule);

        return testParseRuleInternal(rule.getRuleType(), rule.getRuleConfig(), vo.getRawData());
    }

    /**
     * 测试解析规则（直接使用规则配置，不需要规则ID）
     *
     * @param ruleType   规则类型
     * @param ruleConfig 规则配置
     * @param rawData    原始数据
     * @return 解析结果
     */
    public ParseTestResultVO testParseRuleInternal(String ruleType, String ruleConfig, String rawData) {
        long startTime = System.currentTimeMillis();

        try {
            // 根据规则类型选择解析器进行测试
            Object result = switch (ruleType) {
                case "JSON" -> testJsonParse(ruleConfig, rawData);
                case "JAVASCRIPT" -> testJavaScriptParse(ruleConfig, rawData);
                case "REGEX" -> testRegexParse(ruleConfig, rawData);
                case "BINARY" -> testBinaryParse(ruleConfig, rawData);
                default -> throw BusinessException.badRequest("不支持的规则类型: " + ruleType);
            };

            long durationMs = System.currentTimeMillis() - startTime;
            log.debug("解析测试成功: type={}, duration={}ms", ruleType, durationMs);

            return ParseTestResultVO.success(result, durationMs);

        } catch (BusinessException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.warn("解析测试失败: type={}, error={}", ruleType, e.getMessage());
            return ParseTestResultVO.fail(e.getMessage(), durationMs);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("解析测试异常: type={}", ruleType, e);
            return ParseTestResultVO.fail("解析异常: " + e.getMessage(), durationMs);
        }
    }

    /**
     * 根据产品ID获取解析规则列表
     *
     * @param productId 产品ID
     * @return 规则列表（按优先级降序）
     */
    public List<ParseRule> getParseRuleByProductId(Long productId) {
        var query = lambdaQuery()
                .eq(ParseRule::getProductId, productId)
                .eq(ParseRule::getStatus, "1");

        // 平台管理员可查看所有数据
        if (!TenantContext.isPlatformAdmin()) {
            Long tenantId = getTenantId();
            query.eq(ParseRule::getTenantId, tenantId);
        }

        return query.orderByDesc(ParseRule::getPriority).list();
    }

    /**
     * 根据ID获取解析规则
     *
     * @param id 规则ID
     * @return 规则实体
     */
    public ParseRule getRuleById(Long id) {
        ParseRule rule = this.getById(id);
        if (rule == null) {
            throw BusinessException.notFound("解析规则不存在: " + id);
        }
        return rule;
    }

    /**
     * 分页查询解析规则
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param productId 产品ID（可选）
     * @param ruleType  规则类型（可选）
     * @param status    状态（可选）
     * @return 分页结果
     */
    public Page<ParseRule> getParseRuleList(int pageNum, int pageSize,
                                            Long productId, String ruleType, String status) {
        Page<ParseRule> page = new Page<>(pageNum, pageSize);

        var query = lambdaQuery();

        // 平台管理员可查看所有数据，租户管理员只能查看自己租户的
        if (!TenantContext.isPlatformAdmin()) {
            Long tenantId = getTenantId();
            query.eq(ParseRule::getTenantId, tenantId);
        }

        return query
                .eq(productId != null, ParseRule::getProductId, productId)
                .eq(ruleType != null && !ruleType.isEmpty(), ParseRule::getRuleType, ruleType)
                .eq(status != null && !status.isEmpty(), ParseRule::getStatus, status)
                .orderByDesc(ParseRule::getPriority)
                .orderByDesc(ParseRule::getCreateTime)
                .page(page);
    }

    /**
     * 启用/禁用解析规则
     *
     * @param id     规则ID
     * @param status 状态：1-启用，0-禁用
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        ParseRule rule = getRuleById(id);
        checkTenantAccess(rule);

        rule.setStatus(status);
        this.updateById(rule);

        // 发布 Redis 消息通知 connect-service 刷新缓存
        publishRuleUpdate(rule);

        log.info("更新解析规则状态: {} (id={}, status={})", rule.getRuleName(), id, status);
    }

    // ==================== 私有方法 ====================

    /**
     * 验证规则配置格式
     */
    private void validateRuleConfig(String ruleType, String ruleConfig) {
        try {
            // 首先验证是否为合法的 JSON
            objectMapper.readTree(ruleConfig);

            // 根据规则类型进行额外验证
            switch (ruleType) {
                case "JSON" -> validateJsonRuleConfig(ruleConfig);
                case "JAVASCRIPT" -> validateJavaScriptRuleConfig(ruleConfig);
                case "REGEX" -> validateRegexRuleConfig(ruleConfig);
                case "BINARY" -> validateBinaryRuleConfig(ruleConfig);
                default -> throw BusinessException.badRequest("不支持的规则类型: " + ruleType);
            }
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("规则配置必须是合法的 JSON 格式");
        }
    }

    /**
     * 验证 JSON 规则配置
     */
    private void validateJsonRuleConfig(String ruleConfig) {
        try {
            var node = objectMapper.readTree(ruleConfig);
            if (!node.has("mappings")) {
                throw BusinessException.badRequest("JSON 规则配置必须包含 'mappings' 字段");
            }
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("JSON 规则配置格式错误");
        }
    }

    /**
     * 验证 JavaScript 规则配置
     */
    private void validateJavaScriptRuleConfig(String ruleConfig) {
        try {
            var node = objectMapper.readTree(ruleConfig);
            if (!node.has("script")) {
                throw BusinessException.badRequest("JavaScript 规则配置必须包含 'script' 字段");
            }
            // 注意：脚本语法验证在执行时进行
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("JavaScript 规则配置格式错误");
        }
    }

    /**
     * 验证正则规则配置
     */
    private void validateRegexRuleConfig(String ruleConfig) {
        try {
            var node = objectMapper.readTree(ruleConfig);
            if (!node.has("pattern")) {
                throw BusinessException.badRequest("REGEX 规则配置必须包含 'pattern' 字段");
            }
            // 验证正则表达式是否合法
            String pattern = node.get("pattern").asText();
            java.util.regex.Pattern.compile(pattern);
        } catch (java.util.regex.PatternSyntaxException e) {
            throw BusinessException.badRequest("正则表达式语法错误: " + e.getMessage());
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("REGEX 规则配置格式错误");
        }
    }

    /**
     * 验证二进制规则配置
     */
    private void validateBinaryRuleConfig(String ruleConfig) {
        try {
            var node = objectMapper.readTree(ruleConfig);
            if (!node.has("fields")) {
                throw BusinessException.badRequest("BINARY 规则配置必须包含 'fields' 字段");
            }
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("BINARY 规则配置格式错误");
        }
    }

    /**
     * 测试 JSON 解析
     */
    private Object testJsonParse(String ruleConfig, String rawData) {
        try {
            var configNode = objectMapper.readTree(ruleConfig);
            var dataNode = objectMapper.readTree(rawData);
            var mappings = configNode.get("mappings");

            java.util.Map<String, Object> result = new java.util.HashMap<>();

            if (mappings != null && mappings.isArray()) {
                for (var mapping : mappings) {
                    String source = mapping.get("source").asText();
                    String target = mapping.get("target").asText();
                    String type = mapping.has("type") ? mapping.get("type").asText() : "string";

                    // 使用 JsonPath 简化版本：支持 $.xxx 格式
                    Object value = extractJsonValue(dataNode, source);
                    if (value != null) {
                        result.put(target, convertType(value, type));
                    }
                }
            }

            return result;
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("JSON 解析失败: " + e.getMessage());
        }
    }

    /**
     * 从 JSON 节点中提取值（简化版 JsonPath）
     */
    private Object extractJsonValue(com.fasterxml.jackson.databind.JsonNode node, String path) {
        if (path.startsWith("$.")) {
            path = path.substring(2);
        }

        String[] parts = path.split("\\.");
        com.fasterxml.jackson.databind.JsonNode current = node;

        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return null;
            }

            // 处理数组索引 [0]
            if (part.contains("[")) {
                String fieldName = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                current = current.get(fieldName);
                if (current != null && current.isArray()) {
                    current = current.get(index);
                }
            } else {
                current = current.get(part);
            }
        }

        if (current == null || current.isMissingNode()) {
            return null;
        }

        if (current.isTextual()) {
            return current.asText();
        } else if (current.isInt()) {
            return current.asInt();
        } else if (current.isLong()) {
            return current.asLong();
        } else if (current.isDouble()) {
            return current.asDouble();
        } else if (current.isBoolean()) {
            return current.asBoolean();
        }

        return current;
    }

    /**
     * 类型转换
     */
    private Object convertType(Object value, String type) {
        if (value == null) {
            return null;
        }

        try {
            return switch (type.toLowerCase()) {
                case "int", "integer" -> {
                    if (value instanceof Number num) {
                        yield num.intValue();
                    }
                    yield Integer.parseInt(value.toString());
                }
                case "long" -> {
                    if (value instanceof Number num) {
                        yield num.longValue();
                    }
                    yield Long.parseLong(value.toString());
                }
                case "double", "float" -> {
                    if (value instanceof Number num) {
                        yield num.doubleValue();
                    }
                    yield Double.parseDouble(value.toString());
                }
                case "boolean", "bool" -> {
                    if (value instanceof Boolean bool) {
                        yield bool;
                    }
                    yield Boolean.parseBoolean(value.toString());
                }
                default -> value.toString(); // string
            };
        } catch (NumberFormatException e) {
            log.warn("类型转换失败: value={}, type={}", value, type);
            return value;
        }
    }

    /**
     * 测试 JavaScript 解析（使用 GraalJS）
     */
    private Object testJavaScriptParse(String ruleConfig, String rawData) {
        try {
            var configNode = objectMapper.readTree(ruleConfig);
            String script = configNode.get("script").asText();

            // 使用 GraalJS 执行脚本（沙箱模式）
            org.graalvm.polyglot.Context context = org.graalvm.polyglot.Context.newBuilder("js")
                    .allowAllAccess(false) // 禁用所有访问权限
                    .allowIO(false) // 禁用 IO
                    .option("js.eval-time-limit", "3") // 3秒超时
                    .build();

            try {
                context.getBindings("js").putMember("rawData", rawData);

                // 执行脚本
                context.eval("js", script);

                // 调用 parse 函数
                var parseFn = context.getBindings("js").getMember("parse");
                if (parseFn == null) {
                    throw BusinessException.badRequest("JavaScript 脚本必须定义 parse(rawData) 函数");
                }

                var result = parseFn.execute(rawData);

                // 转换结果为 Java 对象
                return result.as(java.lang.Object.class);

            } finally {
                context.close();
            }
        } catch (org.graalvm.polyglot.PolyglotException e) {
            if (e.isCancelled()) {
                throw BusinessException.badRequest("JavaScript 执行超时（3秒）");
            }
            throw BusinessException.badRequest("JavaScript 执行错误: " + e.getMessage());
        } catch (Exception e) {
            throw BusinessException.badRequest("JavaScript 解析失败: " + e.getMessage());
        }
    }

    /**
     * 测试正则解析
     */
    private Object testRegexParse(String ruleConfig, String rawData) {
        try {
            var configNode = objectMapper.readTree(ruleConfig);
            String pattern = configNode.get("pattern").asText();
            var groups = configNode.get("groups");

            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(rawData);

            if (!matcher.find()) {
                throw BusinessException.badRequest("正则表达式未匹配到数据");
            }

            java.util.Map<String, Object> result = new java.util.HashMap<>();

            if (groups != null && groups.isArray()) {
                for (var group : groups) {
                    int index = group.get("index").asInt();
                    String target = group.get("target").asText();
                    String type = group.has("type") ? group.get("type").asText() : "string";

                    if (index <= matcher.groupCount()) {
                        String value = matcher.group(index);
                        if (value != null) {
                            result.put(target, convertType(value, type));
                        }
                    }
                }
            }

            return result;
        } catch (java.util.regex.PatternSyntaxException e) {
            throw BusinessException.badRequest("正则表达式语法错误: " + e.getMessage());
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("REGEX 规则配置格式错误");
        }
    }

    /**
     * 测试二进制解析
     */
    private Object testBinaryParse(String ruleConfig, String rawData) {
        try {
            var configNode = objectMapper.readTree(ruleConfig);
            var fields = configNode.get("fields");

            // 将十六进制字符串转换为字节数组
            byte[] bytes = hexToBytes(rawData);

            java.util.Map<String, Object> result = new java.util.HashMap<>();

            if (fields != null && fields.isArray()) {
                for (var field : fields) {
                    int offset = field.get("offset").asInt();
                    int length = field.get("length").asInt();
                    String target = field.get("target").asText();
                    String type = field.has("type") ? field.get("type").asText() : "string";

                    if (offset + length <= bytes.length) {
                        byte[] fieldBytes = new byte[length];
                        System.arraycopy(bytes, offset, fieldBytes, 0, length);

                        Object value = parseBinaryValue(fieldBytes, type);
                        result.put(target, value);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            throw BusinessException.badRequest("二进制解析失败: " + e.getMessage());
        }
    }

    /**
     * 十六进制字符串转字节数组
     */
    private byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s", ""); // 移除空白字符
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须是偶数");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    /**
     * 解析二进制值
     */
    private Object parseBinaryValue(byte[] bytes, String type) {
        return switch (type.toLowerCase()) {
            case "int8", "byte" -> bytes[0] & 0xFF;
            case "int16", "short" -> java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN).getShort();
            case "int32", "int" -> java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN).getInt();
            case "int64", "long" -> java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN).getLong();
            case "float" -> java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN).getFloat();
            case "double" -> java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN).getDouble();
            default -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8); // string
        };
    }

    /**
     * 检查规则名称是否存在
     */
    private boolean existsRuleName(Long tenantId, Long productId, String ruleName, Long excludeId) {
        return lambdaQuery()
                .eq(ParseRule::getTenantId, tenantId)
                .eq(ParseRule::getProductId, productId)
                .eq(ParseRule::getRuleName, ruleName)
                .ne(excludeId != null, ParseRule::getId, excludeId)
                .exists();
    }

    /**
     * 发布规则更新消息
     */
    private void publishRuleUpdate(ParseRule rule) {
        try {
            String message = String.format("{\"action\":\"update\",\"ruleId\":%d,\"productId\":%d,\"tenantId\":%d}",
                    rule.getId(), rule.getProductId(), rule.getTenantId());
            redisTemplate.convertAndSend(PARSE_RULE_UPDATE_CHANNEL, message);
            log.debug("发布规则更新消息: {}", message);
        } catch (Exception e) {
            log.error("发布规则更新消息失败", e);
        }
    }

    /**
     * 发布规则删除消息
     */
    private void publishRuleDelete(ParseRule rule) {
        try {
            String message = String.format("{\"action\":\"delete\",\"ruleId\":%d,\"productId\":%d,\"tenantId\":%d}",
                    rule.getId(), rule.getProductId(), rule.getTenantId());
            redisTemplate.convertAndSend(PARSE_RULE_UPDATE_CHANNEL, message);
            log.debug("发布规则删除消息: {}", message);
        } catch (Exception e) {
            log.error("发布规则删除消息失败", e);
        }
    }

    /**
     * 获取当前租户ID（平台管理员返回 null）
     */
    private Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            if (TenantContext.isPlatformAdmin()) {
                return null;
            }
            throw BusinessException.unauthorized("未找到租户信息");
        }
        return Long.valueOf(tenantId);
    }

    /**
     * 检查租户访问权限（平台管理员直接放行）
     */
    private void checkTenantAccess(ParseRule rule) {
        if (TenantContext.isPlatformAdmin()) {
            return;
        }
        Long currentTenantId = getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(rule.getTenantId())) {
            log.warn("跨租户访问被拒绝: current={}, target={}",
                    currentTenantId, rule.getTenantId());
            throw BusinessException.forbidden("无权访问该解析规则");
        }
    }
}

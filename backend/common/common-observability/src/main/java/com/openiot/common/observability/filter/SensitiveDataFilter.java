package com.openiot.common.observability.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.openiot.common.observability.config.ObservabilityProperties;
import org.slf4j.Marker;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏过滤器
 *
 * <p>在日志输出前对敏感数据进行脱敏处理：
 * <ul>
 *   <li>密码</li>
 *   <li>Token</li>
 *   <li>密钥</li>
 *   <li>身份证号</li>
 *   <li>手机号</li>
 *   <li>银行卡号</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Component
public class SensitiveDataFilter extends TurboFilter {

    private static final Set<String> DEFAULT_SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd",
            "token", "access_token", "refresh_token", "auth_token",
            "secret", "api_key", "apikey", "secret_key",
            "id_card", "idcard", "id_number",
            "phone", "mobile", "telephone",
            "bank_card", "card_number", "credit_card",
            "email", "mail"
    ));

    // 敏感字段正则匹配模式
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(?i)(\"(password|passwd|pwd|token|secret|api_key|apikey|secret_key|id_card|idcard|bank_card|card_number)\"\\s*:\\s*\")([^\"]+)(\")",
            Pattern.CASE_INSENSITIVE
    );

    // 手机号脱敏模式 (保留前3后4位)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(1[3-9]\\d)\\d{4}(\\d{4})"
    );

    // 身份证号脱敏模式 (保留前4后4位)
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "(\\d{4})\\d{10}(\\d{4})"
    );

    // 邮箱脱敏模式 (保留前3字符和域名)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(\\w{3})[^@]*(@\\w+\\.\\w+)"
    );

    // 银行卡号脱敏模式 (保留前4后4位)
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile(
            "(\\d{4})\\d+(\\d{4})"
    );

    private volatile boolean enabled = true;
    private Set<String> sensitiveFields = DEFAULT_SENSITIVE_FIELDS;

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {

        if (!enabled || format == null) {
            return FilterReply.NEUTRAL;
        }

        // 检查日志消息中是否包含敏感数据
        if (containsSensitiveData(format)) {
            // 如果消息本身包含敏感数据，这里无法修改
            // 建议在日志调用前进行脱敏
            return FilterReply.NEUTRAL;
        }

        // 检查参数中是否包含敏感数据
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String) {
                    params[i] = maskSensitiveData((String) params[i]);
                }
            }
        }

        return FilterReply.NEUTRAL;
    }

    /**
     * 检查字符串是否包含敏感数据
     */
    public boolean containsSensitiveData(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        for (String field : sensitiveFields) {
            if (lowerMessage.contains(field)) {
                return true;
            }
        }

        return PHONE_PATTERN.matcher(message).find() ||
               ID_CARD_PATTERN.matcher(message).find() ||
               EMAIL_PATTERN.matcher(message).find() ||
               BANK_CARD_PATTERN.matcher(message).find();
    }

    /**
     * 对敏感数据进行脱敏
     */
    public String maskSensitiveData(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;

        // 脱敏 JSON 格式的敏感字段
        result = SENSITIVE_FIELD_PATTERN.matcher(result)
                .replaceAll("$1****$4");

        // 脱敏手机号
        result = PHONE_PATTERN.matcher(result)
                .replaceAll("$1****$2");

        // 脱敏身份证号
        result = ID_CARD_PATTERN.matcher(result)
                .replaceAll("$1**********$2");

        // 脱敏邮箱
        result = EMAIL_PATTERN.matcher(result)
                .replaceAll("$1***$2");

        // 脱敏银行卡号
        result = BANK_CARD_PATTERN.matcher(result)
                .replaceAll("$1****$2");

        return result;
    }

    /**
     * 配置敏感字段列表
     */
    public void configure(ObservabilityProperties properties) {
        this.enabled = properties.getLogging().isSensitiveDataMasking();
        if (properties.getLogging().getSensitiveFields() != null &&
            properties.getLogging().getSensitiveFields().length > 0) {
            this.sensitiveFields = new HashSet<>(
                    Arrays.asList(properties.getLogging().getSensitiveFields())
            );
        }
    }

    /**
     * 添加自定义敏感字段
     */
    public void addSensitiveField(String field) {
        sensitiveFields.add(field.toLowerCase());
    }

    /**
     * 移除敏感字段
     */
    public void removeSensitiveField(String field) {
        sensitiveFields.remove(field.toLowerCase());
    }

    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
}

package com.openiot.connect.parser;

/**
 * 解析异常
 * 当解析过程中发生错误时抛出
 *
 * @author open-iot
 */
public class ParseException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private String errorCode;

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ParseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 创建配置错误异常
     */
    public static ParseException configError(String message) {
        return new ParseException("CONFIG_ERROR", message);
    }

    /**
     * 创建数据格式错误异常
     */
    public static ParseException dataFormatError(String message) {
        return new ParseException("DATA_FORMAT_ERROR", message);
    }

    /**
     * 创建执行超时异常
     */
    public static ParseException timeout(String message) {
        return new ParseException("TIMEOUT", message);
    }

    /**
     * 创建执行错误异常
     */
    public static ParseException executionError(String message, Throwable cause) {
        return new ParseException("EXECUTION_ERROR", message, cause);
    }
}

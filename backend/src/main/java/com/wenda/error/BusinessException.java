package com.wenda.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * 业务异常。所有 controller / service 抛出的业务错误必须使用本异常或其子类。
 *
 * <p>错误码必须来自 {@link ErrorCode}，不得在调用处临时拼写。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<Map<String, Object>> details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, List<Map<String, Object>> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public BusinessException(ErrorCode errorCode, List<Map<String, Object>> details) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<Map<String, Object>> details() {
        return details;
    }
}

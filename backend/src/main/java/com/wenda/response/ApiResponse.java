package com.wenda.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wenda.error.ErrorCode;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 统一响应包装（基线：接口文档 v0.2 §2.3）。
 *
 * <pre>
 * {
 *   "success": true,
 *   "code": "OK",
 *   "message": "成功。",
 *   "data": { ... },
 *   "requestId": "req_20260607_xxxxx",
 *   "timestamp": "2026-06-07T10:30:00+08:00"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private List<Map<String, Object>> details;
    private String requestId;
    private String timestamp;

    public ApiResponse() {}

    private ApiResponse(boolean success, String code, String message, T data,
                        List<Map<String, Object>> details, String requestId) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.details = details;
        this.requestId = requestId;
        this.timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .withZone(java.time.ZoneId.systemDefault())
                .format(Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(true, ErrorCode.OK.code(), ErrorCode.OK.defaultMessage(), data, null, requestId);
    }

    public static <T> ApiResponse<T> created(T data, String requestId) {
        return new ApiResponse<>(true, ErrorCode.CREATED.code(), ErrorCode.CREATED.defaultMessage(), data, null, requestId);
    }

    public static <T> ApiResponse<T> accepted(T data, String requestId) {
        return new ApiResponse<>(true, ErrorCode.ACCEPTED.code(), ErrorCode.ACCEPTED.defaultMessage(), data, null, requestId);
    }

    public static <T> ApiResponse<T> error(ErrorCode ec, String requestId) {
        return new ApiResponse<>(false, ec.code(), ec.defaultMessage(), null, null, requestId);
    }

    public static <T> ApiResponse<T> error(ErrorCode ec, String message, String requestId) {
        return new ApiResponse<>(false, ec.code(), message, null, null, requestId);
    }

    public static <T> ApiResponse<T> error(ErrorCode ec, String message,
                                           List<Map<String, Object>> details, String requestId) {
        return new ApiResponse<>(false, ec.code(), message, null, details, requestId);
    }

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public List<Map<String, Object>> getDetails() { return details; }
    public String getRequestId() { return requestId; }
    public String getTimestamp() { return timestamp; }
}

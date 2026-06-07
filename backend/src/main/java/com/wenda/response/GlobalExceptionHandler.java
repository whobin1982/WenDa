package com.wenda.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理。所有 controller 抛出的异常最终都转成 {@link ApiResponse}。
 *
 * <p>错误码必须来自 {@link ErrorCode}，不在此处拼写新错误码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        String rid = RequestContextHolder.requestId();
        log.warn("business exception: code={} message={} requestId={}",
                ex.errorCode().code(), ex.getMessage(), rid);
        ApiResponse<Void> body = ex.details() == null
                ? ApiResponse.error(ex.errorCode(), ex.getMessage(), rid)
                : ApiResponse.error(ex.errorCode(), ex.getMessage(), ex.details(), rid);
        return ResponseEntity.status(ex.errorCode().httpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        String rid = RequestContextHolder.requestId();
        List<Map<String, Object>> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> {
            Map<String, Object> d = new HashMap<>();
            d.put("field", fe.getField());
            d.put("reason", fe.getDefaultMessage());
            d.put("rejectedValue", fe.getRejectedValue());
            details.add(d);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR,
                        ErrorCode.VALIDATION_ERROR.defaultMessage(), details, rid));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        String rid = RequestContextHolder.requestId();
        List<Map<String, Object>> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(v -> {
            Map<String, Object> d = new HashMap<>();
            d.put("field", v.getPropertyPath().toString());
            d.put("reason", v.getMessage());
            d.put("rejectedValue", v.getInvalidValue());
            details.add(d);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR,
                        ErrorCode.VALIDATION_ERROR.defaultMessage(), details, rid));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            JsonProcessingException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        String rid = RequestContextHolder.requestId();
        log.warn("bad request: {} requestId={}", ex.getMessage(), rid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST.defaultMessage(), rid));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        String rid = RequestContextHolder.requestId();
        log.warn("unauthorized: {} requestId={}", ex.getMessage(), rid);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED,
                        ErrorCode.UNAUTHORIZED.defaultMessage(), rid));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        String rid = RequestContextHolder.requestId();
        log.warn("forbidden: {} requestId={}", ex.getMessage(), rid);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN,
                        ErrorCode.FORBIDDEN.defaultMessage(), rid));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
        String rid = RequestContextHolder.requestId();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND,
                        ErrorCode.NOT_FOUND.defaultMessage(), rid));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
        String rid = RequestContextHolder.requestId();
        log.error("unhandled exception requestId={}", rid, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR,
                        ErrorCode.INTERNAL_ERROR.defaultMessage(), rid));
    }
}

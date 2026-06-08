package com.wenda.response;

import com.wenda.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    void okShape() {
        ApiResponse<String> r = ApiResponse.ok("hello", "req_1");
        assertTrue(r.isSuccess());
        assertEquals("OK", r.getCode());
        assertEquals("hello", r.getData());
        assertEquals("req_1", r.getRequestId());
        assertNotNull(r.getTimestamp());
        assertTrue(r.getTimestamp().contains("T"));
    }

    @Test
    void createdShape() {
        ApiResponse<Object> r = ApiResponse.created(Map.of("id", "u1"), "req_2");
        assertTrue(r.isSuccess());
        assertEquals("CREATED", r.getCode());
        assertEquals(201, ErrorCode.CREATED.httpStatus().value());
    }

    @Test
    void acceptedShape() {
        ApiResponse<Object> r = ApiResponse.accepted(Map.of("taskId", "t1"), "req_3");
        assertEquals("ACCEPTED", r.getCode());
    }

    @Test
    void errorShapeWithDetails() {
        var details = List.<Map<String, Object>>of(Map.of("field", "name", "reason", "不能为空"));
        ApiResponse<Void> r = ApiResponse.error(ErrorCode.VALIDATION_ERROR, "参数校验失败。", details, "req_4");
        assertFalse(r.isSuccess());
        assertEquals("VALIDATION_ERROR", r.getCode());
        assertEquals("参数校验失败。", r.getMessage());
        assertEquals(1, r.getDetails().size());
        assertEquals("name", r.getDetails().get(0).get("field"));
    }
}

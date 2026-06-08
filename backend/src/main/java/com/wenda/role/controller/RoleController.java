package com.wenda.role.controller;

import com.wenda.context.RequestContextHolder;
import com.wenda.error.BusinessException;
import com.wenda.error.ErrorCode;
import com.wenda.response.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 角色元数据 Controller（API-ROLE-001）。
 *
 * <p>返回当前实现所支持的全部角色代码、名称、描述、是否系统内置。
 * 基线：接口文档 v0.2 / 权限判定矩阵 v1.0 §2。
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final JdbcTemplate jdbc;

    public RoleController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT code, name, description, is_system FROM roles ORDER BY code");
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色元数据未初始化。");
        }
        return ApiResponse.ok(rows, RequestContextHolder.requestId());
    }
}

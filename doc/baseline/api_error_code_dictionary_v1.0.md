# 全量错误码字典 v1.0

**用途：接口实现、前端提示、接口自动化测试和验收断言的统一错误码来源。**

| 错误码 | HTTP | 适用模块 | 标准 message | details 结构 | 是否进入审计 | 关联接口说明 |
|---|---:|---|---|---|---:|---|
| OK | 200 | 通用 | 成功。 | 无 | 否 | 全部接口成功响应 |
| CREATED | 201 | 通用 | 创建成功。 | 无 | 否 | 创建类接口 |
| ACCEPTED | 202 | 通用 | 异步任务已创建。 | asyncTaskId、taskStatus | 是 | 异步任务类接口 |
| BAD_REQUEST | 400 | 通用 | 请求格式错误。 | requestBody、parseError | 否 | 全部接口 |
| VALIDATION_ERROR | 400 | 通用 | 参数校验失败。 | field、reason、rejectedValue | 否 | 全部接口 |
| UNAUTHORIZED | 401 | 认证与权限 | 未登录或 Token 无效。 | authReason | 是 | 需登录接口 |
| TOKEN_EXPIRED | 401 | 认证与权限 | 登录已过期，请重新登录。 | expiredAt | 是 | 需登录接口 |
| FORBIDDEN | 403 | 认证与权限 | 无权限访问资源。 | resourceType、action、scope | 是 | 需鉴权接口 |
| ACCESS_DENIED | 403 | 认证与权限 | 访问被拒绝。 | resourceType、action、denyReason | 是 | 需鉴权接口 |
| SCOPE_FORBIDDEN | 403 | 认证与权限 | 无权访问该组织或资源范围。 | schoolId、collegeId、majorId、resourceId | 是 | 范围受限接口 |
| NOT_FOUND | 404 | 通用 | 资源不存在或不可见。 | resourceType、resourceId | 否 | 全部资源查询接口 |
| CONFLICT | 409 | 通用 | 唯一性冲突或业务冲突。 | conflictField、existingResourceId | 否 | 创建 / 更新接口 |
| VERSION_CONFLICT | 409 | 并发控制 | 资源版本不一致。 | expectedVersion、actualVersion | 否 | PATCH / PUT 接口 |
| IDEMPOTENCY_CONFLICT | 409 | 幂等 | 幂等键已用于不同请求体。 | idempotencyKey、originalRequestHash | 是 | 创建 / 任务类接口 |
| BUSINESS_STATE_INVALID | 422 | 业务状态 | 当前状态不允许执行该操作。 | currentStatus、allowedStatuses | 是 | 状态流接口 |
| SCHOOL_SCOPE_REQUIRED | 422 | 租户隔离 | 缺少学校空间上下文。 | requestContext | 是 | 业务接口 |
| FILE_TOO_LARGE | 413 | 文件 | 文件超过大小限制。 | maxSizeBytes、actualSizeBytes | 是 | 文件上传接口 |
| UNSUPPORTED_FILE_TYPE | 415 | 文件 | 文件类型不支持。 | allowedTypes、actualType | 是 | 文件上传 / 导入接口 |
| FILE_TYPE_NOT_ALLOWED | 415 | 文件 | 该业务场景不允许上传此文件类型。 | businessType、allowedTypes、actualType | 是 | 文件上传 / 企业材料 / 报告相关接口 |
| FILE_SECURITY_SCAN_PENDING | 423 | 文件安全 | 文件尚未通过安全扫描。 | fileId、scanStatus | 是 | 文件预览 / 下载 / 解析接口 |
| FILE_SECURITY_SCAN_FAILED | 422 | 文件安全 | 文件安全扫描未通过。 | fileId、scanResult | 是 | 文件确认 / 下载 / 解析接口 |
| FILE_ACCESS_DENIED | 403 | 文件 | 无权访问该文件。 | fileId、resourceType、resourceId | 是 | 文件下载 / 预览接口 |
| AI_PROVIDER_DISABLED | 422 | AI | 学校未启用对应 AI Provider。 | providerCode、schoolId | 是 | AI 任务接口 |
| AI_OUTPUT_SCHEMA_INVALID | 422 | AI | AI 输出不符合结构化 Schema。 | schemaName、validationErrors | 是 | AI 任务结果处理接口 |
| AI_TASK_FAILED | 422 | AI | AI 任务执行失败。 | taskId、providerCode、failureReason | 是 | AI 任务接口 |
| TASK_NOT_FOUND | 404 | 异步任务 | 异步任务不存在或不可见。 | taskId | 否 | 任务查询接口 |
| TASK_ALREADY_RUNNING | 409 | 异步任务 | 任务已在执行中。 | taskId、businessKey | 否 | 任务创建接口 |
| TASK_TIMEOUT | 422 | 异步任务 | 任务执行超时。 | taskId、timeoutSeconds | 是 | 任务查询 / Worker 执行 |
| REPORT_TEMPLATE_NOT_FOUND | 404 | 报告 | 报告模板不存在或不可用。 | reportType、templateVersion | 是 | 报告生成接口 |
| REPORT_FILE_NOT_READY | 423 | 报告 | 报告文件尚未生成完成。 | reportId、taskStatus | 否 | 报告下载接口 |
| KNOWLEDGE_SOURCE_DISABLED | 422 | 知识库 | 知识库资料已禁用或不可正式引用。 | documentId、reviewStatus、licenseStatus | 是 | 知识库检索 / 引用 / 课程内容发布接口 |
| KNOWLEDGE_SEARCH_UNSUPPORTED_FILTER | 400 | 知识库 | 知识库检索筛选条件不支持。 | filterName、allowedFilters | 否 | 知识库检索接口 |
| AUTHORIZATION_EXPIRED | 403 | 授权与能力地图 | 学生授权已过期。 | authorizationId、expiredAt | 是 | 能力地图公开访问 / PDF 下载接口 |
| AUTHORIZATION_REVOKED | 403 | 授权与能力地图 | 学生授权已撤销。 | authorizationId、revokedAt | 是 | 能力地图公开访问 / PDF 下载接口 |
| DISPLAY_SCOPE_FORBIDDEN | 403 | 授权与能力地图 | 请求内容超出学生授权展示范围。 | requestedScope、allowedScopes | 是 | 能力地图公开访问 / PDF 下载接口 |
| IMPORT_TEMPLATE_INVALID | 400 | 导入 | 导入模板版本或结构无效。 | templateVersion、expectedColumns、actualColumns | 是 | 导入预校验接口 |
| IMPORT_CONFLICT_REQUIRES_DECISION | 409 | 导入 | 导入存在冲突，需要用户选择处理策略。 | conflictRows、availableStrategies | 是 | 导入预校验 / 提交接口 |
| RATE_LIMITED | 429 | 限流 | 请求过于频繁。 | retryAfterSeconds | 是 | 全部接口 |
| INTERNAL_ERROR | 500 | 系统 | 服务端异常。 | errorId | 是 | 全部接口 |
| DEPENDENCY_UNAVAILABLE | 503 | 外部依赖 | 外部依赖不可用。 | dependencyName、operation | 是 | AI、对象存储、文件扫描、报告、邮件等依赖接口 |

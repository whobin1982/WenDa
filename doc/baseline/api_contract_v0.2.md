# 接口文档 v0.2

**产品名称：AI 原生本科专业建设与学生能力成长平台**  
**文档版本：v0.2**  
**生成依据：《产品需求说明书 v0.3》《系统架构设计 v0.3》《验收清单终版 v1.2》《权限判定矩阵 v1.0》《任务-接口精确映射表 v1.0》《开发输入基线索引 v1.0》及 API-DEC-001～API-DEC-012**  
**文档视角：API 设计师 + 后端架构师**  
**技术基线：Vue 3 + TypeScript；Java 21 + Spring Boot 3.x；PostgreSQL；S3 兼容对象存储；PostgreSQL 任务表 + Java Worker**

**权威输入说明：本文档与其他输入文件存在冲突时，版本、冲突优先级和自动开发读取规则以《开发输入基线索引 v1.0》为准。**

---

## 1. 输入充分性检查

### 1.1 检查结论

**结论：输入足够生成接口文档 v0.2。**

当前接口设计基于以下已确认基线：

| 输入项 | 状态 | 说明 |
|---|---:|---|
| 产品需求说明书 | 已确认 | 覆盖 FR-001～FR-032、NFR-001～NFR-015。 |
| 验收清单初版 | 已确认 | 已形成 AC-DEC-001～AC-DEC-014，待确认验收项已清零。 |
| 系统架构设计 | 已确认 | 已形成 ARCH-DEC-001～ARCH-DEC-015。 |
| 接口待确认项 | 已确认 | 已形成 API-DEC-001～API-DEC-012；本版无阻塞待确认项。 |
| MVP 部署边界 | 已确认 | 平台托管单学校独立租户，所有接口默认在当前 school_id / tenant_id 范围内执行。 |
| 账号认证 | 已确认 | MVP 默认内置账号，预留 SSO，企业账号邀请制。 |
| 文件与异步 | 已确认 | 文件默认私有、后端鉴权；AI、解析、导出等长任务异步执行。 |

### 1.2 设计边界

本接口文档只定义当前需求范围内的 API，不包含以下接口：

```text
1. 完整教务系统接口；
2. 完整成绩系统接口；
3. 完整排课接口；
4. LMS 在线教学接口；
5. 多学校 SaaS 运营后台接口；
6. 学校 SSO 实际协议回调接口；
7. 原生 App / 小程序专属接口；
8. 开放互联网搜索采集接口；
9. 向量数据库管理接口；
10. 复杂审批流模板配置接口；
11. 完整招聘流程接口。
```

---

## 2. 通用接口规范

### 2.1 基础路径与版本

```text
API Base Path: /api/v1
Content-Type: application/json; charset=utf-8
File Upload: multipart/form-data
Authentication: Bearer Token
```

说明：

```text
1. MVP 阶段所有业务接口默认运行在当前用户所属 school_id / tenant_id 范围内。
2. 客户端不得通过请求体伪造 school_id / tenant_id。
3. 如请求中出现 schoolId / tenantId，仅用于系统管理员创建学校空间或后续多学校扩展；普通业务接口以后端鉴权上下文为准。
4. 所有写操作必须记录审计日志，除非明确标注为非关键操作。
```

### 2.2 通用请求头

| Header | 必填 | 说明 |
|---|---:|---|
| Authorization | 是 | `Bearer <accessToken>`；登录、公开邀请页、公开能力地图页除外。 |
| X-Request-Id | 否 | 客户端请求追踪 ID；未传时后端生成。 |
| Idempotency-Key | 条件必填 | 对创建类、任务类、导入提交、报告导出、授权创建、企业邀请等接口必填。 |
| If-Match | 条件必填 | 更新已有资源时建议传资源 version，用于防止并发覆盖。 |
| Accept-Language | 否 | 默认 `zh-CN`。 |

### 2.3 通用响应格式

成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "OK",
  "data": {},
  "requestId": "req_20260606_xxxxx",
  "timestamp": "2026-06-06T10:30:00+08:00"
}
```

失败响应：

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "参数校验失败",
  "details": [
    {
      "field": "majorName",
      "reason": "不能为空"
    }
  ],
  "requestId": "req_20260606_xxxxx",
  "timestamp": "2026-06-06T10:30:00+08:00"
}
```

分页响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "OK",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 128,
    "totalPages": 7,
    "sort": "createdAt",
    "order": "desc"
  },
  "requestId": "req_20260606_xxxxx",
  "timestamp": "2026-06-06T10:30:00+08:00"
}
```

### 2.4 通用分页、排序、过滤规则

| 参数 | 类型 | 默认 | 说明 |
|---|---|---:|---|
| page | integer | 1 | 从 1 开始。 |
| pageSize | integer | 20 | 默认 20，最大 100。大表如支撑矩阵可使用专用接口分块返回。 |
| sort | string | createdAt | 排序字段，必须在接口允许列表内。 |
| order | string | desc | 可选：asc / desc。 |
| keyword | string | 无 | 模糊搜索关键字；具体搜索字段由接口定义。 |

通用过滤规则：

```text
1. 列表接口应显式声明可过滤字段，不支持任意字段过滤。
2. 时间范围统一使用 startAt / endAt，ISO 8601 格式。
3. 状态过滤统一使用 status。
4. 组织范围过滤使用 collegeId、majorId、courseId、studentId 等显式字段。
5. 普通用户传入越权范围时返回 FORBIDDEN，不返回空数据掩盖越权。
```

### 2.5 通用幂等规则

| 场景 | 规则 |
|---|---|
| 创建学校、学院、专业、课程、学生证据、企业邀请、能力地图授权 | 必须传 `Idempotency-Key`。重复提交相同 Key 返回首次结果。 |
| AI 任务、文件解析任务、报告生成任务、导入提交任务 | 必须传 `Idempotency-Key`。重复提交不得创建重复任务。 |
| PATCH 更新 | 使用 `If-Match: <version>`；版本不一致返回 `VERSION_CONFLICT`。 |
| 审核、发布、归档、恢复 | 同一资源同一状态重复提交应返回当前状态或 `BUSINESS_STATE_INVALID`，不得重复产生正式版本。 |
| GET 查询 | 天然幂等。 |
| DELETE | MVP 不使用物理删除；删除类操作统一建模为归档、撤回、停用或作废。 |

### 2.6 通用错误码

| 错误码 | HTTP | 说明 |
|---|---:|---|
| OK | 200 | 成功。 |
| CREATED | 201 | 创建成功。 |
| ACCEPTED | 202 | 异步任务已创建。 |
| BAD_REQUEST | 400 | 请求格式错误。 |
| VALIDATION_ERROR | 400 | 参数校验失败。 |
| UNAUTHORIZED | 401 | 未登录或 Token 无效。 |
| FORBIDDEN | 403 | 无权限访问资源。 |
| NOT_FOUND | 404 | 资源不存在或不可见。 |
| CONFLICT | 409 | 唯一性冲突或业务冲突。 |
| VERSION_CONFLICT | 409 | 资源版本不一致。 |
| IDEMPOTENCY_CONFLICT | 409 | 幂等键已用于不同请求体。 |
| BUSINESS_STATE_INVALID | 422 | 当前状态不允许执行该操作。 |
| SCHOOL_SCOPE_REQUIRED | 422 | 缺少学校空间上下文。 |
| FILE_TOO_LARGE | 413 | 文件超过限制。 |
| UNSUPPORTED_FILE_TYPE | 415 | 文件类型不支持。 |
| FILE_SECURITY_SCAN_PENDING | 423 | 文件尚未通过安全扫描。 |
| FILE_SECURITY_SCAN_FAILED | 422 | 文件安全扫描未通过。 |
| AI_PROVIDER_DISABLED | 422 | 学校未启用对应 AI Provider。 |
| AI_OUTPUT_SCHEMA_INVALID | 422 | AI 输出不符合结构化 Schema。 |
| TASK_NOT_FOUND | 404 | 异步任务不存在或不可见。 |
| TASK_ALREADY_RUNNING | 409 | 任务已在执行中。 |
| AUTHORIZATION_EXPIRED | 403 | 学生授权已过期。 |
| AUTHORIZATION_REVOKED | 403 | 学生授权已撤销。 |
| RATE_LIMITED | 429 | 请求过于频繁。MVP 可先不启用复杂限流。 |
| INTERNAL_ERROR | 500 | 服务端异常。 |
| DEPENDENCY_UNAVAILABLE | 503 | 外部依赖不可用，例如 AI Provider、对象存储、扫描服务。 |

---

### 2.7 全量错误码字典

本节为接口实现、前端提示、接口自动化测试和验收断言的统一错误码来源。所有模块不得自行新增未登记错误码；如需新增，必须同步更新本节和 `api_error_code_matrix_exact_v1.0.md`。

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

> 关联接口的机器可读精确列表见 `api_error_code_matrix_exact_v1.0.md`。

---

## 3. 权限模型摘要

### 3.1 角色代码

| 角色代码 | 中文名称 | 说明 |
|---|---|---|
| SYSTEM_ADMIN | 系统管理员 | 管理学校空间、系统配置、审计。 |
| SCHOOL_ADMIN | 学校管理者 | 管理本学校下学院、专业、配置和看板。 |
| COLLEGE_MANAGER | 学院管理者 | 管理授权学院下专业与汇总数据。 |
| MAJOR_OWNER | 专业负责人 | 管理授权专业的专业建设、OBE、课程体系、发布。 |
| ACADEMIC_ADMIN | 教务管理人员 | 维护培养方案、课程计划、导出材料。 |
| TEACHER | 任课教师 | 管理所授课程、课程内容、学生证据审核。 |
| MENTOR | 导师 / 班主任 | 查看所指导学生画像、处理申诉、审核能力地图。 |
| STUDENT | 学生 | 查看本人画像、证据、任务、能力地图与授权。 |
| EMPLOYER_MENTOR | 企业导师 | 在授权范围内维护岗位、评价、查看授权能力地图。 |
| KNOWLEDGE_ADMIN | 知识库管理员 | 管理课程资料、可信度、版权许可、审核。 |

### 3.2 权限判定原则

```text
接口权限 = RBAC 角色权限 + Scope 组织范围 + ABAC 资源属性条件
```

典型规则：

```text
1. 学校管理员只能访问当前学校空间数据。
2. 学院管理者只能访问授权学院范围数据。
3. 专业负责人只能访问授权专业范围数据。
4. 教师只能访问所授课程及与其课程相关学生证据。
5. 导师只能访问所指导学生画像、证据、预警和能力地图。
6. 学生只能访问本人数据，且只能授权本人能力地图。
7. 企业导师只能访问被邀请、审核且学生授权范围内的数据。
8. AI 任务必须继承发起人的数据权限，不得访问无权限数据。
9. 报告导出必须校验数据版本、角色权限和学生授权范围。
```

---

## 4. 核心对象与字段契约

> 字段命名采用 camelCase。数据库字段可采用 snake_case，但接口层统一 camelCase。

### 4.1 基础对象

#### School

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| schoolId | string | 是 | 学校 ID。 |
| schoolName | string | 是 | 学校名称。 |
| schoolType | string | 否 | 学校类型，枚举：application_undergraduate / research_university / vocational_undergraduate / higher_vocational / independent_college / other。 |
| region | string | 否 | 所在地区。 |
| status | string | 是 | active / disabled / archived。 |
| createdAt | datetime | 是 | 创建时间。 |
| updatedAt | datetime | 是 | 更新时间。 |
| version | integer | 是 | 乐观锁版本。 |

#### College

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| collegeId | string | 是 | 学院 ID。 |
| schoolId | string | 是 | 归属学校。 |
| collegeName | string | 是 | 学院名称。 |
| status | string | 是 | active / disabled / archived。 |
| createdAt | datetime | 是 | 创建时间。 |
| updatedAt | datetime | 是 | 更新时间。 |
| version | integer | 是 | 乐观锁版本。 |

#### User

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| userId | string | 是 | 用户 ID。 |
| schoolId | string | 是 | 学校空间。 |
| username | string | 是 | 登录名。 |
| displayName | string | 是 | 显示名称。 |
| email | string | 否 | 邮箱。 |
| phone | string | 否 | 手机号。 |
| authSource | string | 是 | local / cas / oidc / saml / ldap；MVP 默认 local。 |
| externalIdentityId | string | 否 | 外部身份 ID，SSO 预留。 |
| roles | array | 是 | 角色列表。 |
| scopes | array | 是 | 组织范围。 |
| status | string | 是 | active / disabled / invited / pending_review。 |
| version | integer | 是 | 乐观锁版本。 |

#### Scope

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| scopeType | string | 是 | school / college / major / course / student。 |
| scopeId | string | 是 | 范围对象 ID。 |

### 4.2 专业、OBE、课程体系对象

#### Major

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| majorId | string | 是 | 专业 ID。 |
| schoolId | string | 是 | 学校 ID。 |
| collegeId | string | 是 | 学院 ID。 |
| majorName | string | 是 | 专业名称。 |
| majorCode | string | 否 | 专业代码。MVP 中可选；如填写，应在当前学校范围内唯一；未填写不阻塞专业草案创建。 |
| educationYears | integer | 否 | 学制。 |
| degreeType | string | 否 | 学位类型，使用平台默认学位枚举，详见 6.2。 |
| ownerUserId | string | 是 | 专业负责人。 |
| targetIndustry | string | 否 | 目标产业自由文本，例如“低空经济与智能制造”。 |
| industryTags | array | 否 | 可选行业标签，例如 low_altitude_economy / intelligent_manufacturing。 |
| targetJobRoles | array | 否 | 目标岗位。 |
| constructionStage | string | 是 | not_started / drafting / reviewing / published / archived。 |
| maturityScore | number | 否 | 成熟度百分比。 |
| maturityLevel | string | 否 | low / medium / high，展示文案前端本地化。 |
| currentMajorVersionId | string | 否 | 当前培养方案版本。 |
| version | integer | 是 | 乐观锁版本。 |

#### ProgramObjective

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| objectiveId | string | 是 | 培养目标 ID。 |
| majorVersionId | string | 是 | 培养方案版本 ID。 |
| code | string | 是 | 编号，如 PO-1。 |
| title | string | 是 | 目标名称。 |
| description | string | 是 | 描述。 |
| relatedRequirementIds | array | 否 | 关联毕业要求。 |
| status | string | 是 | draft / pending_review / published / archived。 |
| version | integer | 是 | 乐观锁版本。 |

#### GraduationRequirement

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| requirementId | string | 是 | 毕业要求 ID。 |
| majorVersionId | string | 是 | 培养方案版本 ID。 |
| code | string | 是 | 编号，如 GR-1。 |
| title | string | 是 | 要求名称。 |
| description | string | 是 | 描述。 |
| relatedObjectiveIds | array | 否 | 关联培养目标。 |
| status | string | 是 | draft / pending_review / published / archived。 |
| version | integer | 是 | 乐观锁版本。 |

#### AbilityIndicator

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| indicatorId | string | 是 | 能力指标点 ID。 |
| requirementId | string | 是 | 所属毕业要求。 |
| code | string | 是 | 编号，如 GR-1.1。 |
| title | string | 是 | 指标点名称。 |
| description | string | 是 | 描述。 |
| evidenceExamples | array | 否 | 专业补充证据示例。 |
| status | string | 是 | draft / pending_review / published / archived。 |
| version | integer | 是 | 乐观锁版本。 |

#### CurriculumVersion

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| curriculumVersionId | string | 是 | 课程体系版本 ID。 |
| majorId | string | 是 | 专业 ID。 |
| majorVersionId | string | 是 | 培养方案版本 ID。 |
| versionName | string | 是 | 版本名称。 |
| status | string | 是 | draft / review_pending / approved / published / superseded / archived。 |
| effectiveGrade | string | 否 | 适用年级。 |
| totalCredits | number | 否 | 总学分。 |
| totalHours | number | 否 | 总学时。 |
| theoryHours | number | 否 | 理论学时。 |
| practiceHours | number | 否 | 实践学时。 |
| version | integer | 是 | 乐观锁版本。 |

#### CurriculumCourse

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| curriculumCourseId | string | 是 | 课程体系内课程实例 ID。 |
| curriculumVersionId | string | 是 | 课程体系版本。 |
| courseId | string | 是 | 平台课程 ID。 |
| courseVersionId | string | 否 | 专业课程版本 ID。 |
| courseModule | string | 是 | 课程模块枚举，详见 6.2。 |
| suggestedSemester | integer | 是 | 1～8。 |
| credits | number | 是 | 学分。 |
| totalHours | number | 是 | 学时。 |
| theoryHours | number | 否 | 理论学时。 |
| practiceHours | number | 否 | 实践学时。 |
| courseNature | string | 否 | 课程性质枚举，详见 6.2。 |

#### SupportMatrixItem

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| supportId | string | 是 | 支撑关系 ID。 |
| curriculumVersionId | string | 是 | 课程体系版本。 |
| courseId | string | 是 | 课程 ID。 |
| indicatorId | string | 是 | 能力指标点 ID。 |
| supportLevel | string | 是 | high / medium / low / none。 |
| supportWeight | number | 是 | high=1.0, medium=0.6, low=0.3, none=0。 |
| supportDescription | string | 否 | 支撑说明。 |
| status | string | 是 | draft / confirmed / published。 |

### 4.3 知识库与课程内容对象

#### Course

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| courseId | string | 是 | 平台课程 ID。 |
| schoolId | string | 是 | 学校 ID。 |
| courseCode | string | 是 | 学校范围唯一。可为系统生成临时代码。 |
| isTemporaryCode | boolean | 是 | 是否临时代码。 |
| courseName | string | 是 | 课程名称。 |
| courseType | string | 否 | 课程类型枚举，详见 6.2。 |
| credits | number | 否 | 学分。 |
| totalHours | number | 否 | 学时。 |
| theoryHours | number | 否 | 理论学时。 |
| practiceHours | number | 否 | 实践学时。 |
| status | string | 是 | active / disabled / archived。 |
| version | integer | 是 | 乐观锁版本。 |

#### KnowledgeDocument

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| documentId | string | 是 | 知识文档 ID。 |
| schoolId | string | 是 | 学校 ID。 |
| courseId | string | 否 | 关联课程。 |
| sourceType | string | 是 | file / url。 |
| sourceName | string | 是 | 来源名称。 |
| sourceUrl | string | 条件必填 | URL 资料必填。 |
| fileId | string | 条件必填 | 文件资料必填。 |
| author | string | 否 | 作者。 |
| organization | string | 否 | 机构。 |
| publishDate | date | 否 | 发布时间。 |
| credibilityLevel | string | 是 | high / medium / low。 |
| copyrightStatus | string | 是 | unknown / authorized / public_license / internal_reference / forbidden。 |
| licenseType | string | 否 | CC / open_source / commercial / school_internal / other / unknown。 |
| usageScope | string | 是 | formal_reference / draft_only / generation_forbidden / forbidden。 |
| reviewStatus | string | 是 | pending_review / trusted / limited_use / rejected / disabled / archived。 |
| parseStatus | string | 是 | pending / parsed / failed。 |
| version | integer | 是 | 乐观锁版本。 |

#### KnowledgeChunk

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| chunkId | string | 是 | 知识片段 ID。 |
| documentId | string | 是 | 所属文档。 |
| chunkText | string | 是 | 片段文本。 |
| summary | string | 否 | 摘要。 |
| tags | array | 否 | 标签。 |
| knowledgePoints | array | 否 | 知识点。 |
| inheritedUsageScope | string | 是 | 继承文档使用范围。 |
| chunkOrder | integer | 是 | 片段顺序。 |

#### CitationRecord

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| citationId | string | 是 | 引用记录 ID。 |
| generatedContentId | string | 是 | 被生成内容 ID。 |
| documentId | string | 是 | 文档 ID。 |
| chunkId | string | 否 | 片段 ID。 |
| citedAt | datetime | 是 | 引用时间。 |
| citationPurpose | string | 是 | course_outline / assignment / experiment / case / report / other。 |
| reviewStatusSnapshot | string | 是 | 当时审核状态快照。 |
| copyrightStatusSnapshot | string | 是 | 当时版权状态快照。 |
| licenseTypeSnapshot | string | 否 | 当时许可快照。 |
| usageScopeSnapshot | string | 是 | 当时使用范围快照。 |

### 4.4 学生、证据、能力对象

#### Student

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| studentId | string | 是 | 学生 ID。 |
| schoolId | string | 是 | 学校 ID。 |
| studentNo | string | 是 | 学号，学校范围唯一。 |
| studentName | string | 是 | 姓名。 |
| collegeId | string | 是 | 学院。 |
| majorId | string | 是 | 专业。 |
| grade | string | 是 | 年级。 |
| className | string | 是 | 班级。 |
| majorVersionId | string | 是 | 培养方案版本。 |
| studentStatus | string | 是 | studying / suspended / withdrawn / graduated / other。 |
| mentorUserId | string | 否 | 导师 / 班主任。 |
| interestDirection | string | 否 | 兴趣方向。 |
| developmentGoal | string | 否 | 个人发展目标。 |
| status | string | 是 | active / archived。 |
| version | integer | 是 | 乐观锁版本。 |

#### StudentCourseRecord

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| recordId | string | 是 | 课程修读记录 ID。 |
| studentId | string | 是 | 学生 ID。 |
| courseId | string | 是 | 课程 ID。 |
| courseCode | string | 是 | 课程代码。 |
| courseName | string | 是 | 课程名称。 |
| studySemester | string | 是 | 修读学期。 |
| studyStatus | string | 是 | completed / in_progress / retake / exempted / failed / other。 |
| score | string | 否 | 成绩，仅作为证据来源之一。 |
| courseAchievement | string | 否 | 课程达成结果。 |
| majorVersionId | string | 是 | 培养方案版本。 |
| curriculumVersionId | string | 是 | 课程体系版本。 |
| dataSource | string | 否 | academic_export / teacher_import / manual / other。 |

#### StudentEvidence

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| evidenceId | string | 是 | 学习证据 ID。 |
| studentId | string | 是 | 学生 ID。 |
| evidenceType | string | 是 | course_score / assignment / experiment_report / project_code / project_report / competition_certificate / internship_review / portfolio / teacher_review / mentor_review / employer_review / reflection / other。 |
| title | string | 是 | 证据标题。 |
| description | string | 否 | 说明。 |
| relatedCourseId | string | 否 | 关联课程。 |
| relatedProjectName | string | 否 | 关联项目 / 竞赛 / 实习等。 |
| fileIds | array | 否 | 附件文件。 |
| mappedIndicatorIds | array | 否 | 关联能力指标点。 |
| status | string | 是 | submitted / parsed / pending_review / verified / counted / rejected / need_more_evidence。 |
| submittedBy | string | 是 | 提交人。 |
| reviewedBy | string | 否 | 审核人。 |
| reviewComment | string | 否 | 审核意见。 |
| version | integer | 是 | 乐观锁版本。 |

#### AbilityLevelRecord

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| abilityRecordId | string | 是 | 能力记录 ID。 |
| studentId | string | 是 | 学生 ID。 |
| indicatorId | string | 是 | 能力指标点。 |
| levelCode | string | 是 | L1～L5，学校可配置名称和说明。 |
| evidenceIds | array | 是 | 已审核证据 ID。 |
| status | string | 是 | suggested / confirmed / archived。 |
| confirmedBy | string | 否 | 确认人。 |
| confirmedAt | datetime | 否 | 确认时间。 |

### 4.5 能力地图、企业与反馈对象

#### GraduateAbilityMap

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| abilityMapId | string | 是 | 能力地图 ID。 |
| studentId | string | 是 | 学生 ID。 |
| majorId | string | 是 | 专业 ID。 |
| majorVersionId | string | 是 | 培养方案版本。 |
| status | string | 是 | generating / draft / mentor_review / student_confirmed / authorized_export / archived。 |
| abilitySummary | object | 否 | 能力总览。 |
| evidenceSummary | object | 否 | 证据摘要。 |
| mentorReviewComment | string | 否 | 导师审核意见。 |
| aiSummary | string | 否 | AI 总结，仅内部展示，导出需按授权规则过滤。 |
| version | integer | 是 | 乐观锁版本。 |

#### AbilityMapAuthorization

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| authorizationId | string | 是 | 授权 ID。 |
| abilityMapId | string | 是 | 能力地图 ID。 |
| studentId | string | 是 | 学生 ID。 |
| granteeType | string | 是 | employer / public_link。 |
| granteeId | string | 否 | 用人单位或企业导师 ID。 |
| displayScopes | array | 是 | 章节级展示范围枚举，详见 6.2 AbilityMapDisplayScope。 |
| validFrom | datetime | 是 | 生效时间。 |
| validUntil | datetime | 是 | 截止时间。 |
| status | string | 是 | active / expired / revoked。 |
| accessToken | string | 是 | 对外访问令牌，不应明文返回多次；创建时一次性返回。 |

#### Employer

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| employerId | string | 是 | 用人单位 ID。 |
| employerName | string | 是 | 用人单位名称。 |
| unifiedSocialCreditCode | string | 是 | 统一社会信用代码 / 组织机构代码。 |
| organizationType | string | 是 | enterprise / public_institution / research_institute / government / other。 |
| industry | string | 是 | 所属行业自由文本。 |
| industryTags | array | 否 | 可选行业标签，后续可映射标准行业字典。 |
| region | string | 是 | 所在地区。 |
| officialWebsiteOrContact | string | 否 | 官网或公开联系方式。 |
| reviewStatus | string | 是 | invited / pending_info / pending_review / approved / rejected / need_more_material / disabled。 |
| cooperationInfo | object | 是 | 合作关系说明。 |
| version | integer | 是 | 乐观锁版本。 |

---

## 5. 接口目录与契约

### 5.1 认证、用户与组织

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-AUTH-001 | 本地账号登录 | FR-001, FR-002 / 用户登录 | POST `/auth/login` | 匿名 | body: username, password | accessToken, refreshToken, expiresIn, user: UserSummary | 无 | 否 | UNAUTHORIZED, VALIDATION_ERROR |
| API-AUTH-002 | 退出登录 | FR-001, FR-002 / 用户退出 | POST `/auth/logout` | 已登录 | body: refreshToken 可选 | loggedOut | 无 | 是 | UNAUTHORIZED |
| API-AUTH-003 | 刷新 Token | FR-001 / 认证续期 | POST `/auth/refresh` | 匿名，需 refreshToken | body: refreshToken | accessToken, expiresIn | 无 | 否 | UNAUTHORIZED |
| API-AUTH-004 | 当前用户信息 | FR-001, FR-002 / 权限菜单初始化 | GET `/auth/me` | 已登录 | 无 | userId, displayName, roles, scopes, permissions, schoolId | 无 | GET 幂等 | UNAUTHORIZED |
| API-ORG-001 | 创建学校空间 | FR-001 / 单学校独立租户初始化 | POST `/schools` | SYSTEM_ADMIN | body: schoolName, schoolType?, region? | School | 无 | 必须 Idempotency-Key | CONFLICT, VALIDATION_ERROR |
| API-ORG-002 | 获取当前学校信息 | FR-001, NFR-014 / 学校空间信息 | GET `/schools/current` | 已登录 | 无 | School | 无 | GET 幂等 | UNAUTHORIZED, FORBIDDEN |
| API-ORG-003 | 更新学校信息 | FR-001 / 学校基础信息维护 | PATCH `/schools/{schoolId}` | SYSTEM_ADMIN 或 SCHOOL_ADMIN | path: schoolId; body: schoolName?, schoolType?, region?, status?; header: If-Match | School | 无 | If-Match | VERSION_CONFLICT, FORBIDDEN |
| API-ORG-004 | 创建学院 | FR-001, FR-003 / 多学院管理 | POST `/colleges` | SCHOOL_ADMIN | body: collegeName, status? | College | 无 | 必须 Idempotency-Key | CONFLICT, VALIDATION_ERROR |
| API-ORG-005 | 学院列表 | FR-003, FR-004 / 学校看板和学院管理 | GET `/colleges` | SCHOOL_ADMIN, COLLEGE_MANAGER | query: page, pageSize, keyword, status | items: College[] | sort: collegeName, createdAt; filter: status, keyword | GET 幂等 | FORBIDDEN |
| API-ORG-006 | 更新学院 | FR-001, FR-003 / 学院维护 | PATCH `/colleges/{collegeId}` | SCHOOL_ADMIN | path: collegeId; body: collegeName?, status?; header: If-Match | College | 无 | If-Match | VERSION_CONFLICT, NOT_FOUND |
| API-USER-001 | 创建用户 | FR-001 / 内置账号管理 | POST `/users` | SYSTEM_ADMIN, SCHOOL_ADMIN | body: username, displayName, email?, phone?, roles, scopes, initialPasswordMode | User | 无 | 必须 Idempotency-Key | CONFLICT, VALIDATION_ERROR |
| API-USER-002 | 用户列表 | FR-001, FR-002 / 用户与角色管理 | GET `/users` | SYSTEM_ADMIN, SCHOOL_ADMIN, COLLEGE_MANAGER | query: page, pageSize, role?, collegeId?, majorId?, status?, keyword | items: User[] | sort: createdAt, displayName; filter: role, scope, status, keyword | GET 幂等 | FORBIDDEN |
| API-USER-003 | 更新用户基础信息 | FR-001 / 用户维护 | PATCH `/users/{userId}` | SYSTEM_ADMIN, SCHOOL_ADMIN 或本人有限字段 | body: displayName?, email?, phone?, status?; header: If-Match | User | 无 | If-Match | VERSION_CONFLICT, FORBIDDEN |
| API-USER-004 | 绑定用户角色与范围 | FR-001, FR-002 / 权限配置 | PUT `/users/{userId}/roles-scopes` | SYSTEM_ADMIN, SCHOOL_ADMIN | body: roles[], scopes[]; header: If-Match | User.roles, User.scopes, version | 无 | If-Match | FORBIDDEN, VERSION_CONFLICT, VALIDATION_ERROR |
| API-USER-005 | 停用用户 | FR-001, FR-032 / 账号停用，不物理删除 | POST `/users/{userId}/disable` | SYSTEM_ADMIN, SCHOOL_ADMIN | body: reason | User.status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |
| API-USER-006 | 管理员重置本地密码 | FR-001 / 内置账号初始化 | POST `/users/{userId}/password-reset` | SYSTEM_ADMIN, SCHOOL_ADMIN | body: deliveryMode: in_app / email_optional / manual | resetStatus, temporaryPasswordVisibleOnce? | 无 | 必须 Idempotency-Key | FORBIDDEN, DEPENDENCY_UNAVAILABLE |
| API-ROLE-001 | 角色与权限元数据 | FR-001, FR-002 / 前端权限渲染 | GET `/roles` | SYSTEM_ADMIN, SCHOOL_ADMIN | query: includePermissions? | roles[], permissions[] | 无 | GET 幂等 | FORBIDDEN |

### 5.2 学校级配置中心

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-CFG-001 | 获取课程质量阈值 | FR-011, FR-031 / 学校级质量规则 | GET `/school-settings/quality-rules` | SCHOOL_ADMIN, MAJOR_OWNER 只读 | 无 | totalCreditsMin/Max, semesterCreditsMin/Max, practiceCreditRatioMin, indicatorSupportMinCount, requireHighOrMediumSupport, version | 无 | GET 幂等 | FORBIDDEN |
| API-CFG-002 | 更新课程质量阈值 | FR-011, FR-031 / 学校级质量规则配置 | PATCH `/school-settings/quality-rules` | SCHOOL_ADMIN | body: threshold fields; header: If-Match | qualityRules, version | 无 | If-Match | VERSION_CONFLICT, VALIDATION_ERROR |
| API-CFG-003 | 获取 AI 配置 | FR-031, NFR-007 / AI Provider 治理 | GET `/school-settings/ai` | SYSTEM_ADMIN, SCHOOL_ADMIN | 无 | providers[], externalProviderEnabled, localProviderSlot, studentDataToExternalAllowed, trainingAllowed, retentionPolicy, version | 无 | GET 幂等 | FORBIDDEN |
| API-CFG-004 | 更新 AI 配置 | FR-031, NFR-010 / AI 安全策略 | PATCH `/school-settings/ai` | SCHOOL_ADMIN | body: providerEnablement, dataPolicy, retentionPolicy, desensitizationPolicy; header: If-Match | aiSettings, version | 无 | If-Match | AI_PROVIDER_DISABLED, VERSION_CONFLICT |
| API-CFG-005 | 获取能力等级配置 | FR-023, FR-031 / 学校级能力等级 | GET `/school-settings/ability-levels` | SCHOOL_ADMIN, MAJOR_OWNER, MENTOR, STUDENT 只读 | 无 | levels: code, name, description, evidenceRequirementTemplate, version | 无 | GET 幂等 | FORBIDDEN |
| API-CFG-006 | 更新能力等级配置 | FR-023, FR-031 / 学校级能力等级 | PUT `/school-settings/ability-levels` | SCHOOL_ADMIN | body: levels[]; header: If-Match | levels[], version | 无 | If-Match | VALIDATION_ERROR, VERSION_CONFLICT |
| API-CFG-007 | 获取成长预警配置 | FR-024, FR-031 / 学校级预警可见范围 | GET `/school-settings/growth-warning-rules` | SCHOOL_ADMIN, MAJOR_OWNER 只读 | 无 | warningTypes[], visibilityRules, notificationRules, version | 无 | GET 幂等 | FORBIDDEN |
| API-CFG-008 | 更新成长预警配置 | FR-024, FR-031 / 学校级预警配置 | PATCH `/school-settings/growth-warning-rules` | SCHOOL_ADMIN | body: enabled, levels, visibility, notification, exportAllowed; header: If-Match | warningRules, version | 无 | If-Match | VERSION_CONFLICT, VALIDATION_ERROR |
| API-CFG-009 | 获取临时代码策略 | FR-014, FR-031 / 课程临时代码治理 | GET `/school-settings/course-code-policy` | SCHOOL_ADMIN, MAJOR_OWNER, ACADEMIC_ADMIN | 无 | tempCodeFormat, allowPublishWithTemporaryCode, version | 无 | GET 幂等 | FORBIDDEN |
| API-CFG-010 | 更新临时代码策略 | FR-014, FR-031 / 发布前临时代码规则 | PATCH `/school-settings/course-code-policy` | SCHOOL_ADMIN | body: allowPublishWithTemporaryCode; header: If-Match | courseCodePolicy, version | 无 | If-Match | VERSION_CONFLICT |
| API-CFG-011 | 获取学校级 AI 策略 | FR-030, NFR-007, NFR-010 / AI 安全策略 | GET `/school/ai-policy` | SYSTEM_ADMIN, SCHOOL_ADMIN | 无 | externalProviderEnabled, allowStudentDataToExternalProvider, allowKnowledgeDocumentsToExternalProvider, requireCitationForCourseContent, requireHumanReviewForAllAIOutputs, maskSensitiveFieldsBeforeAI, version | 无 | GET 幂等 | FORBIDDEN |
| API-CFG-012 | 更新学校级 AI 策略 | FR-030, NFR-007, NFR-010 / AI 安全策略配置 | PUT `/school/ai-policy` | SCHOOL_ADMIN | body: policy fields; header: If-Match | aiPolicy, version | 无 | If-Match | VERSION_CONFLICT, AI_POLICY_FORBIDS_EXTERNAL_PROVIDER |

### 5.3 看板与多专业管理

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-DSH-001 | 学校级看板 | FR-004 / 学校级多学院多专业试点 | GET `/dashboards/school` | SCHOOL_ADMIN | query: collegeId?, status? | collegeCount, majorCount, majorStageStats, maturityStats, riskCount, pendingReviewCount, updatedAt | filter: collegeId, status | GET 幂等 | FORBIDDEN |
| API-DSH-002 | 学院级看板 | FR-004 / 学院专业状态 | GET `/dashboards/colleges/{collegeId}` | SCHOOL_ADMIN, COLLEGE_MANAGER | path: collegeId | majorCount, majorCards[], riskSummary, pendingReviewCount | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-DSH-003 | 专业级看板 | FR-004, FR-005 / 专业工作台入口 | GET `/dashboards/majors/{majorId}` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER | path: majorId | constructionStage, maturityScore, maturityLevel, completeness, risks[], pendingReviews[], currentVersions, recentChanges | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-MAJ-001 | 创建专业 | FR-003 / 新建专业 | POST `/majors` | SCHOOL_ADMIN, COLLEGE_MANAGER | body: collegeId, majorName, majorCode?, educationYears?, degreeType?, ownerUserId, targetIndustries?, targetJobRoles? | Major | 无 | 必须 Idempotency-Key | CONFLICT, VALIDATION_ERROR |
| API-MAJ-002 | 专业列表 | FR-003, FR-004 / 多专业列表 | GET `/majors` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER | query: page, pageSize, collegeId?, status?, constructionStage?, keyword | items: Major[] | sort: majorName, maturityScore, updatedAt; filter: collegeId, stage, status, keyword | GET 幂等 | FORBIDDEN |
| API-MAJ-003 | 专业详情 | FR-003, FR-005 / 专业基础信息 | GET `/majors/{majorId}` | 授权范围内用户 | path: majorId | Major | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-MAJ-004 | 更新专业 | FR-003 / 专业基础信息维护 | PATCH `/majors/{majorId}` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER | body: editable Major fields; header: If-Match | Major | 无 | If-Match | VERSION_CONFLICT, FORBIDDEN |
| API-MAJ-005 | 培养方案版本列表 | FR-003, FR-032 / 版本管理 | GET `/majors/{majorId}/major-versions` | 授权范围内用户 | query: status? | items: MajorVersion[] | sort: createdAt, status; filter: status | GET 幂等 | FORBIDDEN |
| API-MAJ-006 | 创建培养方案版本草案 | FR-003, FR-012 / 新版本不覆盖历史 | POST `/majors/{majorId}/major-versions` | MAJOR_OWNER, ACADEMIC_ADMIN | body: versionName, basedOnVersionId?, effectiveGrade? | MajorVersion | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, VALIDATION_ERROR |
| API-MAJ-007 | 获取专业 AI 上下文 | FR-005, FR-030 / 专业上下文补充 | GET `/majors/{majorId}/ai-context` | MAJOR_OWNER, SCHOOL_ADMIN, COLLEGE_MANAGER 只读 | path: majorId | majorBackground, targetIndustries[], targetJobGroups[], professionalTerms[], courseDesignPreferences, generationPreferences, contextVersion | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-MAJ-008 | 更新专业 AI 上下文 | FR-005, FR-030 / 专业上下文补充 | PUT `/majors/{majorId}/ai-context` | MAJOR_OWNER | body: majorBackground?, targetIndustries?, targetJobGroups?, professionalTerms?, courseDesignPreferences?, generationPreferences?; header: If-Match | aiContext, contextVersion | 无 | If-Match | AI_CONTEXT_VALIDATION_ERROR, VERSION_CONFLICT |

### 5.4 AI 草案、审核与发布通用接口

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-AI-001 | 创建 AI 专业画像任务 | FR-006, FR-012, FR-030 / 专业画像草案 | POST `/majors/{majorId}/ai/profile-tasks` | MAJOR_OWNER | body: inputSourceIds?, promptContext?, targetIndustries?, targetJobRoles? | taskId, status | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED, VALIDATION_ERROR |
| API-AI-002 | 创建 AI 批量草案任务 | FR-006～FR-012 / 一键生成多类草案但分别审核 | POST `/majors/{majorId}/ai/batch-draft-tasks` | MAJOR_OWNER | body: draftTypes[]: profile/obe/curriculum/support_matrix/quality_analysis | taskId, subTaskIds[] | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED, VALIDATION_ERROR |
| API-AI-003 | 查询 AI / 异步任务 | FR-012, FR-030, NFR-009 / 任务状态展示 | GET `/tasks/{taskId}` | 任务发起人或授权管理员 | path: taskId | taskId, taskType, status, progressPercent, resultObjectType, resultObjectId, errorCode, errorMessage, createdAt, finishedAt | 无 | GET 幂等 | TASK_NOT_FOUND, FORBIDDEN |
| API-AI-004 | 取消异步任务 | FR-012, NFR-009 / 取消未执行任务 | POST `/tasks/{taskId}/cancel` | 任务发起人或管理员 | body: reason | taskId, status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, TASK_ALREADY_RUNNING |
| API-DRF-001 | 草案列表 | FR-012 / 草案区 | GET `/drafts` | 授权范围内用户 | query: page, pageSize, objectType?, objectId?, status?, createdBy? | items: Draft[] | sort: createdAt, updatedAt; filter: objectType, status, createdBy | GET 幂等 | FORBIDDEN |
| API-DRF-002 | 草案详情 | FR-012 / 查看 AI 输出与人工编辑 | GET `/drafts/{draftId}` | 草案所属资源授权用户 | path: draftId | draftId, draftType, content, aiRunId, status, editableFields, citations?, version | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-DRF-003 | 编辑草案 | FR-012 / 人工编辑 | PATCH `/drafts/{draftId}` | 草案所属资源编辑权限 | body: contentPatch, editComment?; header: If-Match | Draft | 无 | If-Match | VERSION_CONFLICT, BUSINESS_STATE_INVALID |
| API-REV-001 | 提交审核 | FR-012 / 最小审核状态流 | POST `/drafts/{draftId}/submit-review` | 草案编辑权限用户 | body: reviewerUserIds?, comment? | reviewId, status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-REV-002 | 审核通过并发布 | FR-012, FR-032 / 人工审核发布 | POST `/reviews/{reviewId}/approve` | 对应审核权限角色 | body: comment, publishScope? | reviewStatus, publishedObjectId, versionId | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |
| API-REV-003 | 审核驳回 | FR-012 / 审核驳回 | POST `/reviews/{reviewId}/reject` | 对应审核权限角色 | body: reason | reviewStatus | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-REV-004 | 要求修改 | FR-012 / 需修改状态 | POST `/reviews/{reviewId}/request-changes` | 对应审核权限角色 | body: changeRequests[] | reviewStatus | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-REV-005 | 待审核列表 | FR-004, FR-012 / 待办中心 | GET `/reviews/pending` | 当前用户 | query: page, pageSize, objectType?, majorId?, status? | items: ReviewTask[] | sort: createdAt; filter: objectType, majorId, status | GET 幂等 | FORBIDDEN |

### 5.5 OBE、课程体系、课程地图与支撑矩阵

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-OBE-001 | 获取 OBE 结构 | FR-007 / 培养目标、毕业要求、指标点 | GET `/major-versions/{majorVersionId}/obe` | 授权范围内用户 | path: majorVersionId | objectives[], requirements[], indicators[], relationSummary | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-OBE-002 | 保存 OBE 草案 | FR-007, FR-012 / 人工编辑 OBE | PUT `/major-versions/{majorVersionId}/obe/draft` | MAJOR_OWNER | body: objectives[], requirements[], indicators[]; header: If-Match | obeDraft, version | 无 | If-Match | VALIDATION_ERROR, VERSION_CONFLICT |
| API-OBE-003 | 生成 OBE 草案任务 | FR-007, FR-012 / AI OBE 草案 | POST `/major-versions/{majorVersionId}/obe/generation-tasks` | MAJOR_OWNER | body: sourceDocumentIds?, promptContext? | taskId, status | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED |
| API-CUR-001 | 创建课程体系草案 | FR-008 / 动态课程体系 | POST `/major-versions/{majorVersionId}/curricula` | MAJOR_OWNER, ACADEMIC_ADMIN | body: versionName, basedOnCurriculumVersionId? | CurriculumVersion | 无 | 必须 Idempotency-Key | VALIDATION_ERROR |
| API-CUR-002 | 获取课程体系版本 | FR-008, FR-009 / 课程体系查看 | GET `/curricula/{curriculumVersionId}` | 授权范围内用户 | path: curriculumVersionId | CurriculumVersion, courses[], relations[] | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-CUR-003 | 更新课程体系元数据 | FR-008 / 课程体系编辑 | PATCH `/curricula/{curriculumVersionId}` | MAJOR_OWNER, ACADEMIC_ADMIN | body: versionName?, effectiveGrade?, description?; header: If-Match | CurriculumVersion | 无 | If-Match | VERSION_CONFLICT |
| API-CUR-004 | 添加课程到课程体系 | FR-008, FR-014 / 课程体系课程清单 | POST `/curricula/{curriculumVersionId}/courses` | MAJOR_OWNER, ACADEMIC_ADMIN | body: courseId, courseVersionId?, courseModule, suggestedSemester, credits, hours | CurriculumCourse | 无 | 必须 Idempotency-Key | CONFLICT, BUSINESS_STATE_INVALID |
| API-CUR-005 | 更新课程体系内课程 | FR-008, FR-009 / 课程地图编辑 | PATCH `/curricula/{curriculumVersionId}/courses/{curriculumCourseId}` | MAJOR_OWNER, ACADEMIC_ADMIN | body: module?, semester?, credits?, hours?; header: If-Match | CurriculumCourse | 无 | If-Match | VERSION_CONFLICT |
| API-CUR-006 | 归档课程体系内课程 | FR-008, FR-032 / 不物理删除 | POST `/curricula/{curriculumVersionId}/courses/{curriculumCourseId}/archive` | MAJOR_OWNER, ACADEMIC_ADMIN | body: reason | status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-CUR-007 | 批量保存课程关系 | FR-008 / 先修关系、推荐前置、不可同学期 | PUT `/curricula/{curriculumVersionId}/course-relations` | MAJOR_OWNER, ACADEMIC_ADMIN | body: relations[]: sourceCourseId, targetCourseId, relationType | relations[], validationWarnings[] | 无 | If-Match 可选 | VALIDATION_ERROR, BUSINESS_STATE_INVALID |
| API-CUR-008 | 四年课程地图 | FR-009 / 8 学期课程地图 | GET `/curricula/{curriculumVersionId}/course-map` | 授权范围内用户 | path: curriculumVersionId | semesters[1..8], creditLoad, warnings[] | 无 | GET 幂等 | FORBIDDEN |
| API-CUR-009 | 学分学时统计 | FR-009 / 学分、实践比例统计 | GET `/curricula/{curriculumVersionId}/statistics` | 授权范围内用户 | path: curriculumVersionId | totalCredits, theoryHours, practiceHours, moduleRatios, semesterLoads, thresholdSnapshot | 无 | GET 幂等 | FORBIDDEN |
| API-SUP-001 | 获取课程支撑矩阵 | FR-010 / 支撑关系查看 | GET `/curricula/{curriculumVersionId}/support-matrix` | 授权范围内用户 | query: courseId?, requirementId?, indicatorId? | matrixRows[], indicators[], supportSummary | filter: courseId, requirementId, indicatorId | GET 幂等 | FORBIDDEN |
| API-SUP-002 | 保存支撑矩阵草案 | FR-010, FR-012 / 人工确认支撑关系 | PUT `/curricula/{curriculumVersionId}/support-matrix/draft` | MAJOR_OWNER, TEACHER 授权课程范围 | body: supportItems[] | supportItems[], riskWarnings[] | 无 | If-Match 可选 | VALIDATION_ERROR, FORBIDDEN |
| API-SUP-003 | 生成支撑矩阵草案任务 | FR-010, FR-012 / AI 支撑关系草案 | POST `/curricula/{curriculumVersionId}/support-matrix/generation-tasks` | MAJOR_OWNER | body: source: curriculum / obe / course_targets | taskId | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED |
| API-QA-001 | 创建课程体系质量分析任务 | FR-011, FR-030 / 规则风险 + AI 建议 | POST `/curricula/{curriculumVersionId}/quality-analysis-tasks` | MAJOR_OWNER, COLLEGE_MANAGER | body: analysisTypes[], includeAiSuggestion | taskId | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED, VALIDATION_ERROR |
| API-QA-002 | 获取质量分析结果 | FR-011, FR-030 / 风险清单与依据 | GET `/quality-analyses/{analysisId}` | 授权范围内用户 | path: analysisId | maturityScore, maturityLevel, risks[], thresholdSnapshot, aiSuggestions?, relatedDraftIds[] | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |

### 5.6 课程库、知识库与课程内容

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-CRS-001 | 创建平台课程 | FR-014 / 平台课程库 | POST `/courses` | MAJOR_OWNER, ACADEMIC_ADMIN | body: courseCode?, courseName, courseType?, credits?, hours?, allowTemporaryCode? | Course | 无 | 必须 Idempotency-Key | CONFLICT, VALIDATION_ERROR |
| API-CRS-002 | 课程列表 | FR-014 / 课程库检索 | GET `/courses` | 授权范围内用户 | query: page, pageSize, keyword?, courseCode?, courseType?, isTemporaryCode?, status? | items: Course[] | sort: courseCode, courseName, createdAt; filter: type, tempCode, status, keyword | GET 幂等 | FORBIDDEN |
| API-CRS-003 | 课程详情 | FR-014 / 课程信息 | GET `/courses/{courseId}` | 授权范围内用户 | path: courseId | Course | 无 | GET 幂等 | NOT_FOUND, FORBIDDEN |
| API-CRS-004 | 更新平台课程 | FR-014 / 正式代码替换临时代码 | PATCH `/courses/{courseId}` | MAJOR_OWNER, ACADEMIC_ADMIN | body: courseCode?, courseName?, credits?, hours?; header: If-Match | Course | 无 | If-Match | CONFLICT, VERSION_CONFLICT |
| API-CRS-005 | 创建专业课程版本 | FR-014 / 专业内课程版本 | POST `/courses/{courseId}/versions` | MAJOR_OWNER, TEACHER 授权课程范围 | body: majorId, courseObjectives?, cases?, practiceProjects?, assessmentMethods? | CourseVersion | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, FORBIDDEN |
| API-CRS-006 | 更新专业课程版本 | FR-014, FR-017 / 课程目标与能力映射 | PATCH `/course-versions/{courseVersionId}` | MAJOR_OWNER, TEACHER 授权课程范围 | body: courseObjectives?, indicatorIds?, contentSummary?; header: If-Match | CourseVersion | 无 | If-Match | VERSION_CONFLICT |
| API-FILE-001 | 创建上传会话 | FR-015, FR-021, NFR-009 / 知识库资料、学习证据、企业材料、报告附件上传 | POST `/files/upload-sessions` | 已登录，按 businessObjectType / purpose 鉴权 | body: businessObjectType, businessObjectId?, fileName, fileSize, mimeType, fileHash?, purpose | fileId, uploadSessionId, uploadUrl, expiresInSeconds=300, requiredHeaders, status | 无 | 必须 Idempotency-Key；同一 fileHash + Key 返回同一会话 | FILE_TOO_LARGE, FILE_TYPE_NOT_ALLOWED, FILE_ACCESS_DENIED |
| API-FILE-002 | 确认上传完成 | FR-015, FR-021, NFR-009 / 触发安全扫描 | POST `/files/{fileId}/complete-upload` | 上传会话创建人或管理员 | path: fileId | fileId, status, securityScanStatus | 无 | 幂等；重复确认返回当前状态 | FILE_NOT_FOUND, BUSINESS_STATE_INVALID |
| API-FILE-003 | 文件元数据 | FR-015, FR-021, NFR-012 / 文件状态查看 | GET `/files/{fileId}` | 文件授权用户 | path: fileId | file metadata, securityScanStatus, parseStatus, archivedFlag | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-FILE-004 | 获取文件下载地址 | FR-002, FR-027, NFR-012 / 私有文件下载 | POST `/files/{fileId}/download-url` | 文件授权用户 | body: purpose, authorizationId? | downloadUrl 或 proxyDownloadUrl, expiresInSeconds=300, fileName | 无 | 不强制幂等；每次生成 URL 需审计 | FILE_ACCESS_DENIED, FILE_SECURITY_SCAN_PENDING, FILE_SECURITY_SCAN_FAILED, FILE_AUTHORIZATION_REQUIRED |
| API-FILE-005 | 获取文件预览地址 | FR-015, FR-021, FR-026 / 文件预览 | POST `/files/{fileId}/preview-url` | 文件授权用户 | body: purpose, authorizationId? | previewUrl 或 proxyPreviewUrl, expiresInSeconds=300, previewType | 无 | 不强制幂等；每次生成 URL 需审计 | FILE_ACCESS_DENIED, FILE_SECURITY_SCAN_PENDING, FILE_SECURITY_SCAN_FAILED |
| API-FILE-006 | 重新安全扫描 | FR-015, NFR-012 / 扫描异常处理 | POST `/files/{fileId}/rescan` | SYSTEM_ADMIN, SCHOOL_ADMIN, KNOWLEDGE_ADMIN 授权范围 | body: reason | taskId, securityScanStatus | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-KB-001 | 创建知识库文档 | FR-015 / 文件或 URL 入库 | POST `/knowledge/documents` | TEACHER, KNOWLEDGE_ADMIN | body: courseId?, sourceType, fileId? 或 sourceUrl, sourceName, metadata? | KnowledgeDocument | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, FILE_SECURITY_SCAN_PENDING |
| API-KB-002 | 知识库文档列表 / 检索 | FR-015, FR-016 / 资料管理和基础中文关键词检索 | GET `/knowledge/documents` | TEACHER, KNOWLEDGE_ADMIN, MAJOR_OWNER | query: page, pageSize, q?, courseId?, majorId?, documentType?, reviewStatus?, credibilityLevel?, usageScope?, importedFrom?, importedTo? | items: KnowledgeDocument[], matchedFields[] | sort: 默认 -updatedAt；可选 importedAt, publishDate, credibilityLevel; filter: 结构化筛选 + 关键词 | GET 幂等 | FORBIDDEN, KNOWLEDGE_SEARCH_UNSUPPORTED_FILTER |
| API-KB-003 | 知识库文档详情 | FR-015, FR-016 / 来源与许可查看 | GET `/knowledge/documents/{documentId}` | 授权范围内用户 | path: documentId | KnowledgeDocument, chunksSummary, reviewHistory | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-KB-004 | 更新知识库文档元数据 | FR-016 / 可信度、版权许可维护 | PATCH `/knowledge/documents/{documentId}` | KNOWLEDGE_ADMIN | body: author?, organization?, publishDate?, credibilityLevel?, copyrightStatus?, licenseType?, usageScope?, tags?; header: If-Match | KnowledgeDocument | 无 | If-Match | VERSION_CONFLICT, VALIDATION_ERROR |
| API-KB-005 | 提交知识库资料审核结果 | FR-016 / 人工审核 | POST `/knowledge/documents/{documentId}/review` | KNOWLEDGE_ADMIN | body: reviewStatus: trusted/limited_use/rejected/disabled, comment | KnowledgeDocument.reviewStatus | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-KB-006 | 创建文档解析任务 | FR-015, NFR-009 / 文档解析、摘要、标签 | POST `/knowledge/documents/{documentId}/parse-tasks` | TEACHER, KNOWLEDGE_ADMIN | body: parseOptions? | taskId, status | 无 | 必须 Idempotency-Key | FILE_SECURITY_SCAN_PENDING, FILE_SECURITY_SCAN_FAILED |
| API-KB-007 | 知识片段检索 | FR-016, ARCH-DEC-009 / 结构化 + 基础中文关键词检索 | GET `/knowledge-chunks/search` | TEACHER, KNOWLEDGE_ADMIN, MAJOR_OWNER | query: q, courseId?, documentId?, onlyUsableForCitation?, page?, pageSize? | chunks[], documents[], matchedFields[], filtersApplied | 默认按标题/标签/摘要/片段匹配优先级 + 可信度 + 更新时间排序；pageSize 最大 100 | GET 幂等 | KNOWLEDGE_SEARCH_QUERY_TOO_LONG, KNOWLEDGE_CITATION_SOURCE_NOT_ALLOWED |
| API-KB-008 | 引用记录列表 | FR-016, FR-017 / 生成内容引用追溯 | GET `/citations` | 授权范围内用户 | query: generatedContentId?, documentId?, courseId? | items: CitationRecord[] | sort: citedAt; filter: generatedContentId, documentId, courseId | GET 幂等 | FORBIDDEN |
| API-CC-001 | 创建课程内容生成任务 | FR-017 / 课程大纲、实验、作业草案 | POST `/course-versions/{courseVersionId}/content-generation-tasks` | TEACHER 授权课程范围 | body: contentTypes[], knowledgeDocumentIds?, generationContext? | taskId | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED, VALIDATION_ERROR |
| API-CC-002 | 课程内容详情 | FR-017 / 课程内容草案与正式版本 | GET `/course-contents/{contentId}` | 授权课程用户 | path: contentId | contentId, courseVersionId, contentType, content, citations[], status, version | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-CC-003 | 编辑课程内容草案 | FR-017, FR-012 / 教师编辑 | PATCH `/course-contents/{contentId}` | TEACHER 授权课程范围 | body: contentPatch; header: If-Match | CourseContent | 无 | If-Match | VERSION_CONFLICT, BUSINESS_STATE_INVALID |
| API-CC-004 | 发布课程内容 | FR-017, FR-012 / 教师审核发布 | POST `/course-contents/{contentId}/publish` | TEACHER 授权课程范围 | body: reviewComment | publishedVersionId, status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |

### 5.7 学生导入、画像、申诉与个性化路径

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-IMP-001 | 导入模板元数据列表 | FR-025 / 学生名单、课程修读记录模板 | GET `/import-templates` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER, ACADEMIC_ADMIN, MENTOR, TEACHER 授权范围 | query: templateType?, format? | items: templateType, templateName, templateVersion, supportedFormats, downloadUrl, updatedAt | filter: templateType, format | GET 幂等 | IMPORT_TEMPLATE_NOT_FOUND |
| API-IMP-002 | 下载导入模板 | FR-025 / 平台标准 Excel / CSV 模板 | GET `/import-templates/{templateType}/download` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER, ACADEMIC_ADMIN, MENTOR, TEACHER 授权范围 | path: templateType: student_roster / course_enrollment; query: format=xlsx/csv | 文件流，Content-Disposition 附带模板版本 | 无 | GET 幂等 | IMPORT_TEMPLATE_FORMAT_NOT_SUPPORTED |
| API-IMP-003 | 导入预校验 | FR-025 / 重复与冲突识别 | POST `/imports/precheck` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER, ACADEMIC_ADMIN, MENTOR, TEACHER 授权范围 | body: importType, templateVersion, fileId, options | importId, totalRows, newRows, duplicateRows, conflictRows, errorRows, previewItems[] | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, FILE_SECURITY_SCAN_PENDING, IMPORT_FILE_TEMPLATE_MISMATCH |
| API-IMP-004 | 提交导入 | FR-025 / 用户选择跳过、更新或终止 | POST `/imports/{importId}/commit` | 发起人或管理员 | body: duplicateStrategy: skip/update/abort, conflictConfirmations[] | taskId, importStatus | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, VALIDATION_ERROR |
| API-IMP-005 | 导入结果 | FR-025 / 导入日志与错误明细 | GET `/imports/{importId}` | 发起人或管理员 | path: importId | importStatus, counts, failureReasons[], errorFileId?, templateVersion, logSummary | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-STU-001 | 学生列表 | FR-018, FR-025 / 学生档案检索 | GET `/students` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER, MENTOR, TEACHER 授权范围 | query: page, pageSize, collegeId?, majorId?, grade?, className?, mentorUserId?, status?, keyword? | items: Student[] | sort: studentNo, studentName, updatedAt; filter: org, grade, mentor, status, keyword | GET 幂等 | FORBIDDEN |
| API-STU-002 | 学生详情 | FR-018 / 学生档案 | GET `/students/{studentId}` | 学生本人、导师、授权教师、管理者 | path: studentId | Student, courseRecordSummary, evidenceSummary | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-STU-003 | 更新学生基础信息 | FR-018, FR-025 / 可更正基础信息 | PATCH `/students/{studentId}` | SCHOOL_ADMIN, MENTOR 授权范围，学生本人有限字段 | body: mentorUserId?, interestDirection?, developmentGoal?, contact?; header: If-Match | Student | 无 | If-Match | VERSION_CONFLICT, FORBIDDEN |
| API-STU-004 | 学生课程修读记录 | FR-025 / 成绩证据定位 | GET `/students/{studentId}/course-records` | 学生本人、导师、授权教师 | query: page, pageSize, semester?, courseId?, studyStatus? | items: StudentCourseRecord[] | sort: studySemester, courseName; filter: semester, course, status | GET 幂等 | FORBIDDEN |
| API-PRF-001 | 学生画像 | FR-018 / 动态画像查看 | GET `/students/{studentId}/profile` | 学生本人、导师、授权教师 | path: studentId | profileSummary, dimensions[], basis[], generatedAt, version | 无 | GET 幂等 | FORBIDDEN |
| API-PRF-002 | 创建画像生成任务 | FR-018, FR-020 / 学生画像草案 | POST `/students/{studentId}/profile/generation-tasks` | MENTOR 授权范围 | body: includeDimensions[], sourceRange? | taskId | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED, FORBIDDEN |
| API-APL-001 | 提交画像 / 建议 / 预警申诉 | FR-019 / 学生申诉与补充说明 | POST `/students/{studentId}/profile-appeals` | 学生本人 | body: appealType, targetObjectType, targetObjectId?, description, fileIds? | appealId, status | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, FORBIDDEN |
| API-APL-002 | 申诉列表 | FR-019 / 导师初审、分派处理 | GET `/profile-appeals` | 学生本人、MENTOR、TEACHER、MAJOR_OWNER 授权范围 | query: page, pageSize, studentId?, status?, appealType?, assigneeUserId? | items: Appeal[] | sort: createdAt; filter: student, status, type, assignee | GET 幂等 | FORBIDDEN |
| API-APL-003 | 分派申诉 | FR-019 / 导师转教师或专业负责人 | POST `/profile-appeals/{appealId}/route` | MENTOR 初审人 | body: targetRole, assigneeUserId, reason | appealStatus, assignee | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |
| API-APL-004 | 处理申诉 | FR-019 / 采纳、驳回、需补充 | POST `/profile-appeals/{appealId}/resolve` | 当前处理人 | body: result: accepted/rejected/need_more_evidence, comment, relatedChangeIds? | appealStatus, result | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-PATH-001 | 生成培养路径推荐任务 | FR-020 / 个性化推荐 | POST `/students/{studentId}/development-path/generation-tasks` | MENTOR 授权范围，学生本人可请求草案 | body: targetDirection?, jobRoleId?, includeItems[] | taskId | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED |
| API-PATH-002 | 查看培养路径 | FR-020 / 推荐结果说明依据 | GET `/students/{studentId}/development-path` | 学生本人、导师 | path: studentId | recommendations[], basis[], mentorConfirmationStatus, version | 无 | GET 幂等 | FORBIDDEN |
| API-PATH-003 | 导师确认 / 调整培养路径 | FR-020 / 导师确认 | POST `/students/{studentId}/development-path/mentor-confirm` | MENTOR 授权范围 | body: confirmedItems[], adjustedItems[], comment | confirmationStatus, version | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |

### 5.8 学习证据、能力升级、成长任务与预警

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-EVD-001 | 提交学习证据 | FR-021 / 学生上传证据 | POST `/students/{studentId}/evidences` | 学生本人、教师/导师代录需授权 | body: evidenceType, title, description?, relatedCourseId?, fileIds?, reflection? | StudentEvidence | 无 | 必须 Idempotency-Key | FILE_SECURITY_SCAN_PENDING, VALIDATION_ERROR |
| API-EVD-002 | 学习证据列表 | FR-021 / 证据清单 | GET `/students/{studentId}/evidences` | 学生本人、导师、授权教师 | query: page, pageSize, evidenceType?, status?, relatedCourseId?, indicatorId? | items: StudentEvidence[] | sort: submittedAt, status; filter: type, status, course, indicator | GET 幂等 | FORBIDDEN |
| API-EVD-003 | 学习证据详情 | FR-021 / 证据追溯 | GET `/evidences/{evidenceId}` | 证据授权用户 | path: evidenceId | StudentEvidence, files[], mappedIndicators[], reviewHistory | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-EVD-004 | 创建证据解析任务 | FR-021 / 证据解析与能力映射草案 | POST `/evidences/{evidenceId}/parse-tasks` | 学生本人、导师、教师授权范围 | body: parseOptions? | taskId | 无 | 必须 Idempotency-Key | FILE_SECURITY_SCAN_PENDING, FILE_SECURITY_SCAN_FAILED |
| API-EVD-005 | 更新证据能力点映射草案 | FR-021 / 能力点映射 | PUT `/evidences/{evidenceId}/indicator-mappings` | 学生本人提交草案，教师/导师确认 | body: indicatorIds[], mappingReason? | mappedIndicators[], status | 无 | If-Match 可选 | VALIDATION_ERROR, FORBIDDEN |
| API-EVD-006 | 提交证据审核 | FR-021, FR-022 / 待审核 | POST `/evidences/{evidenceId}/submit-review` | 学生本人、教师/导师代录 | body: reviewerUserId?, comment? | reviewId, status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-EVD-007 | 审核证据 | FR-022 / 已确认、驳回、需补充 | POST `/evidence-reviews/{reviewId}/decision` | TEACHER 相关课程或 MENTOR 授权范围 | body: decision: verified/rejected/need_more_evidence, comment, confirmedIndicatorIds? | evidenceStatus, reviewResult | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |
| API-ABL-001 | 学生能力等级 | FR-022, FR-023 / 能力等级查看 | GET `/students/{studentId}/abilities` | 学生本人、导师、专业负责人授权范围 | query: requirementId?, indicatorId? | abilityRecords[], evidenceCoverageSummary | filter: requirementId, indicatorId | GET 幂等 | FORBIDDEN |
| API-ABL-002 | 创建能力升级建议任务 | FR-022 / AI 可建议升级 | POST `/students/{studentId}/ability-upgrade-suggestion-tasks` | MENTOR, TEACHER 授权范围 | body: indicatorIds?, evidenceIds? | taskId | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED |
| API-ABL-003 | 确认能力升级 | FR-022 / 教师或导师确认生效 | POST `/ability-upgrades/{upgradeSuggestionId}/confirm` | MENTOR 或 TEACHER 授权范围 | body: confirmedLevelCode, evidenceIds, comment | AbilityLevelRecord | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, VALIDATION_ERROR |
| API-GRW-001 | 成长任务列表 | FR-023 / 学生成长任务 | GET `/students/{studentId}/growth-tasks` | 学生本人、导师 | query: page, pageSize, status?, indicatorId? | items: GrowthTask[] | sort: dueDate, createdAt; filter: status, indicator | GET 幂等 | FORBIDDEN |
| API-GRW-002 | 更新成长任务状态 | FR-023 / 任务完成 | PATCH `/growth-tasks/{taskId}` | 学生本人、导师 | body: status?, completionEvidenceId?, comment?; header: If-Match | GrowthTask | 无 | If-Match | VERSION_CONFLICT, FORBIDDEN |
| API-GRW-003 | 学生徽章列表 | FR-023 / 已审核徽章 | GET `/students/{studentId}/badges` | 学生本人、导师 | query: page, pageSize, indicatorId?, levelCode? | items: GrowthBadge[] | sort: awardedAt; filter: indicator, level | GET 幂等 | FORBIDDEN |
| API-WRN-001 | 成长预警列表 | FR-024 / 辅导支持 | GET `/growth-warnings` | 学生本人、导师、授权教师、专业负责人汇总 | query: page, pageSize, studentId?, majorId?, warningType?, level?, status? | items: GrowthWarning[] | sort: createdAt, level; filter: student, major, type, level, status | GET 幂等 | FORBIDDEN |
| API-WRN-002 | 处理成长预警 | FR-024 / 导师处理 | POST `/growth-warnings/{warningId}/handle` | MENTOR 授权范围 | body: action: acknowledged/resolved/create_task, comment, growthTaskId? | warningStatus, handledAt | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |

### 5.9 毕业能力地图与对外授权

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-MAP-001 | 创建能力地图生成任务 | FR-026 / 毕业能力地图草案 | POST `/students/{studentId}/ability-maps/generation-tasks` | STUDENT 本人、MENTOR 授权范围 | body: majorVersionId?, includeSections[] | taskId | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, FORBIDDEN |
| API-MAP-002 | 能力地图列表 | FR-026 / 能力地图版本 | GET `/students/{studentId}/ability-maps` | 学生本人、导师、专业负责人授权范围 | query: page, pageSize, status? | items: GraduateAbilityMap[] | sort: createdAt, status; filter: status | GET 幂等 | FORBIDDEN |
| API-MAP-003 | 能力地图详情 | FR-026 / 内部审核版 | GET `/ability-maps/{abilityMapId}` | 学生本人、导师、专业负责人 | path: abilityMapId | GraduateAbilityMap, evidenceLinks[], reviewHistory | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-MAP-004 | 提交能力地图审核 | FR-026 / 导师或专业负责人审核 | POST `/ability-maps/{abilityMapId}/submit-review` | 学生本人、导师 | body: reviewerUserId?, comment? | reviewId, status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-MAP-005 | 能力地图审核决定 | FR-026 / 审核通过或退回 | POST `/ability-maps/{abilityMapId}/review-decision` | MENTOR, MAJOR_OWNER | body: decision: approved/rejected/request_changes, comment | status, reviewResult | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-MAP-006 | 创建能力地图 PDF 导出任务 | FR-026, FR-027 / PDF 导出 | POST `/ability-maps/{abilityMapId}/export-pdf-tasks` | 学生本人、导师、专业负责人；对外导出需学生授权 | body: exportType: internal/external, authorizationId? | taskId | 无 | 必须 Idempotency-Key | AUTHORIZATION_EXPIRED, AUTHORIZATION_REVOKED |
| API-AUTHZ-001 | 创建能力地图授权 | FR-027 / 范围授权 + 有效期 | POST `/ability-maps/{abilityMapId}/authorizations` | 学生本人 | body: granteeType, granteeId?, displayScopes[], validFrom, validUntil | authorizationId, status, accessTokenVisibleOnce, publicUrl? | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, BUSINESS_STATE_INVALID |
| API-AUTHZ-002 | 授权列表 | FR-027 / 学生查看授权 | GET `/students/{studentId}/ability-map-authorizations` | 学生本人 | query: page, pageSize, status? | items: AbilityMapAuthorization[] | sort: createdAt, validUntil; filter: status | GET 幂等 | FORBIDDEN |
| API-AUTHZ-003 | 撤销授权 | FR-027 / 可撤销 | POST `/ability-map-authorizations/{authorizationId}/revoke` | 学生本人 | body: reason | status: revoked, revokedAt | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID |
| API-PUB-001 | 公开授权能力地图访问 | FR-027 / 外部授权展示 | GET `/public/ability-maps/{accessToken}` | 匿名或企业导师，按授权令牌 | path: accessToken | authorizedAbilityMapView, authorizationScope, expiresAt | 无 | GET 幂等 | AUTHORIZATION_EXPIRED, AUTHORIZATION_REVOKED, FORBIDDEN |
| API-PUB-002 | 授权访问日志 | FR-027 / 学生查看谁访问过 | GET `/students/{studentId}/ability-map-access-logs` | 学生本人 | query: page, pageSize, authorizationId?, startAt?, endAt? | items: AccessLog[] | sort: accessedAt; filter: authorizationId, time range | GET 幂等 | FORBIDDEN |

### 5.10 用人单位、岗位能力与就业反馈

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-EMP-001 | 创建企业邀请 | FR-028 / 邀请制企业账号 | POST `/employer-invitations` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER | body: employerName?, contactEmail?, relatedCollegeId?, relatedMajorId?, cooperationType, invitationReason | invitationId, inviteUrl, status, expiresAt | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, FORBIDDEN |
| API-EMP-002 | 查看企业邀请信息 | FR-028 / 企业联系人接受邀请前查看 | GET `/public/employer-invitations/{invitationToken}` | 匿名 | path: invitationToken | invitationSummary, requiredFields, expiresAt | 无 | GET 幂等 | NOT_FOUND, BUSINESS_STATE_INVALID |
| API-EMP-003 | 企业联系人提交信息 | FR-028 / 补充企业基础信息 | POST `/public/employer-invitations/{invitationToken}/accept` | 匿名 | body: employerInfo, contactInfo, cooperationInfo, optionalMaterialFileIds? | employerId, reviewStatus | 无 | 必须 Idempotency-Key | VALIDATION_ERROR, BUSINESS_STATE_INVALID |
| API-EMP-004 | 企业账号审核 | FR-028 / 学校或学院审核 | POST `/employers/{employerId}/review` | SCHOOL_ADMIN, COLLEGE_MANAGER | body: decision: approved/rejected/need_more_material, comment | reviewStatus, employerId | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |
| API-EMP-005 | 用人单位列表 | FR-028 / 企业档案 | GET `/employers` | SCHOOL_ADMIN, COLLEGE_MANAGER, MAJOR_OWNER | query: page, pageSize, reviewStatus?, industry?, keyword? | items: Employer[] | sort: createdAt, employerName; filter: status, industry, keyword | GET 幂等 | FORBIDDEN |
| API-EMP-006 | 更新用人单位档案 | FR-028 / 企业信息维护 | PATCH `/employers/{employerId}` | SCHOOL_ADMIN, COLLEGE_MANAGER, EMPLOYER_MENTOR 有限字段 | body: employerInfo?, contactInfo?, status?; header: If-Match | Employer | 无 | If-Match | VERSION_CONFLICT, FORBIDDEN |
| API-JOB-001 | 创建岗位能力模型 | FR-028, FR-029 / 岗位能力需求 | POST `/employers/{employerId}/job-roles` | EMPLOYER_MENTOR 授权企业范围，MAJOR_OWNER | body: jobRoleName, description?, relatedMajorId?, abilityRequirements[] | JobRole | 无 | 必须 Idempotency-Key | FORBIDDEN, VALIDATION_ERROR |
| API-JOB-002 | 岗位能力模型列表 | FR-028 / 岗位模型查看 | GET `/job-roles` | EMPLOYER_MENTOR, MAJOR_OWNER, COLLEGE_MANAGER | query: page, pageSize, employerId?, majorId?, keyword? | items: JobRole[] | sort: createdAt, jobRoleName; filter: employer, major, keyword | GET 幂等 | FORBIDDEN |
| API-JOB-003 | 更新岗位能力映射 | FR-028, FR-029 / 岗位能力到专业能力指标点 | PUT `/job-roles/{jobRoleId}/ability-mappings` | EMPLOYER_MENTOR 授权范围，MAJOR_OWNER | body: mappings[]: jobAbilityName, indicatorIds[], importanceLevel? | mappings[] | 无 | If-Match 可选 | VALIDATION_ERROR, FORBIDDEN |
| API-EVL-001 | 提交实习 / 项目评价 | FR-028, FR-029 / 企业评价 | POST `/employers/{employerId}/evaluations` | EMPLOYER_MENTOR 授权范围 | body: studentId?, projectName?, evaluationType, content, indicatorRatings?, fileIds? | evaluationId, status | 无 | 必须 Idempotency-Key | FORBIDDEN, VALIDATION_ERROR |
| API-FDB-001 | 提交就业 / 毕业生反馈 | FR-029 / 就业反馈反哺 | POST `/employment-feedback` | EMPLOYER_MENTOR, MAJOR_OWNER | body: employerId?, majorId, feedbackType, content, relatedJobRoleId?, evidenceFileIds? | feedbackId, status | 无 | 必须 Idempotency-Key | FORBIDDEN, VALIDATION_ERROR |
| API-FDB-002 | 创建反馈改进建议任务 | FR-029, FR-030 / 反馈转课程体系建议 | POST `/employment-feedback/{feedbackId}/improvement-suggestion-tasks` | MAJOR_OWNER | body: targetCurriculumVersionId | taskId | 无 | 必须 Idempotency-Key | AI_PROVIDER_DISABLED, BUSINESS_STATE_INVALID |

### 5.11 报告、归档、审计、通知

| 接口编号 | 接口名称 | 关联需求 / 场景 | 方法与 URL | 权限 | 请求参数 | 响应字段 | 分页 / 排序 / 过滤 | 幂等性 | 主要错误码 |
|---|---|---|---|---|---|---|---|---|---|
| API-RPT-001 | 查询可生成报告类型 | FR-013, FR-030 / 报告类型与平台模板版本 | GET `/report-types` | 授权用户 | query: businessObjectType? | items: reportType, displayName, supportedFormats, currentTemplateVersion | filter: objectType | GET 幂等 | FORBIDDEN |
| API-RPT-002 | 创建报告生成任务 | FR-013, FR-030 / Word、PDF、Excel 导出 | POST `/reports/generate` | 对应业务资源导出权限 | body: reportType, businessObjectType, businessObjectId, exportFormat, authorizationId? | taskId, reportId, status, templateVersion | 无 | 必须 Idempotency-Key | FORBIDDEN, REPORT_AUTHORIZATION_REQUIRED, REPORT_TEMPLATE_NOT_FOUND, REPORT_DATA_VERSION_MISSING |
| API-RPT-003 | 报告列表 | FR-013, FR-030 / 报告中心 | GET `/reports` | 授权范围内用户 | query: page, pageSize, reportType?, businessObjectType?, majorId?, exportFormat?, status? | items: Report[] | sort: generatedAt, reportType; filter: type, object, major, format, status | GET 幂等 | FORBIDDEN |
| API-RPT-004 | 报告详情 | FR-013, FR-030 / 报告元数据与数据快照 | GET `/reports/{reportId}` | 报告授权用户 | path: reportId | reportId, reportType, fileId, templateVersion, dataVersionSnapshot, authorizationSnapshot?, generatedBy, generatedAt, status | 无 | GET 幂等 | FORBIDDEN, NOT_FOUND |
| API-RPT-005 | 获取报告下载地址 | FR-013, FR-026, FR-030 / 后端鉴权下载 | POST `/reports/{reportId}/download-url` | 报告授权用户 | body: purpose | downloadUrl 或 proxyDownloadUrl, expiresInSeconds=300 | 无 | 不强制幂等；生成 URL 需审计 | FORBIDDEN, AUTHORIZATION_REVOKED, REPORT_FILE_NOT_READY |
| API-ARC-001 | 归档资源 | FR-032 / 逻辑删除、作废、归档 | POST `/archives` | 资源归档权限用户 | body: resourceType, resourceId, archiveReason | archiveRecordId, status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |
| API-ARC-002 | 归档记录列表 | FR-032 / 管理员查看归档 | GET `/archives` | SYSTEM_ADMIN, SCHOOL_ADMIN 或资源管理员 | query: page, pageSize, resourceType?, majorId?, archivedBy?, startAt?, endAt? | items: ArchiveRecord[] | sort: archivedAt; filter: type, major, user, time | GET 幂等 | FORBIDDEN |
| API-ARC-003 | 恢复归档资源 | FR-032 / 管理员恢复误归档 | POST `/archives/{archiveRecordId}/restore` | SYSTEM_ADMIN, SCHOOL_ADMIN 或资源恢复权限 | body: reason | restoredResourceId, status | 无 | 必须 Idempotency-Key | BUSINESS_STATE_INVALID, FORBIDDEN |
| API-AUD-001 | 审计日志查询 | FR-002, FR-032, NFR-012 / 关键操作追溯 | GET `/audit-logs` | SYSTEM_ADMIN, SCHOOL_ADMIN，部分资源管理员 | query: page, pageSize, actorUserId?, action?, businessObjectType?, businessObjectId?, startAt?, endAt?, result? | items: AuditLog[] | sort: operationTime; filter: actor, action, object, time, result | GET 幂等 | FORBIDDEN |
| API-NOT-001 | 通知列表 | FR-004, FR-012, ARCH-DEC-010 / 站内通知轮询 | GET `/notifications` | 当前用户 | query: page, pageSize, readStatus?, notificationType?, sort? | items: Notification[] | sort: 默认 -createdAt; filter: read, type | GET 幂等 | UNAUTHORIZED |
| API-NOT-002 | 未读和待办数量 | FR-004, FR-012 / 顶部角标轮询 | GET `/notifications/unread-count` | 当前用户 | 无 | unreadCount, todoCount | 无 | GET 幂等 | UNAUTHORIZED |
| API-NOT-003 | 标记通知已读 | FR-012 / 待办阅读 | PATCH `/notifications/{notificationId}/read` | 通知接收人 | 无 | notificationId, readStatus, readAt | 无 | 幂等；重复标记返回当前状态 | FORBIDDEN, NOT_FOUND |
| API-TODO-001 | 待办列表 | FR-004, FR-012 / 审核待办和任务待办 | GET `/todos` | 当前用户 | query: page, pageSize, todoType?, status?, businessObjectType?, sort? | items: Todo[] | sort: 默认 -createdAt; filter: todoType, status, objectType | GET 幂等 | UNAUTHORIZED |


---

### 5.12 v0.2 关键接口补充契约

本节将 API-DEC-001～API-DEC-012 中对接口稳定性影响较大的规则补充为统一契约，前后端开发和 QA 验收应以本节为准。

#### 5.12.1 AI Provider、模型与 Prompt 上下文

**决策依据：API-DEC-003、API-DEC-004。**

1. 接口不写死具体 AI 厂商、模型名称、模型版本和模型参数。
2. AI Provider 使用 `providerId`、`providerType`、`providerCode`、`providerName` 表达。
3. AI 模型使用 `modelId`、`modelDisplayName`、`modelPurpose` 表达。
4. 外部云模型默认 disabled，学校管理员启用后才可调用。
5. 学校用户不开放完整 Prompt 模板编辑接口。
6. 专业负责人可维护专业 AI 上下文；平台 Prompt 模板由系统管理员 / 平台方维护版本。

AI 运行记录必须包含：

```json
{
  "aiRunId": "airun_001",
  "taskType": "major_profile_generation",
  "providerId": "prov_default_external",
  "providerType": "external_cloud",
  "modelId": "model_default_reasoning",
  "promptTemplateId": "prompt_major_profile",
  "promptVersion": "v1.0.3",
  "majorAIContextVersion": "v2",
  "outputSchemaVersion": "major_profile_schema_v1",
  "inputSummary": "脱敏后的输入摘要",
  "status": "succeeded"
}
```

相关错误码：

```text
AI_POLICY_FORBIDS_EXTERNAL_PROVIDER
AI_POLICY_FORBIDS_STUDENT_DATA
AI_CONTEXT_VALIDATION_ERROR
PROMPT_TEMPLATE_NOT_AVAILABLE
PROMPT_OUTPUT_SCHEMA_MISMATCH
AI_PROVIDER_DISABLED
AI_MODEL_NOT_AVAILABLE
AI_OUTPUT_SCHEMA_INVALID
```

#### 5.12.2 文件访问与签名 URL

**决策依据：API-DEC-005、API-DEC-011。**

文件接口采用“后端鉴权 + 短期签名 URL”为主，敏感场景可走后端代理。签名 URL 默认有效期为 5 分钟。系统不得返回永久公开 URL。

文件访问流程：

```text
请求上传 / 预览 / 下载
→ 后端校验登录态、角色、组织范围、资源权限、学生授权、文件安全扫描状态
→ 记录访问或下载意图
→ 返回短期有效签名 URL 或代理 URL
→ 记录审计日志
```

企业证明材料只允许 `.pdf`、`.docx`、`.jpg`、`.jpeg`、`.png`，且仅作为人工审核附件，不做 OCR、不进入 AI 上下文、不进入课程知识库。

#### 5.12.3 导入模板与导入预校验

**决策依据：API-DEC-006。**

MVP 提供后端导入模板下载接口，模板包含字段说明、必填标识、枚举值说明和示例行。导入预校验接口必须支持 `templateVersion`。

模板类型：

```text
student_roster
course_enrollment
```

导入流程：

```text
下载模板
→ 上传导入文件
→ precheck 预校验
→ 用户选择 skip / update / abort
→ commit 提交导入
→ 查询导入结果与错误明细
```

#### 5.12.4 审计日志脱敏摘要

**决策依据：API-DEC-007。**

审计日志 `beforeSummary` / `afterSummary` 只记录字段级脱敏摘要和业务对象引用，不保存敏感原文。

不得进入审计日志的内容包括：学生画像全文、成长预警详情、申诉原文、学习证据正文、教师 / 导师 / 企业评价全文、AI Prompt 原文、AI 输出长文本全文、文件正文、知识库片段全文、未脱敏联系方式、敏感黑名单字段、API Key、Provider Key、数据库密码、永久文件访问 URL。

#### 5.12.5 能力地图授权范围

**决策依据：API-DEC-008。**

能力地图授权采用章节级 `displayScopes`。MVP 不支持字段级、单条证据级、单条评价级授权。

创建授权请求示例：

```json
{
  "authorizedToType": "employer",
  "authorizedToId": "emp_001",
  "displayScopes": [
    "basic_info",
    "ability_overview",
    "ability_radar",
    "ability_levels",
    "evidence_summary",
    "portfolio",
    "mentor_review"
  ],
  "validFrom": "2026-06-06T00:00:00Z",
  "validUntil": "2026-07-06T00:00:00Z",
  "purpose": "job_application"
}
```

#### 5.12.6 通知、待办与任务状态轮询

**决策依据：API-DEC-009。**

MVP 通知和异步任务状态采用轮询。系统提供通知列表、未读数量、标记已读、待办列表和异步任务状态查询接口。MVP 不强制提供 WebSocket / SSE 实时推送。

建议前端轮询：

```text
通知未读数量：60 秒
当前页面关联任务 running 状态：3～5 秒
任务结束后停止轮询
```

#### 5.12.7 报告模板版本与数据快照

**决策依据：API-DEC-010。**

报告生成由后端根据 `reportType` 和 `exportFormat` 自动选择当前生效的平台模板版本；前端不选择模板版本。生成成功后必须保存：

```text
reportId
reportType
exportFormat
templateVersion
dataVersionSnapshot
authorizationSnapshot（如适用）
fileId
generatedBy
generatedAt
taskId
```

历史报告重新下载时默认下载已生成文件，不因后续业务数据变化而自动重算。

#### 5.12.8 课程知识库检索边界

**决策依据：API-DEC-012。**

MVP 课程知识库检索采用结构化筛选 + 基础中文关键词匹配 + 明确排序规则。MVP 不承诺复杂中文分词、同义词扩展、拼音检索、错别字纠错、语义相似检索、向量召回、图谱检索或 OCR 图片内容检索。

AI 引用检索必须强制过滤：当前学校空间、授权课程 / 专业范围、已审核、未禁用、安全扫描通过、版权和许可允许当前用途的资料。

---

## 6. 领域枚举

### 6.1 状态枚举

| 枚举 | 值 | 说明 |
|---|---|---|
| DraftStatus | ai_draft / human_edited / pending_review / published / rejected / need_changes / archived | AI 草案与审核状态。 |
| CurriculumStatus | draft / review_pending / approved / published / superseded / archived | 课程体系版本状态。 |
| EvidenceStatus | submitted / parsed / pending_review / verified / counted / rejected / need_more_evidence | 学习证据状态。 |
| KnowledgeDocumentStatus | pending_review / trusted / limited_use / rejected / disabled / archived | 知识库资料审核状态。 |
| FileSecurityStatus | uploaded / pending_security_scan / scan_passed / scan_failed / scan_error | 文件安全扫描状态。 |
| FileParseStatus | pending_parse / parsed / parse_failed | 文件解析状态。 |
| AbilityMapStatus | generating / draft / mentor_review / student_confirmed / authorized_export / archived | 能力地图状态。 |
| EmployerReviewStatus | invited / pending_info / pending_review / approved / rejected / need_more_material / disabled | 企业账号审核状态。 |
| TaskStatus | pending / queued / running / succeeded / failed / cancelled / retrying / archived | 异步任务状态。 |
| AuthorizationStatus | active / expired / revoked | 能力地图授权状态。 |

### 6.2 业务枚举（API-DEC-001、API-DEC-002、API-DEC-008、API-DEC-011）

接口请求和响应统一使用英文枚举值；前端展示中文名称；导入模板可使用中文名称，但导入时必须映射为英文枚举值。MVP 阶段不提供学校级枚举配置接口，后续可扩展学校级别名和启停配置。

#### 6.2.1 课程枚举

| 枚举 | 接口值 | 中文展示 |
|---|---|---|
| courseModule | general | 通识课程 |
| courseModule | discipline_basic | 学科基础课程 |
| courseModule | major_basic | 专业基础课程 |
| courseModule | major_core | 专业核心课程 |
| courseModule | direction | 方向课程 |
| courseModule | practice | 实践课程 |
| courseModule | graduation_design | 毕业设计 / 毕业论文 |
| courseModule | innovation_entrepreneurship | 创新创业课程 |
| courseModule | other | 其他 |
| courseNature | required | 必修 |
| courseNature | elective | 选修 |
| courseNature | limited_elective | 限选 |
| courseNature | optional_elective | 任选 |
| courseNature | other | 其他 |
| courseType | theory | 理论课 |
| courseType | practice | 实践课 |
| courseType | mixed | 理论 + 实践 |
| courseType | experiment | 实验课 |
| courseType | project | 项目课 |
| courseType | internship | 实习 / 实训 |
| courseType | seminar | 研讨课 |
| courseType | other | 其他 |

#### 6.2.2 学校、学位和行业字段

| 字段 | 接口值 / 类型 | 中文展示 / 规则 |
|---|---|---|
| schoolType | application_undergraduate | 应用型本科 |
| schoolType | research_university | 研究型大学 |
| schoolType | vocational_undergraduate | 职业本科 |
| schoolType | higher_vocational | 高职院校 |
| schoolType | independent_college | 独立学院 |
| schoolType | other | 其他 |
| degreeType | bachelor_of_engineering | 工学学士 |
| degreeType | bachelor_of_science | 理学学士 |
| degreeType | bachelor_of_management | 管理学学士 |
| degreeType | bachelor_of_arts | 文学学士 |
| degreeType | bachelor_of_education | 教育学学士 |
| degreeType | bachelor_of_economics | 经济学学士 |
| degreeType | bachelor_of_law | 法学学士 |
| degreeType | bachelor_of_agriculture | 农学学士 |
| degreeType | bachelor_of_medicine | 医学学士 |
| degreeType | bachelor_of_fine_arts | 艺术学学士 |
| degreeType | other | 其他 |
| industry | string | 自由文本，例如“低空经济”。 |
| industryTags | string[] | 可选行业标签，例如 low_altitude_economy。行业标准化后续扩展。 |

#### 6.2.3 AI Provider 与任务枚举

| 枚举 | 值 | 说明 |
|---|---|---|
| providerType | external_cloud | 外部云模型 Provider。默认关闭，学校管理员启用后可用。 |
| providerType | local_private | 本地 / 私有模型配置位。MVP 不要求交付真实本地模型。 |
| taskType | major_profile_generation / obe_generation / curriculum_generation / support_matrix_generation / course_content_generation / quality_analysis / report_generation / document_parse / ability_map_generation | AI 或异步任务类型。 |

#### 6.2.4 能力地图授权 displayScopes

| 枚举值 | 中文展示 |
|---|---|
| basic_info | 基础信息 |
| ability_overview | 能力总览 |
| ability_radar | 能力雷达图 |
| graduation_requirements | 毕业要求达成情况 |
| ability_levels | 核心能力等级 |
| evidence_summary | 证据清单摘要 |
| course_evidence | 课程支撑证据 |
| project_evidence | 项目 / 实验 / 竞赛 / 作品证据 |
| internship_evaluation | 实习与企业评价 |
| portfolio | 作品集 |
| mentor_review | 导师审核意见 |
| job_fit_suggestion | 岗位适配建议 |
| development_suggestion | 后续发展建议 |
| badges | 已审核徽章 |

固定禁止对外展示项：student_profile_detail、growth_warnings、appeal_records、unaudited_evidence、sensitive_blacklist_data、unauthorized_evaluations、ai_internal_analysis、draft_ability_conclusions、revoked_content。

#### 6.2.5 企业证明材料文件类型白名单

| purpose | 允许扩展名 | 说明 |
|---|---|---|
| employer_supporting_material | .pdf / .docx / .jpg / .jpeg / .png | 仅作为人工审核附件；不 OCR，不进入 AI 上下文，不进入课程知识库。 |

#### 6.2.6 报告类型枚举

| reportType | 支持格式 | 说明 |
|---|---|---|
| major_construction_report | docx / pdf | 专业建设报告。 |
| training_plan | docx / pdf | 培养方案。 |
| curriculum_report | docx / pdf | 课程体系报告。 |
| quality_analysis | docx / pdf | 质量分析报告。 |
| curriculum_course_list | xlsx | 课程清单。 |
| support_matrix | xlsx | 课程支撑矩阵。 |
| credit_hour_statistics | xlsx | 学分学时统计。 |
| graduate_ability_map | pdf | 毕业能力地图。 |
| employment_feedback | docx / pdf | 就业反馈与改进报告。 |
---

## 7. 安全、权限与审计要求

### 7.1 接口级安全要求

```text
1. 所有非公开接口必须校验 Bearer Token。
2. 所有业务接口必须校验 school_id / tenant_id 范围。
3. 所有写接口必须校验角色权限和资源权限。
4. AI 任务创建必须继承发起人的权限范围。
5. 文件下载、报告下载、能力地图访问必须进行后端鉴权。
6. 不得返回永久公开文件 URL。
7. 学生画像、成长预警、申诉记录、未审核证据不得出现在对外授权能力地图接口响应中。
8. 企业导师访问学生能力地图必须校验学生授权范围、有效期和撤销状态。
```

### 7.2 必须生成审计日志的接口类别

```text
1. 登录、登录失败、退出；
2. 用户创建、角色范围变更、停用；
3. 学校、学院、专业创建和更新；
4. AI 任务创建、完成、失败、重试；
5. 草案编辑、提交审核、审核通过、驳回、发布；
6. 课程体系版本发布、归档、恢复；
7. 文件上传、扫描失败、下载、预览、归档、恢复；
8. 知识库资料审核、禁用、引用；
9. 学生数据导入、证据上传、证据审核、能力升级；
10. 学生画像申诉、处理、关闭；
11. 成长预警查看和处理；
12. 能力地图生成、审核、授权、撤销、导出、外部访问；
13. 企业邀请、企业审核、企业访问授权数据；
14. 报告导出、下载；
15. 学校级配置变更。
```

---

## 8. 幂等性与并发控制细则

### 8.1 Idempotency-Key 规则

```text
1. Header 名称：Idempotency-Key。
2. 建议格式：客户端生成 UUID。
3. 同一用户、同一学校空间、同一接口、同一 Idempotency-Key 在保留窗口内只执行一次。
4. 若同一 Idempotency-Key 对应请求体不同，返回 IDEMPOTENCY_CONFLICT。
5. 幂等结果保留周期：MVP 建议至少 24 小时；最终保留周期待技术方案确认。
```

### 8.2 If-Match 规则

```text
1. 所有 PATCH / PUT 更新资源接口建议携带 If-Match。
2. If-Match 值对应资源 version。
3. 版本不一致返回 VERSION_CONFLICT。
4. 客户端应重新 GET 最新资源后再提交修改。
```

---

## 9. 已确认接口决策与后续非阻塞事项

### 9.1 已确认接口决策

| 决策编号 | 决策内容 | 影响接口 / 模块 |
|---|---|---|
| API-DEC-001 | courseModule、courseNature、courseType 使用平台默认枚举；接口使用英文枚举，前端展示中文；学校级枚举配置后续扩展。 | 课程库、课程体系、课程地图、报告导出 |
| API-DEC-002 | schoolType、degreeType 使用平台默认枚举；industry 使用自由文本，industryTags 为可选数组；行业标准化后续扩展。 | 学校管理、专业档案、企业档案、岗位模型 |
| API-DEC-003 | 接口不写死具体 AI Provider、模型和参数；使用 providerId / modelId 抽象字段；具体模型由部署配置和学校级配置决定。 | AI 配置、AI 任务、AI 运行记录 |
| API-DEC-004 | MVP 不开放完整 Prompt 模板编辑；只开放学校级 AI 策略和专业级上下文补充；Prompt 模板由系统管理员 / 平台方维护版本。 | AI 策略、专业 AI 上下文、AI 运行追溯 |
| API-DEC-005 | 文件接口采用后端鉴权 + 短期签名 URL 为主；签名 URL 默认 5 分钟有效；敏感场景可走后端代理。 | 文件上传、预览、下载、报告、能力地图 |
| API-DEC-006 | 后端提供平台标准导入模板下载接口，支持 Excel / CSV，模板带 templateVersion；学校级模板后续扩展。 | 学生导入、课程修读记录导入 |
| API-DEC-007 | 审计日志 beforeSummary / afterSummary 只记录字段级脱敏摘要和业务对象引用；敏感原文不进入审计日志。 | 审计日志、学生隐私、AI 追溯 |
| API-DEC-008 | 能力地图授权采用章节级 displayScopes + 固定禁止项；字段级授权后续扩展。 | 能力地图授权、企业访问、PDF 导出 |
| API-DEC-009 | 通知和任务状态采用轮询；提供通知、待办、未读数量和任务状态接口；WebSocket / SSE 后续扩展。 | 通知中心、待办中心、异步任务 |
| API-DEC-010 | 报告生成由后端自动选择当前平台模板版本；生成后保存文件、模板版本、数据版本快照和授权范围快照；历史报告不自动重算。 | 报告中心、能力地图 PDF |
| API-DEC-011 | 企业证明材料允许 PDF、DOCX、JPG、JPEG、PNG；仅作为人工审核附件，不做 OCR、不进入 AI 上下文。 | 企业账号审核、文件服务 |
| API-DEC-012 | 知识库检索采用结构化筛选 + 基础中文关键词匹配 + 明确排序；中文分词、语义检索、向量检索后续扩展。 | 课程知识库、课程内容引用 |

### 9.2 当前阻塞待确认项

```text
无。
```

### 9.3 后续技术方案阶段需细化但不阻塞接口契约的事项

| 编号 | 后续细化项 | 说明 |
|---|---|---|
| API-FT-001 | 实际默认 AI Provider 和模型配置 | 接口以抽象字段表达；具体 Provider、模型、参数在部署或技术方案阶段确定。 |
| API-FT-002 | Prompt 模板内部内容和版本规则 | 本文只定义追溯字段；Prompt 具体内容由平台方维护。 |
| API-FT-003 | 文件安全扫描实现组件 | 接口只约定扫描状态和错误码；具体扫描器在部署方案确定。 |
| API-FT-004 | PDF / Word / Excel 渲染库 | 接口只约定任务和报告元数据；具体渲染库在技术方案确定。 |
| API-FT-005 | 中文全文检索实现 | MVP 为基础关键词匹配；是否启用 PostgreSQL 全文检索、分词或搜索引擎由技术方案确定。 |
| API-FT-006 | 监控、告警和日志平台 | 不影响业务 API 契约。 |
| API-FT-007 | SSO 具体协议 | MVP 默认内置账号；SSO 后续通过 Authentication Provider 扩展。 |

---

## 10. 需求到接口追踪矩阵

| 需求编号 | 覆盖接口 |
|---|---|
| FR-001 | API-AUTH-001～004, API-ORG-001～006, API-USER-001～006, API-ROLE-001 |
| FR-002 | API-AUTH-004, API-FILE-003, API-AUD-001, API-AUTHZ-001～003, API-PUB-001～002 |
| FR-003 | API-ORG-004～006, API-MAJ-001～006 |
| FR-004 | API-DSH-001～003, API-NOT-001 |
| FR-005 | API-DSH-003, API-MAJ-003, API-MAJ-007～008 |
| FR-006 | API-AI-001, API-DRF-001～003, API-REV-001～004 |
| FR-007 | API-OBE-001～003 |
| FR-008 | API-CUR-001～007 |
| FR-009 | API-CUR-008～009 |
| FR-010 | API-SUP-001～003 |
| FR-011 | API-QA-001～002, API-CFG-001～002 |
| FR-012 | API-AI-001～004, API-DRF-001～003, API-REV-001～005, API-CFG-011～012 |
| FR-013 | API-RPT-001～005 |
| FR-014 | API-CRS-001～006, API-CFG-009～010 |
| FR-015 | API-FILE-001～006, API-KB-001～006 |
| FR-016 | API-KB-002～008, API-CC-001～004 |
| FR-017 | API-CC-001～004 |
| FR-018 | API-STU-001～003, API-PRF-001～002 |
| FR-019 | API-APL-001～004 |
| FR-020 | API-PATH-001～003 |
| FR-021 | API-EVD-001～006, API-FILE-001～006 |
| FR-022 | API-EVD-007, API-ABL-001～003 |
| FR-023 | API-CFG-005～006, API-GRW-001～003 |
| FR-024 | API-CFG-007～008, API-WRN-001～002 |
| FR-025 | API-IMP-001～005, API-STU-001～004 |
| FR-026 | API-MAP-001～006 |
| FR-027 | API-AUTHZ-001～003, API-PUB-001～002, API-MAP-006 |
| FR-028 | API-EMP-001～006, API-JOB-001～003, API-EVL-001 |
| FR-029 | API-EVL-001, API-FDB-001～002 |
| FR-030 | API-QA-001～002, API-RPT-001～005, API-AI-003, API-CFG-003～004, API-CFG-011～012 |
| FR-031 | API-CFG-001～012 |
| FR-032 | API-ARC-001～003, API-AUD-001, 各资源归档 / 发布接口 |

---

## 11. 版本结论

本接口文档 v0.2 已完成以下内容：

```text
1. 定义 /api/v1 通用请求、响应、错误码、分页、排序、过滤和幂等规则；
2. 按认证、组织、配置、看板、专业、AI 草案、OBE、课程体系、课程库、知识库、学生、证据、能力地图、企业、报告、归档、审计、通知等领域定义接口；
3. 每个接口均关联需求编号和业务场景；
4. 每个接口均明确方法、URL、权限、请求参数、响应字段、分页/排序/过滤、幂等性和主要错误码；
5. 将 API-DEC-001～API-DEC-012 回填为稳定接口契约；
6. 未设计超出当前 MVP 需求范围的接口；
7. 当前无阻塞待确认项，后续细化项已转入技术方案阶段。
```

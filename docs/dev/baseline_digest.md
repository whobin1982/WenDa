# Wenda 基线摘要 v1.0（baseline_digest）

> 项目：AI 原生本科专业建设与学生能力成长平台  
> 文档目的：把 `doc/baseline/` 下 14 份权威输入文件的核心规则、版本、覆盖结论、首批 P0 切片浓缩到一份机器可读的中文摘要，作为后续自动开发、人工开发、PR 评审、CI 门禁的统一入口。  
> 文档版本：v1.0（与基线 v1.0 对齐）  
> 生成日期：2026-06-07  
> 冲突仲裁：本摘要与任何 `doc/baseline/*.md` 冲突时，**以基线索引 v1.0 第 4 节"文档优先级规则"为准**；`doc/archive/` 任何文件不作为本摘要或自动开发的输入。

---

## 1. 权威输入文件清单（14 份）

| # | 文档 | 版本 | 文件 | 用途 | 优先级 |
|---|---|---|---|---|---|
| 1 | 产品需求说明书 | v0.3 | `product_requirements_spec_v0.3.md` | 需求范围、FR、NFR、非目标、开源治理上游约束 | P0 |
| 2 | 系统架构设计 | v0.3 | `system_architecture_design_v0.3.md` | 架构边界、模块、部署、存储、AI、权限、Adapter 红线 | P0 |
| 3 | 接口文档 | v0.2 | `api_contract_v0.2.md` | API 契约、请求响应、错误码、权限、幂等、分页 | P0 |
| 4 | 详细技术方案 | v0.4 | `detailed_technical_solution_v0.4.md` | 模块实现、数据模型、Adapter、DEV 任务、开源治理 | P0 |
| 5 | 权限判定矩阵 | v1.0 | `permission_decision_matrix_v1.0.md` | 权限判定、PERM→AC 映射、自动化测试依据 | P0 |
| 6 | 任务-接口精确映射表 | v1.0 | `task_api_mapping_matrix_v1.0.md` | DEV-001～DEV-070 ↔ 162 个核心 API 精确映射 | P0 |
| 7 | 验收清单终版 | v1.2 | `acceptance_checklist_final_v1.2.md` | 最终完成标准、AC-RBAC / AC-API / AC-FR / AC-NFR / AC-OSG / AC-FINAL / AC-TECH / AC-E2E | P0 |
| 8 | 需求-验收精确追踪矩阵 | v1.0 | `requirement_acceptance_trace_matrix_exact_v1.0.md` | FR/NFR/ENG/DEV/API/RG ↔ 验收项的精确追踪 | P0 |
| 9 | 需求-接口-任务精确追踪矩阵 | v1.0 | `requirement_api_task_trace_matrix_exact_v1.0.md` | FR/NFR ↔ API ↔ DEV 精确映射 | P0 |
| 10 | DEV-验收精确追踪矩阵 | v1.0 | `dev_acceptance_trace_matrix_exact_v1.0.md` | DEV-001～DEV-070 ↔ AC 精确映射 | P0 |
| 11 | 权限测试精确追踪矩阵 | v1.0 | `permission_test_trace_matrix_exact_v1.0.md` | PERM ↔ AC-RBAC/AC-API ↔ 核心 API 精确追踪 | P0 |
| 12 | 全量错误码字典 | v1.0 | `api_error_code_dictionary_v1.0.md` | 后端异常枚举、前端提示、测试断言统一来源 | P0 |
| 13 | API-错误码精确矩阵 | v1.0 | `api_error_code_matrix_exact_v1.0.md` | 错误码 ↔ API 编号的机器可读精确绑定 | P0 |
| 14 | 开发输入基线索引 | v1.0 | `development_input_baseline_index_v1.0.md` | 权威输入清单、版本规则、冲突优先级、自动开发规则 | P0 |

> **`doc/archive/` 共 9 份**（产品需求说明书_v0.3 旧版、接口文档_v0.2 旧版、系统架构设计_v0.3 旧版、详细技术方案_v0.4 旧版、权限判定矩阵_v1.0 旧版、任务-接口精确映射表_v1.0 旧版、开发输入基线索引_v1.0 旧版、验收清单初版_v0.2、验收清单终版_v1.2 旧版）。仅供人工追溯，**不作为自动开发输入**。

---

## 2. 文档优先级与冲突仲裁

按基线索引 v1.0 第 4 节：

1. 需求范围冲突 → PRD v0.3 为准。
2. 架构边界冲突 → 系统架构设计 v0.3 为准。
3. 接口契约冲突 → 接口文档 v0.2 为准。
4. 权限自然语言描述与权限矩阵冲突 → 权限判定矩阵 v1.0 为准。
5. DEV 任务与 API 归属冲突 → 任务-接口精确映射表 v1.0 为准。
6. 完成标准冲突 → 验收清单终版 v1.2 为准。
7. 自动开发、测试生成、缺陷回溯、验收覆盖统计 → 5 份精确追踪矩阵为准。
8. 技术实现细节冲突 → 详细技术方案 v0.4 为准，但不得突破 PRD、架构、接口、权限和验收约束。

> 其他任何来源（README、CLAUDE.md、对话、archive 旧版）**不得重复定义冲突优先级**，只能引用本节。

---

## 3. MVP 范围与分阶段

### 3.1 MVP 总体原则

- **不追求一次性完成全部功能**；分阶段验证 4 个闭环：
  - **MVP-1**：专业建设与课程体系闭环（本期交付）
  - **MVP-2**：课程知识库与课程内容生成闭环
  - **MVP-3**：学生画像与学习证据闭环
  - **MVP-4**：毕业能力地图与就业反馈闭环
- 平台托管单学校独立租户，每个学校独立 `school_id` / `tenant_id`。
- 跨学校 SaaS 运营后台、专有云迁移按"预留、不做"处理。
- 交付形态：PC Web（MVP-3 / MVP-4 增学生 H5 / 移动端适配）。

### 3.2 MVP-1 P0（本期第一批交付范围）

| # | 能力 | 关联需求 |
|---|---|---|
| 1 | 学校、学院、用户、角色与权限 | FR-001, FR-002 |
| 2 | 多学院、多专业建档 | FR-003 |
| 3 | 简化学校/学院看板 | FR-004 |
| 4 | 专业建设工作台入口 | FR-005 |
| 5 | 内置 OBE 结构模板 | FR-007 |
| 6 | 培养目标 / 毕业要求 / 能力指标点生成与编辑 | FR-006, FR-007 |
| 7 | 课程库基础能力 | FR-014 |
| 8 | 课程体系生成与编辑 | FR-008 |
| 9 | 课程支撑矩阵 | FR-010 |
| 10 | 四年八学期课程地图 | FR-009 |
| 11 | 质量规则配置与分析 | FR-011 |
| 12 | AI 草案区与人工审核发布 | FR-012 |
| 13 | 专业建设报告、培养方案、课程体系报告导出 | FR-013, FR-030 |

### 3.3 MVP-1 明确不做

- 多专业强制完整闭环、深度专业群分析、复杂多级审批流。
- 学校自定义报告模板、学生端完整功能。
- 完整教务、成绩、排课、选课、LMS。
- 对外发布教育内容由系统管理员直接绕过审核。

### 3.4 通用非目标

OOS-001～OOS-015：完整教务系统、完整排课、完整 LMS、完整成绩、完整考试、完整招聘、跨校 SaaS、原生 App / 复杂小程序、开放互联网搜索采集、未授权对外展示学生数据、未审核资料用于正式发布、深度专业群分析、复杂审批流、学校自定义报告模板。

---

## 4. 技术栈基线（架构 v0.3 §4.2 + 技术方案 v0.4 §2.1 / §4）

| 层级 | 选型 | 许可证 | 优先级 | 备注 |
|---|---|---|---|---|
| 前端框架 | Vue 3 + TypeScript | MIT / Apache-2.0 | 锁定 | Pinia、Vue Router、Axios、Vite |
| UI 组件库 | Element Plus | MIT | 锁定 | Ant Design Vue 作为后续候选 |
| 后端 | Java 21 + Spring Boot 3.3.x | OpenJDK 发行版 / Apache-2.0 | 锁定 | Spring Security、JPA + MyBatis 并存，复杂 SQL 优先 MyBatis |
| ORM / SQL | Spring Data JPA + MyBatis | Apache-2.0 | 锁定 | jOOQ 作为商业双许可候选，**MVP 不引入** |
| 主数据库 | PostgreSQL 16 | PostgreSQL License | 锁定 | 所有核心表必须包含 `school_id` / `tenant_id` |
| 数据库迁移 | Flyway Community | Apache-2.0 | 锁定 | Liquibase 候选 |
| 对象存储 | S3 兼容 Adapter | 抽象层 | 锁定 | 平台托管可选云 OSS；私有化可接 MinIO / Ceph |
| 异步任务 | PostgreSQL 任务表 + Java Worker | 自研 + PG | 锁定 | 后续队列优先 RabbitMQ / Kafka / Valkey |
| AI | AIProviderAdapter | 抽象层 | 锁定 | 默认外部 Provider + 本地 / 私有 Provider 配置位；**外部 Provider 默认关闭** |
| 知识库检索 | PostgreSQL 关键词检索 | PG | 锁定 | 后续候选 OpenSearch / pgvector / Milvus / Qdrant |
| 文档解析 | Apache Tika / POI / docx4j | Apache-2.0 | 候选 | MVP 文档解析在 MVP-2 启动 |
| 文件安全扫描 | FileSecurityScannerAdapter | 抽象层 | 锁定 | ClamAV 独立服务 / 云扫描 / 学校组件候选；**生产禁用 Mock** |
| 报告渲染 | ReportRendererAdapter | 抽象层 | 锁定 | 优先 Apache POI / docx4j / PDFBox / OpenPDF / LibreOffice Headless；**iText 默认不采用** |
| 审计 | PostgreSQL 追加式审计表 | PG | 锁定 | 物理删除禁止 |
| 监控 | Spring Boot Actuator / Micrometer | Apache-2.0 | 锁定 | Prometheus / OpenTelemetry 后续候选；Grafana AGPL 需确认 |
| 邮件 | SMTP（可选） | 协议 | 候选 | EmailNotificationAdapter；未配置 SMTP 时企业邀请可复制链接 |
| 认证 | LocalAuthProvider | 自研 | MVP | CAS / OAuth2 / SAML / LDAP 后续 |
| 任务队列 | PostgreSQL 任务表 | 自研 | MVP | 后续 RabbitMQ / Kafka / Valkey |

---

## 5. 总体架构与部署

- **架构风格**：模块化单体（不是微服务）。MVP 阶段以代码层模块化 + 数据库层清晰领域边界 + 任务层异步解耦 + Adapter 层隔离第三方依赖。
- **24 个领域模块**：M-01 认证组织租户权限 / M-02 学校配置 / M-03 多专业 / M-04 看板 / M-05 专业工作台 / M-06 AI 专业画像 OBE / M-07 课程体系 / M-08 课程库 / M-09 知识库 / M-10 课程内容 / M-11 AI 编排 / M-12 审核发布版本 / M-13 数据导入 / M-14 学生画像培养路径 / M-15 学习证据能力升级 / M-16 成长预警 / M-17 毕业能力地图 / M-18 用人单位就业反馈 / M-19 报告中心 / M-20 文件服务安全扫描 / M-21 异步任务 / M-22 通知待办 / M-23 审计合规 / M-24 可观测性。
- **部署形态**：平台托管单学校独立租户；每个学校独立 `school_id` / `tenant_id`；多学校 SaaS 运营后台不做；专有云 / 私有化预留。
- **部署隔离规则**：核心表带 `school_id` / `tenant_id`；对象存储按租户分 key；AI Provider 按学校生效；质量阈值、能力等级、预警规则、文件阈值按学校配置；审计日志必带 `school_id` / `tenant_id`；文件下载 / 报告导出 / AI 任务 / 企业访问不得越界。

---

## 6. 核心 API 覆盖结论

| 检查项 | 数量 |
|---|---|
| 核心 API（已排除 `API-DEC-*` 决策项与 `API-FT-*` 后续技术细化项） | **162** |
| 已映射到 DEV 任务的 API | **162 / 162（100%）** |
| 涉及 DEV 任务编号 | DEV-001～DEV-070 |
| MVP-1 阶段首批落地的 API（本期 PR） | **30**（见 §9） |
| 后续 PR 拆分 | 132 API，按业务闭环拆分 |

> 不允许用「相关接口」「全部接口」「等」作为自动开发任务边界。

---

## 7. 角色与权限矩阵（10 角色）

| 角色代码 | 名称 | 权限定位 |
|---|---|---|
| `SYSTEM_ADMIN` | 系统管理员 | 平台初始化、跨学校运维、系统配置、开源治理；**无教育内容发布权**、**无学生隐私查看权** |
| `SCHOOL_ADMIN` | 学校管理员 | 学校空间内用户 / 学院 / 配置 / AI 策略 / 审计 / 发布准入 |
| `COLLEGE_MANAGER` | 学院管理者 | 学院范围内专业管理与报告 |
| `MAJOR_OWNER` | 专业负责人 | 本专业建设 / OBE / 课程体系 / 支撑矩阵 / 质量分析 / 正式版本发布 |
| `ACADEMIC_ADMIN` | 教务人员 | 培养方案 / 课程计划辅助维护与导出（**P1**） |
| `TEACHER` | 任课教师 | 所授课程、课程内容、课程知识库、关联学生证据审核 |
| `MENTOR` | 导师 / 班主任 | 所指导学生画像、成长建议、预警、能力地图审核意见 |
| `STUDENT` | 学生 | 本人画像、学习证据、成长任务、能力地图、对外授权 |
| `EMPLOYER_MENTOR` | 企业导师 / 用人单位 | 邀请制、授权企业范围内岗位能力、实习评价、就业反馈、授权能力地图访问 |
| `KNOWLEDGE_ADMIN` | 知识库管理员 | 课程知识库资料审核、标注、可信度、版权许可、禁用 |

**权限规则数量**：`PERM-SYS-001/002`、`PERM-SCHOOL-001/002/003`、`PERM-COLLEGE-001/002`、`PERM-MAJOR-001/002/003`、`PERM-ACAD-001`、`PERM-TEACHER-001/002/003`、`PERM-MENTOR-001/002/003`、`PERM-STUDENT-001/002/003`、`PERM-EMP-001/002/003`、`PERM-KB-001/002`、`PERM-AI-001`、`PERM-FILE-001`、`PERM-RPT-001`、`PERM-ARC-001`、`PERM-NOT-001`、`PERM-OSG-001` = **26 条 P0 规则 + 1 条 P1（PERM-NOT-001 / PERM-ACAD-001）**。

每条 P0 规则必须至少 1 条正向 + 1 条反向自动化测试，覆盖 IDOR 场景。

---

## 8. 错误码字典使用规则

- 字典来源：`api_error_code_dictionary_v1.0.md` + `api_error_code_matrix_exact_v1.0.md`，**后端不得新增未登记错误码**。
- 全部 39 个错误码（HTTP 状态）：`OK 200 / CREATED 201 / ACCEPTED 202 / BAD_REQUEST 400 / VALIDATION_ERROR 400 / UNAUTHORIZED 401 / TOKEN_EXPIRED 401 / FORBIDDEN 403 / ACCESS_DENIED 403 / SCOPE_FORBIDDEN 403 / NOT_FOUND 404 / CONFLICT 409 / VERSION_CONFLICT 409 / IDEMPOTENCY_CONFLICT 409 / BUSINESS_STATE_INVALID 422 / SCHOOL_SCOPE_REQUIRED 422 / FILE_TOO_LARGE 413 / UNSUPPORTED_FILE_TYPE 415 / FILE_TYPE_NOT_ALLOWED 415 / FILE_SECURITY_SCAN_PENDING 423 / FILE_SECURITY_SCAN_FAILED 422 / FILE_ACCESS_DENIED 403 / AI_PROVIDER_DISABLED 422 / AI_OUTPUT_SCHEMA_INVALID 422 / AI_TASK_FAILED 422 / TASK_NOT_FOUND 404 / TASK_ALREADY_RUNNING 409 / TASK_TIMEOUT 422 / REPORT_TEMPLATE_NOT_FOUND 404 / REPORT_FILE_NOT_READY 423 / KNOWLEDGE_SOURCE_DISABLED 422 / KNOWLEDGE_SEARCH_UNSUPPORTED_FILTER 400 / AUTHORIZATION_EXPIRED 403 / AUTHORIZATION_REVOKED 403 / DISPLAY_SCOPE_FORBIDDEN 403 / IMPORT_TEMPLATE_INVALID 400 / IMPORT_CONFLICT_REQUIRES_DECISION 409 / RATE_LIMITED 429 / INTERNAL_ERROR 500 / DEPENDENCY_UNAVAILABLE 503`。
- 后端 `BusinessException` / `ErrorCode` 枚举必须**与字典一一对应**；`details` 字段结构按字典表第 5 列定义。
- 前端 `request` 封装按 `code` 提示用户 + 触发埋点；测试用例断言 `code` 字符串，不允许断言 `message`。
- 新增错误码必须同步：`api_error_code_dictionary_v1.0.md` + `api_error_code_matrix_exact_v1.0.md` + 后端枚举 + 前端 i18n。

---

## 9. 首批 P0 切片（本期 PR 范围）

按 gstack 流程的"小步、可验证、可 PR、可回滚"原则，本期 PR（feature 分支 `feature/phase-1-p0-foundation`）落地 30 个核心 API + 1 套错误码字典 + 1 个权限基础框架 + 1 个审计基础框架：

| 模块 | DEV 任务 | API 编号 | 接口名 | 关联验收项 |
|---|---|---|---|---|
| M-01 认证 | DEV-003 | API-AUTH-001 | 本地账号登录 | AC-API-AUTH-001 |
| M-01 认证 | DEV-003 | API-AUTH-002 | 退出登录 | AC-API-AUTH-002 |
| M-01 认证 | DEV-003 | API-AUTH-003 | 刷新 Token | AC-API-AUTH-003 |
| M-01 认证 | DEV-003 | API-AUTH-004 | 当前用户信息 | AC-API-AUTH-004 |
| M-01 组织 | DEV-004 | API-ORG-001 | 创建学校空间 | AC-API-ORG-001 |
| M-01 组织 | DEV-004 | API-ORG-002 | 获取当前学校信息 | AC-API-ORG-002 |
| M-01 组织 | DEV-004 | API-ORG-003 | 更新学校信息 | AC-API-ORG-003 |
| M-01 组织 | DEV-004 | API-ORG-004 | 创建学院 | AC-API-ORG-004 |
| M-01 组织 | DEV-004 | API-ORG-005 | 学院列表 | AC-API-ORG-005 |
| M-01 组织 | DEV-004 | API-ORG-006 | 更新学院 | AC-API-ORG-006 |
| M-01 用户角色 | DEV-005 | API-USER-001 | 创建用户 | AC-API-USER-001 |
| M-01 用户角色 | DEV-005 | API-USER-002 | 用户列表 | AC-API-USER-002 |
| M-01 用户角色 | DEV-005 | API-USER-003 | 更新用户基础信息 | AC-API-USER-003 |
| M-01 用户角色 | DEV-005 | API-USER-004 | 绑定用户角色与范围 | AC-API-USER-004 |
| M-01 用户角色 | DEV-005 | API-USER-005 | 停用用户 | AC-API-USER-005 |
| M-01 用户角色 | DEV-005 | API-USER-006 | 管理员重置本地密码 | AC-API-USER-006 |
| M-01 用户角色 | DEV-005 | API-ROLE-001 | 角色与权限元数据 | AC-API-ROLE-001 |
| M-02 学校配置 | DEV-007 | API-CFG-001 | 获取课程质量阈值 | AC-API-CFG-001 |
| M-02 学校配置 | DEV-007 | API-CFG-002 | 更新课程质量阈值 | AC-API-CFG-002 |
| M-02 学校配置 | DEV-007 | API-CFG-009 | 获取临时代码策略 | AC-API-CFG-009 |
| M-02 学校配置 | DEV-007 | API-CFG-010 | 更新临时代码策略 | AC-API-CFG-010 |
| M-02 学校配置 | DEV-008 | API-CFG-003 | 获取 AI 配置 | AC-API-CFG-003 |
| M-02 学校配置 | DEV-008 | API-CFG-004 | 更新 AI 配置 | AC-API-CFG-004 |
| M-02 学校配置 | DEV-008 | API-CFG-005 | 获取能力等级配置 | AC-API-CFG-005 |
| M-02 学校配置 | DEV-008 | API-CFG-006 | 更新能力等级配置 | AC-API-CFG-006 |
| M-02 学校配置 | DEV-008 | API-CFG-007 | 获取成长预警配置 | AC-API-CFG-007 |
| M-02 学校配置 | DEV-008 | API-CFG-008 | 更新成长预警配置 | AC-API-CFG-008 |
| M-02 学校配置 | DEV-008 | API-CFG-011 | 获取学校级 AI 策略 | AC-API-CFG-011 |
| M-02 学校配置 | DEV-008 | API-CFG-012 | 更新学校级 AI 策略 | AC-API-CFG-012 |
| M-04 看板 | DEV-009 | API-DSH-001/002/003 | 学校 / 学院 / 专业级看板 | AC-API-DSH-001/002/003 |
| M-23 审计 | DEV-025 | API-AUD-001 | 审计日志查询 | AC-API-AUD-001 |

> 备注：API-DSH-001/002/003 在本期 PR 提供**只读骨架**（聚合 + 字段定义），复杂指标计算留到对应业务模块就绪后接入。**完整闭环比对**留到后续 PR。

### 9.1 横向工程能力

- 后端：Spring Boot 3.3.x 工程骨架、Flyway 迁移目录、统一响应包装 `ApiResponse<T>`、`BusinessException` + `ErrorCode` 枚举、`Idempotency-Key` 拦截器、`If-Match` / 版本字段校验拦截器、`X-Request-Id` 生成与透传、JWT 鉴权 + Spring Security、`PermissionService` 抽象（实现 `PERM-SCHOOL-001/002/003`、`PERM-SYS-001/002` 的最小可执行版本），`AuditLogService` 追加式审计（覆盖写操作），`RequestContext` 持有 `schoolId` / `userId` / `roles`。
- 前端：Vue 3 + Vite + TS + Pinia + Vue Router + Element Plus + Axios 工程骨架，统一 `request` 封装（`requestId` 自动注入 + 错误码字典消费 + `Idempotency-Key` 自动注入 + 401/403 路由处理），RBAC 前端菜单 hook `usePermission`，登录页 + 学校切换 + 空状态。
- 数据库：Flyway V1__init_*.sql 包含首批必需表：`schools / colleges / users / user_credentials / user_sessions / roles / user_role_scopes / school_quality_rules / course_code_policy / school_ai_settings / school_ability_level_settings / growth_warning_rules / idempotency_keys / audit_logs / dashboards_cache / schema_versions`，全部带 `school_id` / `tenant_id`。
- CI：`.github/workflows/backend-ci.yml`（JDK 21 + Maven + Postgres service + Flyway + JUnit + 生产 Mock 禁用断言 + SBOM 预留），`.github/workflows/frontend-ci.yml`（Node 20 + pnpm + tsc + vitest + build），`.github/workflows/osg-gate.yml`（依赖许可证扫描 + 漏洞扫描 + Mock 禁用 + SBOM 预留）。
- 文档：`docs/dev/baseline_digest.md`（本文件）、`docs/dev/phase1_p0_summary.md`（PR 摘要）、`docs/dev/github_issues_milestones.md`（可复制的 Issue / Milestone 内容）。
- 配置文件：`.env.example`（根）、`backend/.env.example`、`frontend/.env.example`，`application-dev.yml / application-test.yml / application-prod.yml` 三 profile，prod 显式禁用 Mock Adapter。
- 范围：未引入的 132 个核心 API 按业务闭环拆分到后续 PR；专业建设 / OBE / 课程体系 / 课程库 / 知识库 / 课程内容 / 审核发布 / 学生成长 / 能力地图 / 用人单位 / 报告中心 / 数据导入 / 文件 / 异步任务 / 通知待办等模块在后续 PR 推进。

---

## 10. 验收门禁（每 PR 必须通过）

| 门禁 | 来源 | 检查项 |
|---|---|---|
| 编译 | CI | 后端 `mvn -B verify` 通过；前端 `pnpm build` 通过；`tsc --noEmit` 通过 |
| 单元 / 接口测试 | CI | JUnit 5 + Spring Boot Test；Vitest + Vue Test Utils；覆盖率 ≥ 60% 行覆盖 |
| 数据库迁移 | CI | Flyway 迁移 + Testcontainers Postgres 启动测试 |
| 权限正反向 | CI | P0 规则至少 1 正 1 反；IDOR 越权测试覆盖 |
| 错误码断言 | CI | 所有失败响应 `code` 命中错误码字典 |
| 幂等键 | CI | 创建 / 任务 / 报告 / 授权接口必须支持 `Idempotency-Key` |
| 版本控制 | CI | PATCH 接口必须支持 `If-Match` |
| Mock 禁用 | CI | `prod` profile 启动断言：MockAIProvider / MockFileSecurityScanner / MockStorageAdapter / SimpleReportRenderer **未启用** |
| 适配层边界 | CI | ArchUnit 断言：业务模块不得直接依赖厂商 SDK |
| 开源治理 | CI | `dependency-license-check` + `osv-scanner` + `syft` SBOM 预留；高风险许可证阻断或审批 |
| 审计 | CI | 写操作必产生 `audit_logs` 行 |
| 响应格式 | CI | 全部接口响应必须符合 `ApiResponse<T>` 结构 |
| 文档同步 | PR | 改动 API 必同步接口文档 + 追踪矩阵 + baseline_digest |
| PR 评审 | GitHub | 至少 1 名维护者 Approve，CI 全绿 |

---

## 11. 开源治理硬性要求（ENG-OSG-001 / NFR-016 / RG-OSG-001～RG-OSG-010）

1. **开源优先**：优先 MIT / Apache-2.0 / BSD / PostgreSQL License 等 OS-1 级别。
2. **许可证分级**：
   - **OS-1** 低风险（MIT / Apache-2.0 / BSD / PG）— 可默认采用，纳入 SBOM。
   - **OS-2** 中风险（MPL / LGPL / EPL）— 需记录使用方式与动态链接边界。
   - **OS-3** 高风险（GPL / AGPL / SSPL / 强 Copyleft）— 负责人 / 法务审批后才能进生产。
   - **CS-1** 商业 SDK — 必说明开源替代、成本、风险、替换路径并审批。
   - **CS-2** 闭源 SaaS — 必审批、默认关闭、可配置。
   - **PC-1** 专有云 — 可用于平台托管，但必须保留抽象层与迁移路径。
3. **Adapter 红线**：业务模块**不得**直接调用闭源厂商 SDK，必须走 `AIProviderAdapter / FileStorageAdapter / FileSecurityScannerAdapter / ReportRendererAdapter / KnowledgeSearchAdapter / EmailNotificationAdapter / TaskQueueAdapter / AuthenticationProvider`。
4. **生产禁用项**：`MockAIProvider / MockFileSecurityScanner / MockStorageAdapter / SimpleReportRenderer / 未审批的商业 SDK / 未审批的 GPL/AGPL/SSPL 组件 / 未加密的 Provider Key / 永久公开文件 URL`。
5. **生产发布门禁**：SBOM、依赖清单、许可证扫描、高风险许可证审批、漏洞扫描、容器镜像扫描、Mock 禁用、Adapter 边界、外部 AI Provider 审批、文件安全扫描。
6. **MVP 推荐工具**：Syft 生成 SBOM；Grype / OSV-Scanner / Trivy 漏洞与镜像扫描；ScanCode Toolkit / Maven license plugin / pnpm license 工具做许可证扫描；OWASP Dependency-Check 作为兜底。

---

## 12. 权限与安全硬性要求（10 条）

1. 后端鉴权是安全边界；前端菜单控制只用于体验优化。
2. P0 权限规则必须有正反向测试；反向测试必须覆盖 IDOR 场景（修改 path/query 中的 `schoolId` / `collegeId` / `majorId` / `studentId` / `employerId` / `abilityMapId`）。
3. 学生画像 / 学习证据 / 能力地图 / 预警 / 授权访问严格保护。
4. 企业只能访问学生授权的能力地图范围。
5. 撤销授权或授权过期后不得继续访问（返回 `AUTHORIZATION_EXPIRED` / `AUTHORIZATION_REVOKED`）。
6. 系统管理员不得直接发布教育内容、不得绕过授权查看学生隐私。
7. 学校管理员不得跨学校访问。
8. 学院管理者不得跨学院访问。
9. 教师 / 导师 / 学生 / 企业导师必须按授课关系、导师关系、本人关系、企业授权关系进行 ABAC 判定。
10. 权限失败返回 `FORBIDDEN` 或对应业务错误码，**不得返回敏感资源存在性细节**；越权访问必须记录安全审计 / 安全事件日志。

---

## 13. API 实现硬性要求

- Base Path：`/api/v1`；Content-Type `application/json; charset=utf-8`；鉴权 `Bearer Token`。
- 通用请求头：`Authorization` / `X-Request-Id` / `Idempotency-Key` / `If-Match` / `Accept-Language`。
- 通用响应：`{ success, code, message, data, requestId, timestamp }`。
- 分页响应：`{ items, page, pageSize, total, totalPages, sort, order }`。
- 写操作记录审计日志（追加式 `audit_logs` 表）。
- 创建类 / 任务类 / 导入提交 / 报告导出 / 授权创建 / 企业邀请必须支持 `Idempotency-Key`。
- PATCH 更新使用 `If-Match: <version>` 或服务内版本字段；版本不一致返回 `VERSION_CONFLICT`。
- 禁止物理删除关键业务数据，删除类操作统一建模为归档、撤回、停用或作废。
- 所有错误码必须来自全量错误码字典（§8）；不得新增未登记错误码。
- 所有核心 API 必须能追溯到 DEV 任务和验收项（通过 `task_api_mapping_matrix_v1.0.md` + `requirement_api_task_trace_matrix_exact_v1.0.md` + `dev_acceptance_trace_matrix_exact_v1.0.md` 三表交叉追溯）。

---

## 14. Blocker 检查

| 检查项 | 结果 |
|---|---|
| 缺少基线索引 | ❌ 无（`development_input_baseline_index_v1.0.md` 存在） |
| 缺少精确追踪矩阵 | ❌ 无（5 份精确追踪矩阵齐备） |
| 缺少权限矩阵 | ❌ 无（`permission_decision_matrix_v1.0.md` + `permission_test_trace_matrix_exact_v1.0.md` 齐备） |
| 缺少任务-接口映射表 | ❌ 无（`task_api_mapping_matrix_v1.0.md` 162 / 162 覆盖） |
| 缺少验收终版 | ❌ 无（`acceptance_checklist_final_v1.2.md` 存在） |
| 版本号冲突 | ❌ 无（基线索引第 5 节 CONS-DEC-001 已统一 PRD v0.3 / 架构 v0.3 / 技术方案 v0.4 / 验收 v1.2） |
| 无法确定技术栈 | ❌ 无（架构 v0.3 §4.2 + 技术方案 v0.4 §2.1 已锁定） |
| 缺少错误码字典 | ❌ 无（`api_error_code_dictionary_v1.0.md` 39 条齐备） |

**结论：无 Blocker，可进入 Phase 1 / Phase 2。**

---

## 15. 后续维护规则

1. 新增 API 必须同步：接口文档 v0.2 + 任务-接口映射表 + 需求-接口-任务追踪矩阵 + 需求-验收追踪矩阵 + DEV-验收追踪矩阵 + 错误码字典与精确矩阵 + 本摘要的 §9 切片。
2. 修订任何权威输入文件必须在 commit message 中标注 `[baseline-bump]`，并在 PR 描述中显式说明对 API、权限、验收的影响。
3. 任何对 `doc/archive/` 文件的引用必须**在 PR 描述中显式声明仅用于人工追溯**，不作为自动开发输入。
4. 任何对开源组件的升级、新增、替换必须按 §11 的许可证分级走准入流程。
5. CI 中的发布门禁不得被 `--no-verify` 绕过。

---

> 本摘要由 Claude Opus 4.8 自动生成于 2026-06-07；与基线索引 v1.0 一致性核查通过。

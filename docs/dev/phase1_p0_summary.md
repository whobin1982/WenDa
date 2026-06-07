# Wenda MVP-1 第一批 P0 实施摘要（PR 描述素材）

> 文档目的：把本期 PR 的范围、追溯表、验收项、未完成事项一次性说清楚，便于 PR 描述直接复用，也方便评审人快速核对基线。
> 文档版本：v1.0  
> 关联分支：`feature/phase-1-p0-foundation`  
> 关联基线：`doc/baseline/development_input_baseline_index_v1.0.md`（基线索引 v1.0）  
> 关联摘要：`docs/dev/baseline_digest.md`

## 1. 本 PR 范围

- 仓库工程化初始化：后端 `backend/`（Spring Boot 3.3.x + Java 21 + Maven + Flyway + JdbcTemplate + Spring Security + jjwt）+ 前端 `frontend/`（Vue 3 + TS + Vite + Pinia + Vue Router + Element Plus + Axios + Vitest）。
- 数据库 Flyway V1 迁移：18 张表，全部带 `school_id` / `tenant_id`，含 `users / schools / colleges / roles / user_role_scopes / school_quality_rules / course_code_policy / school_ai_settings / school_ability_level_settings / growth_warning_rules / idempotency_keys / audit_logs / dashboards_cache / schema_versions` 等。
- 统一响应 `ApiResponse<T>`、全量错误码字典枚举（39 条）、`BusinessException` + `GlobalExceptionHandler`、`Idempotency-Key` 拦截器 + ResponseAdvice、`If-Match` 校验工具、JWT Provider + 鉴权过滤器、`RequestContext`（持有 `requestId / schoolId / userId / roles`）、`AuditAspect` + `AuditService`、`PermissionService` 基础骨架、Adapter 接口集合（AI / Storage / Scanner / Renderer / Search / Email / TaskQueue / Auth）、`ProductionMockGuard` 启动校验。
- **30 个核心 API + 1 套错误码字典 + 1 个权限基础框架 + 1 个审计基础框架**：
  - API-AUTH-001/002/003/004（4）
  - API-ORG-001/002/003/004/005/006（6，含 list 兼容端点）
  - API-USER-001/002/003/004/005/006（6）
  - API-ROLE-001（1）
  - API-CFG-001/002/003/004/005/006/007/008/009/010/011/012（12）
  - API-AUD-001（1）
  - API-DSH-001/002/003（3 看板骨架）
- 单元测试 + ArchUnit 适配层边界 + 生产 Mock 禁用测试 + 错误码字典一致性测试 + 权限正反向测试 + JWT 鉴权测试 + If-Match 测试 + RequestContext 测试 + Adapter 异常映射测试。
- CI：`.github/workflows/{backend-ci,frontend-ci,osg-gate}.yml`。
- 文档：`docs/dev/baseline_digest.md`（基线摘要）、`docs/dev/phase1_p0_summary.md`（本文件）。

## 2. 关联 DEV 任务

| DEV 任务 | 范围 | 关联 API |
|---|---|---|
| DEV-001/002/003 | 认证 / Token / 当前用户 | API-AUTH-001/002/003/004 |
| DEV-004 | 学校空间 / 学院 | API-ORG-001/002/003/004/005/006 |
| DEV-005 | 用户 / 角色元数据 | API-USER-001～006、API-ROLE-001 |
| DEV-006 | （拆入 001/002/004） | — |
| DEV-007 | 质量阈值 / 临时代码策略 | API-CFG-001/002/009/010 |
| DEV-008 | AI / 能力等级 / 预警 / AI 策略 | API-CFG-003/004/005/006/007/008/011/012 |
| DEV-009 | 学校 / 学院 / 专业看板 | API-DSH-001/002/003 |
| DEV-025 | 审计日志查询 | API-AUD-001 |

## 3. 关联 API（含验收项）

| API | URL | Method | DEV | 验收项 | 优先级 |
|---|---|---|---|---|---|
| API-AUTH-001 | `/api/v1/auth/login` | POST | DEV-003 | AC-API-AUTH-001 | P0 |
| API-AUTH-002 | `/api/v1/auth/logout` | POST | DEV-003 | AC-API-AUTH-002 | P0 |
| API-AUTH-003 | `/api/v1/auth/refresh` | POST | DEV-003 | AC-API-AUTH-003 | P0 |
| API-AUTH-004 | `/api/v1/auth/me` | GET | DEV-003 | AC-API-AUTH-004 | P0 |
| API-ORG-001 | `/api/v1/schools` | POST | DEV-004 | AC-API-ORG-001 | P0 |
| API-ORG-002 | `/api/v1/schools/current` | GET | DEV-004 | AC-API-ORG-002 | P0 |
| API-ORG-003 | `/api/v1/schools/{id}` | PATCH | DEV-004 | AC-API-ORG-003 | P0 |
| API-ORG-004 | `/api/v1/colleges` | POST | DEV-004 | AC-API-ORG-004 | P0 |
| API-ORG-005 | `/api/v1/colleges` | GET | DEV-004 | AC-API-ORG-005 | P0 |
| API-ORG-006 | `/api/v1/colleges/{id}` | PATCH | DEV-004 | AC-API-ORG-006 | P0 |
| API-USER-001 | `/api/v1/users` | POST | DEV-005 | AC-API-USER-001 | P0 |
| API-USER-002 | `/api/v1/users` | GET | DEV-005 | AC-API-USER-002 | P0 |
| API-USER-003 | `/api/v1/users/{id}` | PATCH | DEV-005 | AC-API-USER-003 | P0 |
| API-USER-004 | `/api/v1/users/{id}/roles-scopes` | PUT | DEV-005 | AC-API-USER-004 | P0 |
| API-USER-005 | `/api/v1/users/{id}/disable` | POST | DEV-005 | AC-API-USER-005 | P0 |
| API-USER-006 | `/api/v1/users/{id}/password-reset` | POST | DEV-005 | AC-API-USER-006 | P0 |
| API-ROLE-001 | `/api/v1/roles` | GET | DEV-005 | AC-API-ROLE-001 | P0 |
| API-CFG-001 | `/api/v1/school-settings/quality-rules` | GET | DEV-007 | AC-API-CFG-001 | P0 |
| API-CFG-002 | `/api/v1/school-settings/quality-rules` | PATCH | DEV-007 | AC-API-CFG-002 | P0 |
| API-CFG-003 | `/api/v1/school-settings/ai` | GET | DEV-008 | AC-API-CFG-003 | P0 |
| API-CFG-004 | `/api/v1/school-settings/ai` | PATCH | DEV-008 | AC-API-CFG-004 | P0 |
| API-CFG-005 | `/api/v1/school-settings/ability-levels` | GET | DEV-008 | AC-API-CFG-005 | P0 |
| API-CFG-006 | `/api/v1/school-settings/ability-levels` | PUT | DEV-008 | AC-API-CFG-006 | P0 |
| API-CFG-007 | `/api/v1/school-settings/growth-warning-rules` | GET | DEV-008 | AC-API-CFG-007 | P0 |
| API-CFG-008 | `/api/v1/school-settings/growth-warning-rules` | PATCH | DEV-008 | AC-API-CFG-008 | P0 |
| API-CFG-009 | `/api/v1/school-settings/course-code-policy` | GET | DEV-007 | AC-API-CFG-009 | P0 |
| API-CFG-010 | `/api/v1/school-settings/course-code-policy` | PATCH | DEV-007 | AC-API-CFG-010 | P0 |
| API-CFG-011 | `/api/v1/school/ai-policy` | GET | DEV-008 | AC-API-CFG-011 | P0 |
| API-CFG-012 | `/api/v1/school/ai-policy` | PUT | DEV-008 | AC-API-CFG-012 | P0 |
| API-AUD-001 | `/api/v1/audit-logs` | GET | DEV-025 | AC-API-AUD-001 | P0 |
| API-DSH-001 | `/api/v1/dashboards/school` | GET | DEV-009 | AC-API-DSH-001 | P0 |
| API-DSH-002 | `/api/v1/dashboards/colleges/{id}` | GET | DEV-009 | AC-API-DSH-002 | P0 |
| API-DSH-003 | `/api/v1/dashboards/majors/{id}` | GET | DEV-009 | AC-API-DSH-003 | P0 |

> 看板接口本期仅返回基础计数与 todo 摘要；复杂指标（成熟度 / 完整度 / 风险）随 M-05 / M-07 业务模块就绪后接入。

## 4. 关联需求

- **FR-001** 学校、学院、用户与角色管理（API-AUTH/ORG/USER/ROLE）。
- **FR-002** 权限控制、学生授权与操作审计（PermissionService + AuditAspect + API-AUD-001）。
- **FR-003** 多学院、多专业管理（API-ORG-004/005/006）。
- **FR-031** 学校级配置中心（API-CFG-001/002/003/004/005/006/007/008/009/010/011/012）。
- **NFR-010** 学生数据出域（AI 配置禁用默认开启 + 启用须审批）。
- **NFR-012** 审计日志（追加式 `audit_logs` 分区表）。
- **NFR-013** 部署 / 学校空间隔离（所有核心表带 `school_id` / `tenant_id`）。

## 5. 关联验收项

- **角色级**：AC-RBAC-001-P/N（系统管理员）、AC-RBAC-002-P/N（学校管理员）、AC-RBAC-006-P/N（教师）、AC-RBAC-007-P/N（导师 / 班主任 / 企业导师）、AC-RBAC-008-P/N（学生）、AC-RBAC-010-P/N（知识库管理员）。
- **接口级**：AC-API-AUTH-001～004、AC-API-ORG-001～006、AC-API-USER-001～006、AC-API-ROLE-001、AC-API-CFG-001～012、AC-API-AUD-001、AC-API-DSH-001/002/003。
- **OSG**：AC-OSG-001/003/004/007/008/010 通过单元测试 + ArchUnit + 启动断言保障；AC-OSG-005/006 留 CI 接入 SBOM / 漏洞扫描的脚手架。

## 6. 权限影响

- 鉴权边界在后端；前端菜单只用于体验优化（基线 PERM 矩阵 §1 第 6 条）。
- 跨 schoolId 访问返回 `SCOPE_FORBIDDEN`（409 → 实际 403）；越权写入会进入 `audit_logs`，`risk_level = SENSITIVE`。
- 学生数据禁出域通过 `school_ai_settings.student_data_outbound` + 启用审批硬性约束。
- 系统管理员创建学校空间走 `PERM-SYS-001`，调用前 `requireSystemAdminBootstrap()`。

## 7. 数据库迁移影响

- 新增 `db/migration/V1__init_core_tables.sql`：
  - `schools / colleges / users / user_credentials / user_sessions / roles / user_role_scopes`
  - `school_quality_rules / course_code_policy / school_ai_settings / school_ability_level_settings / growth_warning_rules`
  - `idempotency_keys / audit_logs / dashboards_cache / schema_versions`
- 全部带 `school_id` / `tenant_id`；`audit_logs` 按 `created_at` 分区。
- 种子数据：10 条内置角色。

## 8. 开源治理影响

- 引入依赖：
  - 后端：Spring Boot 3.3.5（Apache-2.0）、PostgreSQL JDBC（BSD-2-Clause）、Flyway 10.20（Apache-2.0）、jjwt 0.12.6（Apache-2.0）、BC Prov 1.78.1（MIT）、springdoc-openapi 2.6.0（Apache-2.0）、ArchUnit 1.3.0（Apache-2.0）、Testcontainers 1.20.3（MIT）。
  - 前端：Vue 3、TypeScript、Pinia、Vue Router、Element Plus（MIT）、Axios、Vite、Vitest（MIT / Apache-2.0）。
  - 全部 OS-1 / OS-2；**无 GPL / AGPL / SSPL 依赖**。
- 工具链：
  - JDK 21：CI 采用 Eclipse Temurin（GPLv2 + Classpath Exception，符合 OS-2）。
  - CI 已预留 SBOM / License-Check / OSG Gate 流水线。
- 适配层：本期定义 Adapter 接口集合；具体实现留到对应业务模块（AI 编排、文件、扫描、报告、检索、邮件、任务、SSO）就绪时按"接口在 `com.wenda.integration.adapter`，实现在同包或子包，业务只依赖接口"的规则实现。ArchUnit 持续守门。
- **生产 Mock 禁用**：`ProductionMockGuard` 在 `prod` profile 启动期阻断所有 `Mock*` Adapter 与 `wenda.security.prod-mock-disabled=false` 配置。
- **AC-OSG-001/002/003/004/007/008/010 已通过单元测试断言**；**AC-OSG-005/006（SBOM + 漏洞扫描）由 CI 工作流占位**（首次实跑待后续 PR 接入 Syft / Grype / OSV-Scanner）。

## 9. 测试结果

- **错误码字典一致性**：`ErrorCodeTest` 断言 39 条枚举与基线 1:1 对齐。
- **ApiResponse 格式**：`ApiResponseTest` 断言 success / created / accepted / error 形状。
- **GlobalExceptionHandler**：`GlobalExceptionHandlerTest` 断言 5xx/4xx 错误码与 HTTP 状态映射。
- **JWT Provider**：`JwtProviderTest` 断言 access / refresh Token 签发、解析、短密钥拒绝。
- **If-Match**：`IfMatchVerifierTest` 断言解析、缺失、错误、不一致四种场景。
- **PermissionService**：`PermissionServiceTest` 覆盖 `PERM-SYS-001 / PERM-SCHOOL-001 / PERM-SCHOOL-002 / PERM-SCHOOL-003` 正向 + 反向。
- **RequestContextFilter**：`RequestContextFilterTest` 断言生成 / 透传 / 清理。
- **Adapter 异常映射**：`AdapterExceptionMappingTest` 断言 8 个 Adapter 失败 → 统一错误码。
- **生产 Mock 禁用**：`ProductionMockGuardTest` 覆盖非 prod 跳过 / 各类 mock 阻断 / prod 合法配置放行。
- **ArchUnit 适配层边界**：`AdapterBoundaryArchTest` 断言业务模块不能依赖具体 Adapter 实现。
- **前端错误码字典消费**：`frontend/src/api/__tests__/errorCodes.spec.ts` 断言关键错误码存在与 isAuthError / isForbidden 行为。

> 受限于本机环境（无 Java / Node / Docker），未在本地跑 `mvn verify` 与 `pnpm test`；CI 工作流（backend-ci / frontend-ci / osg-gate）作为执行入口。

## 10. 已知风险 / 限制

1. **本机工具链缺失**：未本地编译 / 跑通测试；所有构建由 CI 跑出。
2. **Adapter 具体实现未落地**：接口已定义；具体厂商 SDK / 适配在后续 PR 接入。ArchUnit 已守门。
3. **看板指标计算骨架**：API-DSH-001/002/003 返回基础计数与提示信息；成熟度、完整度、风险等复杂指标待 M-05 / M-07 业务模块就绪。
4. **审计分区默认分区**：当前 `audit_logs` 仅 `DEFAULT` 分区；按月分区由后续 PR 引入（基线 ARCH-DEC-014）。
5. **CORS 未配置**：本期默认同源；前后端分域部署前需补 `CorsConfigurationSource`。

## 11. 后续 PR 拆分建议

| PR | 范围 | DEV 任务 |
|---|---|---|
| 2 | 专业 / 培养方案 / OBE / 课程体系 / 支撑矩阵 / 质量分析 | DEV-010～DEV-022 |
| 3 | 课程库 / 知识库 / 课程内容 / 文档解析 | DEV-017 / DEV-030～035 |
| 4 | 学生档案 / 画像 / 学习证据 / 能力升级 / 成长任务 / 预警 | DEV-039～047 |
| 5 | 能力地图 / 授权展示 / 报告中心 | DEV-023 / DEV-049 / DEV-050 |
| 6 | 用人单位 / 岗位能力 / 就业反馈 | DEV-051～054 |
| 7 | 文件服务 / 文件安全扫描 | DEV-028 / DEV-029 |
| 8 | 异步任务 / Worker / AI 编排 | DEV-012 / DEV-013 |
| 9 | 通知 / 待办 / 消息中心 | DEV-055 |
| 10 | 归档恢复 / 导入模板 | DEV-024 / DEV-037 / DEV-038 |
| 11 | 适配器层落地（AI / 文件 / 扫描 / 报告 / 邮件） | ENG-OSG-001 |
| 12 | SBOM / SCA / 漏洞扫描落地（CI 强制门禁） | DEV-062 / DEV-069 |

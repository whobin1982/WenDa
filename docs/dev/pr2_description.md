## 1. 本 PR 范围

建立 Wenda MVP-1 第一批 P0 闭环的工程化基础，覆盖 30 个核心 API + 1 套错误码字典（**40 条 = 3 个成功码 + 37 个错误码**）+ 1 个权限基础框架 + 1 个审计基础框架。

**后端**：`backend/`（Spring Boot 3.3.5 + Java 21 + Maven + Flyway + Spring Security + jjwt + springdoc + ArchUnit）
- Flyway V1 迁移 18 张表（schools / colleges / users / roles / user_role_scopes / school_quality_rules / course_code_policy / school_ai_settings / school_ability_level_settings / growth_warning_rules / idempotency_keys / audit_logs 分区表 / dashboards_cache / schema_versions），全部带 `school_id` / `tenant_id`。
- 错误码字典枚举（**40 条 = 3 个成功码 + 37 个错误码**）1:1 对齐 `api_error_code_dictionary_v1.0.md`；统一响应 `ApiResponse<T>`；`BusinessException` + `GlobalExceptionHandler`。
- `Idempotency-Key` 拦截器 + `IdempotencyRequestFilter`（`CachedBodyHttpServletRequest` 解决 body 消费问题）+ `If-Match` / 版本字段校验；`JWT Provider` + 鉴权过滤器；`RequestContext` 持有 `requestId / schoolId / userId / roles`。
- `AuditAspect` + `AuditService`（追加式审计）；`PermissionService` 实现 `PERM-SYS-001 / PERM-SCHOOL-001/002/003`。
- Adapter 接口集合（AI / Storage / Scanner / Renderer / Search / Email / TaskQueue / Auth），具体实现留到对应业务模块。
- `ProductionMockGuard` 启动期阻断 prod profile 下 Mock Adapter。

**前端**：`frontend/`（Vue 3 + TS + Vite + Pinia + Vue Router + Element Plus + Axios + Vitest）
- 统一 `request` 封装（`X-Request-Id` 自动注入、`Idempotency-Key` 自动注入、401/403 路由处理、错误码字典消费）。
- 路由：login / me / dashboard / school / users / settings / audit。
- RBAC 前端菜单 hook `usePermission`（仅体验优化，不作安全边界）。

**API（30 + 兼容端点）**
- Auth：API-AUTH-001/002/003/004
- Org：API-ORG-001/002/003/004/005/006
- User / Role：API-USER-001/002/003/004/005/006 + API-ROLE-001
- Settings：API-CFG-001/002/003/004/005/006/007/008/009/010/011/012
- Audit：API-AUD-001
- Dashboard：API-DSH-001/002/003（骨架）

**测试**
- 后端单元 / ArchUnit：51 个用例
- Flyway + Spring Boot IT：5 个用例（Flyway V1 迁移、关键表存在、10 条系统角色种子、`/actuator/health` 200 + status=UP、未登录受保护 API 返回 401、login 带 `Idempotency-Key` 时 JSON body 不被消费）
- IdempotencyBehaviorIT：3 个用例（Case A 相同 key + 相同 body 命中首次结果 / Case B 相同 key + 不同 body 返回 `IDEMPOTENCY_CONFLICT` / Case C 不同 user / school 相同 key 互不影响）

**CI**：`backend-ci` / `frontend-ci` / `osg-gate` 三件套

**文档**：`docs/dev/baseline_digest.md`、`docs/dev/phase1_p0_summary.md`、`docs/dev/github_issues_milestones.md`

## 2. 关联 DEV 任务

- DEV-003 认证（API-AUTH-*）
- DEV-004 学校空间 / 学院（API-ORG-*）
- DEV-005 用户 / 角色元数据（API-USER-* / API-ROLE-001）
- DEV-007 质量阈值 / 临时代码策略（API-CFG-001/002/009/010）
- DEV-008 AI / 能力等级 / 预警 / AI 策略（API-CFG-003/004/005/006/007/008/011/012）
- DEV-009 看板（API-DSH-001/002/003）
- DEV-025 审计（API-AUD-001）

## 3. 关联 API（30 + 兼容端点）

参见 `docs/dev/phase1_p0_summary.md` §3 完整映射表。

## 4. 关联需求

- FR-001 学校、学院、用户与角色管理
- FR-002 权限控制、学生授权与操作审计
- FR-003 多学院、多专业管理
- FR-031 学校级配置中心
- NFR-010 学生数据出域（AI 配置禁用默认开启 + 启用须审批）
- NFR-012 审计日志（追加式 audit_logs 分区表）
- NFR-013 部署 / 学校空间隔离（所有核心表带 school_id / tenant_id）

## 5. 关联验收项

- **角色级**：AC-RBAC-001/002/006/007/008/010（P/N）
- **接口级**：AC-API-AUTH-001～004、AC-API-ORG-001～006、AC-API-USER-001～006、AC-API-ROLE-001、AC-API-CFG-001～012、AC-API-AUD-001、AC-API-DSH-001/002/003
- **OSG**：AC-OSG-001/003/004/007/008/010 通过单元测试 + ArchUnit + 启动断言保障

## 6. 权限影响

- 鉴权边界在后端；前端菜单只用于体验优化（基线权限判定矩阵 v1.0 §1 第 6 条）。
- 跨 schoolId 访问返回 `SCOPE_FORBIDDEN`；越权写入进入 `audit_logs`，`risk_level = SENSITIVE`。
- 学生数据禁出域通过 `school_ai_settings.student_data_outbound` + 启用审批硬性约束。
- 系统管理员创建学校空间走 `PERM-SYS-001`，调用前 `requireSystemAdminBootstrap()`。

## 7. 数据库迁移影响

新增 `db/migration/V1__init_core_tables.sql`：
- 18 张表，全部带 `school_id` / `tenant_id`；`audit_logs` 按 `created_at` 分区。
- 种子数据：10 条内置角色（SYSTEM_ADMIN / SCHOOL_ADMIN / COLLEGE_MANAGER / MAJOR_OWNER / ACADEMIC_ADMIN / TEACHER / MENTOR / STUDENT / EMPLOYER_MENTOR / KNOWLEDGE_ADMIN）。
- 初始 schema 版本记录 `db/V1`。

## 8. 开源治理影响

- **新引入依赖（后端）**：Spring Boot 3.3.5、PostgreSQL JDBC、Flyway 10.20、jjwt 0.12.6、BC Prov 1.78.1、springdoc-openapi 2.6.0、ArchUnit 1.3.0、Testcontainers 1.20.3。**全部 OS-1 / OS-2**；**无 GPL / AGPL / SSPL**。
- **新引入依赖（前端）**：Vue 3、TypeScript、Pinia、Vue Router、Element Plus、Axios、Vite、Vitest。**全部 MIT / Apache-2.0**。
- **JDK**：CI 采用 Eclipse Temurin（GPLv2 + Classpath Exception，OS-2）。
- **本 PR 已完成 CycloneDX SBOM 强制门禁**：SBOM 生成失败、缺失或为空时 osg-gate 失败。Grype / OSV / Trivy 漏洞扫描仍属后续 PR DEV-062 / DEV-069。
- **生产 Mock 禁用**：`ProductionMockGuard` 在 prod profile 启动期阻断所有 `Mock*` Adapter 与 `wenda.security.prod-mock-disabled=false` 配置。

## 9. 测试结果

**后端单元 + ArchUnit**：`mvn -B -ntp test` 通过（51 个用例；错误码字典一致性 1:1 / ApiResponse 格式 / GlobalExceptionHandler 错误码映射 / JWT 签发与短密钥拒绝 / If-Match 4 种场景 / PermissionService 正反向 / RequestContextFilter 透传与清理 / Adapter 异常映射 8 类 / ProductionMockGuard 启动期断言 / ArchUnit 业务模块不依赖具体 Adapter）。

**Flyway + Spring Boot IT**：`mvn -B -ntp verify -DskipTests=true -DskipITs=false` 通过（5 个用例）。

**IdempotencyBehaviorIT**：3 类幂等行为测试覆盖：
- **Case A** 相同 key + 相同 body：第二次请求返回 201 + 响应 data.id 与首次一致；DB `colleges` 仅 +1；`idempotency_keys` 仅 1 条。
- **Case B** 相同 key + 不同 body：返回 409 + `code=IDEMPOTENCY_CONFLICT` + `success=false`；DB `idempotency_keys` 仍 1 条。
- **Case C** 不同 user / school 相同 key：3 个 `(schoolId, userId, key)` 组合各自独立 201；DB `idempotency_keys` 该 key 共 3 条；两个 school 的 college id 不同。

**前端**：errorCodes.spec.ts 断言关键错误码与 isAuthError / isForbidden 行为。

**CI**（最新 commit `3875801`，2 个 run × 4 个 workflow 全部 pass）：

```text
Backend CI: pass
Frontend CI: pass
OSG gate: pass
Flyway + Spring Boot IT: pass
```

## 10. 已知风险

1. CORS 未配置；后续分域部署前需补 `CorsConfigurationSource`。
2. `audit_logs` 目前仅 `DEFAULT` 分区；按月分区由后续 PR 引入（基线 ARCH-DEC-014）。
3. Adapter 具体实现未落地（仅接口 + ArchUnit 守门）；按对应业务模块接入。
4. `license-maven-plugin` 2.7.1 无 `check` goal；license-scan 阻断留待后续 PR。
5. Grype / OSV-Scanner / Trivy 漏洞扫描未落地（明确属后续 PR DEV-062 / DEV-069）。
6. PR #2 范围偏大（30 API + 9 模块 + 4 workflow + 8 个测试 commit），但作为第一批工程骨架 PR 可接受；后续必须严格小步 PR（每次 ≤ 1 个 DEV 任务 / ≤ 5 个 API）。
7. IT 用了真实 Postgres + BCrypt + JWT；本地无 JDK 21 时无法本地复跑，建议仅依赖 CI 验证。

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

---

> 本 PR 由 Claude Opus 4.8 按基线 `doc/baseline/development_input_baseline_index_v1.0.md` 自动生成；所有规则、API、权限、验收均与基线一一对齐。

🤖 Generated with [Claude Code](https://claude.com/claude-code)

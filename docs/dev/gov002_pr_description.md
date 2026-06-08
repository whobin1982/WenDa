## 1. 本 PR 范围

按 GOV-002（Issue #5）跟踪，修复 PR #2 合并后外部 GitHub 审查发现的 9 项缺陷（4 高风险 + 5 中风险），不动业务模块 API。

## 2. 关联 Issue

- GOV-002（Issue #5）— 仓库治理与外部审查反馈跟踪

## 3. 关联 DEV 任务

无新增 DEV 任务（纯修复 + 工程治理）。

## 4. 关联 API

无新增 / 修改 API 端点；保持 PR #2 的 30 个 API + 9 个模块行为不变。

## 5. 修复清单

**高风险（4 项）**

### #1 IdempotencyResponseAdvice 三子项
- 持久化范围由 POST 扩到 POST/PUT/PATCH/DELETE（`IDEMPOTENT_METHODS` set）
- 响应体序列化改用 `ObjectMapper`（不再 `body.toString()`）
- 业务异常清理 inFlight 占位：`IdempotencyInterceptor.afterCompletion` + `clearInFlight` 方法
- 非 2xx 响应清理 inFlight（Advice 调用 `clearInFlight`）

### #2 user_role_scopes 部分唯一索引
- 新增 V2__user_role_scopes_partial_unique_index.sql
- 删除 V1 全表唯一索引 `uq_urs_user_role_scope`
- 创建 `uq_urs_user_role_scope_active`：`WHERE revoked_at IS NULL`
- COALESCE(college_id, 哨兵 UUID) 区分无 college / 有 college

### #3 LocalAuthProvider refresh 安全
- refresh 时重新查 user.status + 当前角色列表
- 用户禁用 / 归档 / 角色全撤销 → 撤销所有 session + FORBIDDEN
- 新增 `UserSessionRepository.revokeAllForUser(userId)`

### #4 frontend-ci.yml 假绿
- 删除 `lint / test / build` 后的 `|| true`
- 创建 `frontend/eslint.config.js`（ESLint v9 flat config）让 lint 真实门禁
- 引入 `@eslint/js / typescript-eslint / eslint-plugin-vue / vue-eslint-parser`

**中风险（5 项）**

### #5 SYSTEM_ADMIN bootstrap 不可达
- 新增 V3__system_admin_bootstrap.sql
- `users.school_id` 与 `user_role_scopes.school_id / tenant_id` 改 NULL allowed
- 种子内置 SYSTEM_ADMIN root 用户 (id 固定 00000000-...)

### #6 SettingsService PATCH 局部更新重置
- `updateCourseCodePolicy / updateAISettings / updateWarningRules / updateAIPolicy` 改接收 `Map<String, Object> body`
- 用 `body.containsKey` 区分"未传"与"传了 false"
- Controller 调 `*Raw` 方法
- AI 启用约束检查用 effectiveEnabled（合并 current）

### #7 AuditAspect 业务异常状态码
- 不再一律记 422
- `BusinessException` 取 `errorCode.httpStatus().value()` 真实状态
- 保持 `AccessDeniedException → 403 / AuthenticationException → 401 / default → 500`

### #8（与 #5 合并解决）
### #9（已在 #1 修复）

## 6. 权限影响

无新增权限规则；现有 PERM-* 实现未变。修复 #3 间接收紧：刷新 access token 强制重新校验用户状态。

## 7. 数据库迁移影响

新增 2 个 Flyway 迁移：
- V2__user_role_scopes_partial_unique_index.sql
- V3__system_admin_bootstrap.sql

下游 V1 schema 不变。V2 必须 V1 后跑；V3 必须 V1/V2 后跑（按 Flyway 版本顺序自动）。

## 8. 开源治理影响

- 新增前端依赖：`@eslint/js`（MIT）、`typescript-eslint`（Apache-2.0）、`vue-eslint-parser`（MIT）
- 全部 OS-1 / OS-2
- 生产 Mock 禁用策略不变
- SBOM 通过 CycloneDX 自动生成
- OSG gate 已收紧（hard gate）

## 9. 测试结果

```text
Backend CI: pass（V1 + V2 + V3 迁移 + 51 个单测 + ArchUnit）
Frontend CI: pass（vue-tsc 严格类型 + ESLint flat config 真实门禁 + vitest + vite build）
OSG gate: pass（SBOM hard gate + OSG doc check）
Flyway + Spring Boot IT: pass
```

## 10. 已知风险

1. 新加 V2/V3 迁移需在集成测试环境跑通（CI Flyway + IT 已通过）；
2. SYSTEM_ADMIN root 用户仅在 V3 首次部署时种入；升级前已存在 SYSTEM_ADMIN 的部署不需要 V3 种入（ON CONFLICT DO NOTHING）；
3. 部分现有用户的 role binding 在 V2 索引下可能需重新同步（如果有重复 active 行）；
4. `audit / tsc --noEmit` 跑通不等于 `vue-tsc` 严格模式跑通；后续 PR 必须以 frontend-ci 为准。

## 11. 后续 PR

- 启动 PR #3：DEV-010（专业管理最小切片，API-MAJ-001～006）
- 后续按 GOV-001 规则小步推进

---

> 本 PR 由 Claude Opus 4.8 按 GOV-002（Issue #5）自动生成；修复与基线逐一对应。

🤖 Generated with [Claude Code](https://claude.com/claude-code)

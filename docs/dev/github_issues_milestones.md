# GitHub Issue / Milestone 模板（可复制）

> 本文件是 gstack 团队模式下可复制到 GitHub Issue / Milestone 的内容。
> 本次会话**没有**自动在 GitHub 上创建 Issue / Milestone（你未明确要求），仅落盘到仓库。
> 如需批量创建，可在 gh CLI 可用的环境运行：
>
> ```bash
> gh milestone create "MVP-1 专业建设闭环" --due-on 2026-08-31 --description "..."
> gh label create mvp-1 --color 0E8A16
> gh issue create --title "..." --label "mvp-1,backend" --milestone "MVP-1 专业建设闭环" --body "..."
> ```

## Milestones

- **MVP-1 专业建设闭环**：本期第一批 P0。DEV-001～DEV-009 + DEV-025 + 错误码字典 + 统一响应 + 幂等键 + 审计基础 + 权限基础。
- **MVP-2 课程知识库**：课程库 / 知识库 / 课程内容 / 文档解析。
- **MVP-3 学生成长**：学生档案 / 画像 / 学习证据 / 能力升级 / 成长任务 / 预警。
- **MVP-4 能力地图授权**：能力地图 / 授权展示 / 用人单位 / 就业反馈 / 报告中心。

## Labels

| Label | 说明 | 颜色 |
|---|---|---|
| `mvp-1` | MVP-1 范围 | 0E8A16 |
| `mvp-2` | MVP-2 范围 | 1D76DB |
| `mvp-3` | MVP-3 范围 | 5319E7 |
| `mvp-4` | MVP-4 范围 | BFD4F2 |
| `backend` | 后端工作 | C5DEF5 |
| `frontend` | 前端工作 | BFD4F2 |
| `api` | API 契约 | FBCA04 |
| `rbac` | 权限 / RBAC / ABAC | D93F0B |
| `acceptance` | 验收相关 | 0E8A16 |
| `open-source-governance` | 开源治理 / SBOM / SCA | B60205 |
| `p0` | 必须本期完成 | B60205 |

## Issue 列表（示例：本期第一批 P0）

> 数量较多，仅列 5 个代表性模板；其他按 `task_api_mapping_matrix_v1.0.md` 同样格式生成。

### Issue 1：feat(auth): 实现 MVP-1 认证模块（API-AUTH-001/002/003/004）

- **Milestone**：MVP-1 专业建设闭环
- **Labels**：mvp-1, backend, api, p0, acceptance
- **DEV**：DEV-003
- **API**：API-AUTH-001, API-AUTH-002, API-AUTH-003, API-AUTH-004
- **FR / NFR**：FR-001, FR-002, NFR-013
- **验收项**：AC-API-AUTH-001/002/003/004
- **实现要点**：
  - LocalAuthProvider 实现 AuthenticationProvider；
  - POST `/api/v1/auth/login` 支持 `Idempotency-Key`；
  - GET `/api/v1/auth/me` 返回当前用户 + 角色；
  - JWT 签发 / 解析 / 刷新；
  - BCrypt 密码哈希；
  - 失败 5 次自动锁 15 分钟。
- **测试**：
  - 单元：JWT 签发 / 解析 / 短密钥拒绝；
  - 接口：错误密码 / 账号锁定 / Token 过期；
  - 错误码：`UNAUTHORIZED / FORBIDDEN / VALIDATION_ERROR`。
- **依赖**：PR Phase-1-P0-foundation 已合并。

### Issue 2：feat(org): 实现学校空间与学院（API-ORG-001/002/003/004/005/006）

- **Milestone**：MVP-1 专业建设闭环
- **Labels**：mvp-1, backend, api, p0, rbac, acceptance
- **DEV**：DEV-004
- **API**：API-ORG-001～006
- **FR / NFR**：FR-001, FR-003, NFR-013
- **权限**：PERM-SYS-001（创建学校空间）、PERM-SCHOOL-001（学校内组织）
- **验收项**：AC-API-ORG-001～006
- **测试**：
  - 正向：SYSTEM_ADMIN 创建学校、当前学校读取、PATCH + If-Match；
  - 反向：跨 schoolId 写 → SCOPE_FORBIDDEN；IDOR 越权。
- **依赖**：认证模块已合并。

### Issue 3：feat(user): 实现用户与角色管理（API-USER-001～006 + API-ROLE-001）

- **Milestone**：MVP-1 专业建设闭环
- **Labels**：mvp-1, backend, api, p0, rbac, acceptance
- **DEV**：DEV-005
- **API**：API-USER-001～006, API-ROLE-001
- **FR / NFR**：FR-001, FR-002
- **权限**：PERM-SCHOOL-001
- **验收项**：AC-API-USER-001～006, AC-API-ROLE-001

### Issue 4：feat(settings): 实现学校级配置中心（API-CFG-001～012）

- **Milestone**：MVP-1 专业建设闭环
- **Labels**：mvp-1, backend, api, p0, rbac, acceptance
- **DEV**：DEV-007, DEV-008
- **API**：API-CFG-001～012
- **FR / NFR**：FR-011, FR-014, FR-023, FR-024, FR-030, FR-031, NFR-007, NFR-010
- **权限**：PERM-SCHOOL-002, PERM-AI-001
- **验收项**：AC-API-CFG-001～012
- **硬性约束**：启用外部 AI 必须有 approvalRecordId；学生数据出域默认 false。

### Issue 5：feat(audit): 实现审计日志查询（API-AUD-001）

- **Milestone**：MVP-1 专业建设闭环
- **Labels**：mvp-1, backend, api, p0, rbac, acceptance
- **DEV**：DEV-025
- **API**：API-AUD-001
- **FR / NFR**：FR-002, FR-032, NFR-012
- **权限**：PERM-SCHOOL-003
- **验收项**：AC-API-AUD-001

## 后续 PR 拆分

参见 `docs/dev/phase1_p0_summary.md` §11。

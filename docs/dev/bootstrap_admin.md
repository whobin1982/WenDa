# SYSTEM_ADMIN Bootstrap 运维指南

> 基线：GOV-002 外部审查 #6；GOV-001 仓库治理。
> 适用范围：MVP 阶段空白环境初始化（首个 SYSTEM_ADMIN 账户）。

## 1. 背景

MVP 阶段的 `LocalAuthProvider.authenticate()` 走**普通学校级登录流程**：
`POST /api/v1/auth/login` 接收 `schoolCode + username + password`。

为支持 SYSTEM_ADMIN 后续**创建学校空间**（API-ORG-001），需要至少一个 SYSTEM_ADMIN 账户。
该账户**不绑定任何学校**（`school_id = NULL`），用于平台级初始化。

GOV-002 修复 #6 选择**方案 A**：MVP 阶段**不自动 seed** SYSTEM_ADMIN 账户，避免把"占位密码 / 占位用户"
硬编码进仓库；改为运维通过 `db/seed/dev_bootstrap_admin.example.sql` 模板在空白环境**手动 seed**。

## 2. 关键约束

- **真实密码 / bcrypt hash 不得提交仓库**。本仓库仅保留 `dev_bootstrap_admin.example.sql` 模板；
  所有占位符（`:SCHOOL_ID` / `:USER_ID` / `:USERNAME` / `:PASSWORD_HASH` 等）必须在本地替换为实际值。
- **登录仍走普通 `schoolCode + username + password` 流程**。SYSTEM_ADMIN 的 `username` 仍
  必须绑定到某个 `schoolCode`（即便 `users.school_id = NULL`）——**目前 MVP 阶段不支持
  "系统级登录入口"**（如 `schoolCode = SYSTEM`）。该能力是 GOV-002 #6 方案 B 的内容，**未在
  本期实现**。
- 部署到生产前，运维必须：
  1) 在 `schools` 表插入 1 条学校记录（`school_id` 与 SYSTEM_ADMIN 无关但为审计需要）；
  2) 在 `users` 插入 SYSTEM_ADMIN 用户（`school_id = NULL`）；
  3) 在 `user_credentials` 插入本地生成的 bcrypt 哈希；
  4) 在 `user_role_scopes` 绑定 SYSTEM_ADMIN 角色（`school_id / college_id / tenant_id` 均为 NULL）。
- 完成后 SYSTEM_ADMIN 仍通过普通 `/auth/login`（带一个**已知**的 `schoolCode`）登录；
  login 时 `LocalAuthProvider` 会先以 `schoolCode` 查 `schools`，再用 `school_id + username` 查 `users`——
  对 SYSTEM_ADMIN，**这一步的"用 school_id 查"会失败**，因此**MVP 阶段 SYSTEM_ADMIN 不能登录**。

## 3. MVP 阶段限制

为避免 MVP 阶段 SYSTEM_ADMIN 流程的复杂度，本期工程**未提供 SYSTEM_ADMIN 登录入口**。
MVP 阶段真实工作流程：

1. **学校管理员 / 教务 / 教师 / 学生账号** 走 `schoolCode + username + password` 登录；
2. **创建第一所学校 + 第一个 SCHOOL_ADMIN 账号** 由运维**直接在数据库**初始化（用 `dev_bootstrap_admin.example.sql` 模板，但**当前**把第一个用户改成 SCHOOL_ADMIN 而非 SYSTEM_ADMIN，绑定到具体学校）；
3. **系统级操作**（跨学校）MVP 阶段**不实现**。

未来阶段（不在本期范围）：

- 实现 GOV-002 #6 方案 B：系统级登录（`schoolCode = SYSTEM`）、SYSTEM_ADMIN 可通过
  `POST /auth/login` 凭 `username + password`（不需 schoolCode）登录，访问 `POST /api/v1/schools`；
- Adapter 红线、跨学校权限、SSO / OAuth / SAML 接入（基线 §6.2 M-01）。

## 4. 运维步骤（空白环境初始化）

```bash
# 1) 进入 backend 目录，确保 Flyway 迁移已执行（V1 / V2 / V3）
cd backend

# 2) 生成本地 UUID 与 bcrypt 哈希（不提交到仓库）
SCHOOL_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
USER_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
SCOPE_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
TENANT_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')

# 用 Java 生成 bcrypt 哈希（密码由操作员选择，绝不写入脚本）
# （实际生产中运维应使用平台 DBA 工具 / 后台 seed 工具生成）

# 3) 复制 db/seed/dev_bootstrap_admin.example.sql 为 db/seed/dev_bootstrap_admin.local.sql
#    替换 :SCHOOL_ID / :USER_ID / :SCOPE_ID / :TENANT_ID / :SCHOOL_CODE / :SCHOOL_NAME /
#    :USERNAME / :PASSWORD_HASH 为实际值
#    （dev_bootstrap_admin.local.sql 必须 gitignored，绝不提交）

# 4) 加载到目标 DB
psql "$DATABASE_URL" -f src/main/resources/db/seed/dev_bootstrap_admin.local.sql

# 5) 验证四件套
psql "$DATABASE_URL" -c "
  SELECT count(*) AS schools FROM schools WHERE id = '$SCHOOL_ID';
  SELECT count(*) AS users FROM users WHERE id = '$USER_ID' AND school_id IS NULL;
  SELECT count(*) AS credentials FROM user_credentials WHERE user_id = '$USER_ID';
  SELECT count(*) AS role_scopes FROM user_role_scopes WHERE user_id = '$USER_ID' AND role_code = 'SYSTEM_ADMIN';
"
# 期望四个 count 都为 1
```

## 5. 真实凭据生成（Java / Python）

**Java（Spring Boot 标准）**：

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class HashGen {
    public static void main(String[] args) {
        String pwd = args[0]; // 来自 stdin 或安全输入；不写入脚本
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder(12);
        System.out.println(enc.encode(pwd));
    }
}
```

**Python（passlib）**：

```python
from passlib.hash import bcrypt
import sys
pwd = sys.argv[1] if len(sys.argv) > 1 else input("password: ")
print(bcrypt.using(rounds=12).hash(pwd))
```

## 6. 与基线 / Issue 的关系

- GOV-001（Issue #3）— 仓库治理与准入
- GOV-002（Issue #5）— 本轮外部审查修复
- 基线 §6.2 M-01 认证模块、§8 Adapter 边界、§11 NFR-013 学校空间隔离
- 任务-接口映射表：API-ORG-001 关联 DEV-004；MVP 阶段在数据库预置替代 SYSTEM_ADMIN 自动化

## 7. 后续 PR 范围

- PR #3（DEV-010 / API-MAJ-001～006 专业管理）：不动 SYSTEM_ADMIN 流程；
- 后续 PR：实现 GOV-002 #6 方案 B（系统级登录 + SYSTEM_ADMIN 创建学校 + 跨学校权限隔离）；
  该 PR 需重审认证 / 上下文 / 权限 / 审计四个模块。

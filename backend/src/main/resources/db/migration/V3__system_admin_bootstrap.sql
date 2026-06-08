-- ============================================================================
-- Wenda 基线迁移 V3
-- 范围：放宽 users / user_role_scopes 的 school_id / tenant_id 为 NULL
--       允许系统级用户 / 角色绑定（学校初始化 / 跨学校运维场景）。
-- 基线：GOV-002 外部审查 #6
--
-- 重要：V3 不再自动 seed SYSTEM_ADMIN root 用户。
-- MVP 阶段第一个 SYSTEM_ADMIN 账户的创建走运维 seed 机制：
--   1) 部署后由运维通过 `db/seed/dev_bootstrap_admin.example.sql`
--      （或生产定制的等价脚本）插入 school + user + user_credentials + user_role_scopes。
--   2) 真实密码 / bcrypt hash 不得提交仓库；运维用本地脚本生成。
--   3) 登录走普通 `schoolCode + username + password` 流程。
--
-- 详见 docs/dev/bootstrap_admin.md。
-- ============================================================================

-- 放宽 NOT NULL：允许系统级用户 / 角色绑定
ALTER TABLE users ALTER COLUMN school_id DROP NOT NULL;
ALTER TABLE user_role_scopes ALTER COLUMN school_id DROP NOT NULL;
ALTER TABLE user_role_scopes ALTER COLUMN tenant_id DROP NOT NULL;

-- 显式 schema 版本记录（不再 seed SYSTEM_ADMIN root）
INSERT INTO schema_versions (id, component, version, notes)
VALUES (gen_random_uuid(), 'db', 'V3',
        '放宽 users/user_role_scopes 的 school_id/tenant_id 为 NULL；MVP 阶段 SYSTEM_ADMIN 不自动 seed，详见 docs/dev/bootstrap_admin.md')
ON CONFLICT (component, version) DO NOTHING;

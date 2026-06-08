-- ============================================================================
-- Wenda 基线迁移 V3
-- 范围：修复 SYSTEM_ADMIN bootstrap 不可达
-- 基线：GOV-002 外部审查 #8
-- 原因：V1 users.school_id NOT NULL + role 绑定要 school；SYSTEM_ADMIN 创建学校
--      要先有 SYSTEM_ADMIN 用户，陷入鸡生蛋。
-- 修复：
--   1) users.school_id 允许 NULL（系统级用户暂不绑定具体学校）
--   2) 内置 SYSTEM_ADMIN bootstrap 用户（username=root, display_name=系统根账户）
--   3) 同时插入对应 user_role_scopes（school_id 也 NULL；用部分唯一索引变体）
-- ============================================================================

ALTER TABLE users ALTER COLUMN school_id DROP NOT NULL;

-- 放宽 V2 后的 partial unique index：原 (user_id, role_code, school_id, COALESCE(college_id))
-- 现在 school_id 可为 NULL，仍可使用 COALESCE 把 NULL 映射为哨兵 UUID 0。
-- 但 V2 的 SQL 已写明 COALESCE(college_id, '00...0')，不变。
-- 重要：user_role_scopes.school_id 也是 NOT NULL（V1 写死），需要同步放宽。
ALTER TABLE user_role_scopes ALTER COLUMN school_id DROP NOT NULL;
ALTER TABLE user_role_scopes ALTER COLUMN tenant_id DROP NOT NULL;

-- 种子：内置 SYSTEM_ADMIN root 用户（UUID 固定 00000000-0000-0000-0000-000000000001）
-- 注意：必须与 V1 的 roles 种子一起存在；本迁移假定 V1 已执行。
INSERT INTO users (id, school_id, tenant_id, username, display_name, status, user_type)
VALUES (
    '00000000-0000-0000-0000-000000000001'::uuid,
    NULL,
    NULL,
    'root',
    '系统根账户（SYSTEM_ADMIN）',
    'ACTIVE',
    'INTERNAL'
) ON CONFLICT (id) DO NOTHING;

-- 角色绑定：SYSTEM_ADMIN 不绑定 school / college
INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, college_id, is_primary, granted_by)
VALUES (
    '00000000-0000-0000-0000-000000000010'::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    NULL,
    NULL,
    'SYSTEM_ADMIN',
    NULL,
    TRUE,
    '00000000-0000-0000-0000-000000000001'::uuid
) ON CONFLICT DO NOTHING;

-- 显式创建幂等约束 / schema 历史注释
INSERT INTO schema_versions (id, component, version, notes)
VALUES (gen_random_uuid(), 'db', 'V3',
        '修复 SYSTEM_ADMIN bootstrap 不可达：users.school_id NULL + 内置 root 用户')
ON CONFLICT (component, version) DO NOTHING;

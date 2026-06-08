-- ============================================================================
-- Wenda 基线迁移 V4
-- 范围：完善 user_role_scopes 部分唯一索引
-- 基线：GOV-002 外部审查 #2（修复）
--
-- 原因：V2 的 partial unique index 只对 college_id 做 COALESCE：
--   ON user_role_scopes (user_id, role_code, school_id, COALESCE(college_id, 哨兵 UUID))
--   WHERE revoked_at IS NULL
--
--   V3 把 school_id / tenant_id 改为可 NULL（系统级 SYSTEM_ADMIN 场景）。
--   PostgreSQL 在 unique index 中 NULL 与 NULL 不相等（BTree 不索引 NULL 的相等比较）；
--   因此 V2 索引对 (user_id, role_code, school_id=NULL) 无法去重，
--   系统级 active scope 可能重复插入。
--
-- 修复：删除 V2 索引；重建时同时对 school_id 与 college_id 做 COALESCE，
--   让"无 school / 有 college"、"无 college / 有 school"、"两个都无"等 NULL 组合都能
--   精确去重。
-- ============================================================================

DROP INDEX IF EXISTS uq_urs_user_role_scope_active;

CREATE UNIQUE INDEX uq_urs_user_role_scope_active
    ON user_role_scopes(
        user_id,
        role_code,
        COALESCE(school_id,  '00000000-0000-0000-0000-000000000000'::uuid),
        COALESCE(college_id, '00000000-0000-0000-0000-000000000000'::uuid)
    )
    WHERE revoked_at IS NULL;

INSERT INTO schema_versions (id, component, version, notes)
VALUES (gen_random_uuid(), 'db', 'V4',
        '完善 uq_urs_user_role_scope_active：同时 COALESCE(school_id) + COALESCE(college_id)，解决 V3 后系统级 active scope 重复问题')
ON CONFLICT (component, version) DO NOTHING;

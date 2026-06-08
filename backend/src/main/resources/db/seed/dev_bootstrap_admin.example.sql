-- ============================================================================
-- Wenda MVP 阶段 SYSTEM_ADMIN bootstrap 运维 seed 示例
-- ============================================================================
--
-- 用途（基线：docs/dev/bootstrap_admin.md）：
--   本文件作为运维在空白环境初始化第一个 SYSTEM_ADMIN 账户的模板。
--   真实密码 / bcrypt hash 不得提交到仓库；运维在本地生成。
--
-- 必备四件套（缺一不可）：
--   1) schools（学校空间）
--   2) users（SYSTEM_ADMIN 用户，school_id = NULL）
--   3) user_credentials（bcrypt 密码哈希）
--   4) user_role_scopes（绑定 SYSTEM_ADMIN 角色，school_id / college_id / tenant_id 均为 NULL）
--
-- 真实凭据生成示例（不在此文件中包含）：
--   1) 生成 UUID 替换下方占位：
--        SELECT gen_random_uuid() FROM generate_series(1,4);
--   2) 生成 bcrypt 哈希（Java）：
--        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
--        String hash = encoder.encode("REPLACE_WITH_OPERATOR_CHOSEN_PASSWORD");
--   3) 替换下方 :PASSWORD_HASH 占位为 hash。
--
-- 完成后运维通过 psql / 平台 DBA 控制台 / 后台 seed 工具执行。
-- ============================================================================

-- 1) schools：先建一个示例学校空间（占位 UUID；替换为实际值）
INSERT INTO schools (id, school_code, name, short_name, status, tenant_id, version)
VALUES (
    :SCHOOL_ID,
    :SCHOOL_CODE,                          -- 例：'NUAA'
    :SCHOOL_NAME,                          -- 例：'南京航空航天大学'
    NULL,
    'ACTIVE',
    :TENANT_ID,                            -- 自己生成或复用
    0
) ON CONFLICT (id) DO NOTHING;

-- 2) users：SYSTEM_ADMIN 用户（school_id / tenant_id 都为 NULL）
INSERT INTO users (id, school_id, tenant_id, username, display_name, status, user_type, version)
VALUES (
    :USER_ID,                              -- 新生成的 UUID
    NULL,                                  -- 系统级用户不绑定 school
    NULL,                                  -- 系统级用户不绑定 tenant
    :USERNAME,                             -- 例：'root'
    '系统根账户（SYSTEM_ADMIN）',
    'ACTIVE',
    'INTERNAL',
    0
) ON CONFLICT (id) DO NOTHING;

-- 3) user_credentials：bcrypt 密码哈希（运维本地生成）
INSERT INTO user_credentials (user_id, password_hash, password_algo, last_changed_at)
VALUES (
    :USER_ID,
    :PASSWORD_HASH,                        -- BCryptPasswordEncoder(12).encode("...") 生成
    'BCRYPT',
    now()
) ON CONFLICT (user_id) DO NOTHING;

-- 4) user_role_scopes：绑定 SYSTEM_ADMIN 角色
--    （school_id / college_id / tenant_id 全部 NULL；
--      partial unique index uq_urs_user_role_scope_active 的 COALESCE 把 NULL 映射为哨兵 UUID）
INSERT INTO user_role_scopes (id, user_id, school_id, tenant_id, role_code, college_id, is_primary, granted_by)
VALUES (
    :SCOPE_ID,                             -- 新生成的 UUID
    :USER_ID,
    NULL,                                  -- 系统级 scope：school_id NULL
    NULL,                                  -- 系统级 scope：tenant_id NULL
    'SYSTEM_ADMIN',
    NULL,                                  -- 系统级 scope：college_id NULL
    TRUE,
    :USER_ID
) ON CONFLICT DO NOTHING;

-- 验证（执行后请检查行数）：
--   SELECT count(*) FROM schools WHERE id = ':SCHOOL_ID';
--   SELECT count(*) FROM users WHERE id = ':USER_ID' AND school_id IS NULL;
--   SELECT count(*) FROM user_credentials WHERE user_id = ':USER_ID';
--   SELECT count(*) FROM user_role_scopes WHERE user_id = ':USER_ID' AND role_code = 'SYSTEM_ADMIN';
-- 期望四个查询各返回 1。

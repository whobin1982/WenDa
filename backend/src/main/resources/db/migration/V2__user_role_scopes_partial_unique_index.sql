-- ============================================================================
-- Wenda 基线迁移 V2
-- 范围：修复 user_role_scopes 唯一索引不支持"软撤销后重新绑定同一角色"。
-- 基线：GOV-002 外部审查 #5
-- 原因：原 V1 的 uq_urs_user_role_scope 唯一索引对 revoked_at IS NOT NULL 的行
--      仍然生效，导致 UserMgmtRepository.replaceRoleScopes() 先软撤销再插入
--      新行时，被旧记录的唯一索引挡住。
-- 修复：删除原全表唯一索引；创建 partial unique index 限定 revoked_at IS NULL。
-- ============================================================================

ALTER TABLE user_role_scopes DROP CONSTRAINT IF EXISTS uq_urs_user_role_scope;

DROP INDEX IF EXISTS uq_urs_user_role_scope;

-- partial unique index：仅对未撤销的行生效；
-- COALESCE(college_id, '00000000-0000-0000-0000-000000000000') 把 NULL 映射为
-- 哨兵 UUID 0，让"无 college 范围"与"特定 college"在同一索引中能区分。
CREATE UNIQUE INDEX uq_urs_user_role_scope_active
    ON user_role_scopes (user_id, role_code, school_id, COALESCE(college_id, '00000000-0000-0000-0000-000000000000'))
    WHERE revoked_at IS NULL;

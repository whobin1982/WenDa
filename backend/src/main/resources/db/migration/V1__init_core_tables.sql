-- ============================================================================
-- Wenda 基线迁移 V1
-- 范围：MVP-1 第一批 P0 所需的核心表（Auth / Org / User / Role / Settings /
--       Audit / Idempotency / Dashboard 缓存）。
-- 规则：所有核心业务表必须带 school_id / tenant_id；物理删除禁用；
--       字段命名采用 snake_case；版本号字段 version 用于 If-Match。
-- 基线：开发输入基线索引 v1.0 / 系统架构设计 v0.3 §4.2 / 详细技术方案 v0.4
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 租户 / 学校空间 / 学院
-- ----------------------------------------------------------------------------
CREATE TABLE schools (
    id              UUID PRIMARY KEY,
    school_code     VARCHAR(64)  NOT NULL UNIQUE,
    name            VARCHAR(256) NOT NULL,
    short_name      VARCHAR(64),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED
    contact_email   VARCHAR(256),
    contact_phone   VARCHAR(32),
    address         VARCHAR(512),
    description     TEXT,
    tenant_id       UUID         NOT NULL,
    created_by      UUID,
    updated_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,
    archived_at     TIMESTAMPTZ
);
CREATE INDEX idx_schools_tenant ON schools(tenant_id);
CREATE INDEX idx_schools_status ON schools(status);

CREATE TABLE colleges (
    id              UUID PRIMARY KEY,
    school_id       UUID         NOT NULL REFERENCES schools(id),
    tenant_id       UUID         NOT NULL,
    college_code    VARCHAR(64)  NOT NULL,
    name            VARCHAR(256) NOT NULL,
    short_name      VARCHAR(64),
    description     TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by      UUID,
    updated_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,
    archived_at     TIMESTAMPTZ,
    UNIQUE (school_id, college_code)
);
CREATE INDEX idx_colleges_school ON colleges(school_id);

-- ----------------------------------------------------------------------------
-- 2. 用户 / 角色 / 用户角色与 Scope
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    school_id       UUID         NOT NULL REFERENCES schools(id),
    tenant_id       UUID         NOT NULL,
    username        VARCHAR(64)  NOT NULL,
    display_name    VARCHAR(128) NOT NULL,
    email           VARCHAR(256),
    phone           VARCHAR(32),
    avatar_url      VARCHAR(512),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED
    user_type       VARCHAR(16)  NOT NULL DEFAULT 'INTERNAL', -- INTERNAL / EMPLOYER
    last_login_at   TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,
    archived_at     TIMESTAMPTZ,
    UNIQUE (school_id, username)
);
CREATE INDEX idx_users_school ON users(school_id);
CREATE INDEX idx_users_school_status ON users(school_id, status);

CREATE TABLE user_credentials (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    password_hash   VARCHAR(256) NOT NULL,
    password_algo   VARCHAR(32)  NOT NULL DEFAULT 'BCRYPT',
    last_changed_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ
);

CREATE TABLE user_sessions (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id),
    school_id       UUID         NOT NULL,
    tenant_id       UUID         NOT NULL,
    refresh_token   VARCHAR(256) NOT NULL UNIQUE,
    access_jti      VARCHAR(64),
    user_agent      VARCHAR(256),
    ip              VARCHAR(64),
    issued_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ
);
CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_school ON user_sessions(school_id);

-- 角色元数据表
CREATE TABLE roles (
    code            VARCHAR(64) PRIMARY KEY, -- SYSTEM_ADMIN / SCHOOL_ADMIN / ...
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户 ↔ 角色 + Scope（学校 / 学院 / 专业）
CREATE TABLE user_role_scopes (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id),
    school_id       UUID         NOT NULL,
    tenant_id       UUID         NOT NULL,
    role_code       VARCHAR(64)  NOT NULL REFERENCES roles(code),
    college_id      UUID         REFERENCES colleges(id),
    major_id        UUID,        -- 后续专业表创建后加 FK
    is_primary      BOOLEAN      NOT NULL DEFAULT FALSE,
    granted_by      UUID,
    granted_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMPTZ
);
CREATE INDEX idx_urs_user ON user_role_scopes(user_id);
CREATE INDEX idx_urs_school ON user_role_scopes(school_id);
CREATE INDEX idx_urs_role ON user_role_scopes(role_code);
CREATE UNIQUE INDEX uq_urs_user_role_scope
    ON user_role_scopes(user_id, role_code, school_id, COALESCE(college_id, '00000000-0000-0000-0000-000000000000'));

-- ----------------------------------------------------------------------------
-- 3. 学校级配置中心
-- ----------------------------------------------------------------------------
CREATE TABLE school_quality_rules (
    school_id           UUID PRIMARY KEY REFERENCES schools(id),
    min_credits         INT NOT NULL DEFAULT 160,
    max_credits         INT NOT NULL DEFAULT 200,
    min_practice_ratio  NUMERIC(5,2) NOT NULL DEFAULT 0.15,
    max_course_per_term INT NOT NULL DEFAULT 12,
    min_support_degree  VARCHAR(8) NOT NULL DEFAULT 'LOW',
    thresholds_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_by          UUID,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE course_code_policy (
    school_id          UUID PRIMARY KEY REFERENCES schools(id),
    allow_temp_code    BOOLEAN NOT NULL DEFAULT FALSE,
    temp_code_prefix   VARCHAR(8) NOT NULL DEFAULT 'T-',
    temp_code_ttl_days INT NOT NULL DEFAULT 180,
    updated_by         UUID,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE school_ai_settings (
    school_id              UUID PRIMARY KEY REFERENCES schools(id),
    external_provider_code VARCHAR(32) NOT NULL DEFAULT 'disabled',
    external_model_id      VARCHAR(64),
    external_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    student_data_outbound  BOOLEAN NOT NULL DEFAULT FALSE,
    prompt_version         VARCHAR(32) NOT NULL DEFAULT 'v1',
    schema_version         VARCHAR(32) NOT NULL DEFAULT 'v1',
    quota_per_day          INT NOT NULL DEFAULT 0,
    approval_record_id     UUID,
    updated_by             UUID,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE school_ability_level_settings (
    school_id      UUID PRIMARY KEY REFERENCES schools(id),
    levels_json    JSONB NOT NULL DEFAULT '[
        {"code":"L1","name":"入门","order":1,"minScore":0},
        {"code":"L2","name":"基础","order":2,"minScore":40},
        {"code":"L3","name":"胜任","order":3,"minScore":60},
        {"code":"L4","name":"熟练","order":4,"minScore":80},
        {"code":"L5","name":"专家","order":5,"minScore":92}
    ]'::jsonb,
    updated_by     UUID,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE growth_warning_rules (
    school_id          UUID PRIMARY KEY REFERENCES schools(id),
    rules_json         JSONB NOT NULL DEFAULT '{}'::jsonb,
    notification_email BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by         UUID,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
-- 4. 幂等键
-- ----------------------------------------------------------------------------
CREATE TABLE idempotency_keys (
    key                 VARCHAR(128) NOT NULL,
    school_id           UUID NOT NULL,
    user_id             UUID NOT NULL,
    method              VARCHAR(8) NOT NULL,
    path                VARCHAR(256) NOT NULL,
    request_hash        VARCHAR(128) NOT NULL,
    response_status     INT NOT NULL,
    response_body       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (school_id, user_id, key)
);
CREATE INDEX idx_idemp_expires ON idempotency_keys(expires_at);

-- ----------------------------------------------------------------------------
-- 5. 审计日志（追加式）
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    school_id       UUID,
    tenant_id       UUID,
    user_id         UUID,
    user_name       VARCHAR(128),
    action          VARCHAR(64) NOT NULL,    -- CREATE_SCHOOL / UPDATE_USER / LOGIN ...
    resource_type   VARCHAR(64) NOT NULL,    -- school / college / user / role / settings / file / authz
    resource_id     VARCHAR(128),
    method          VARCHAR(8),
    path            VARCHAR(256),
    status_code     INT,
    request_id      VARCHAR(64),
    idempotency_key VARCHAR(128),
    ip              VARCHAR(64),
    user_agent      VARCHAR(256),
    details         JSONB,
    risk_level      VARCHAR(16) NOT NULL DEFAULT 'NORMAL',  -- NORMAL / SENSITIVE / SECURITY
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_audit_school_time ON audit_logs(school_id, created_at);
CREATE INDEX idx_audit_user_time ON audit_logs(user_id, created_at);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_risk ON audit_logs(risk_level);

-- 默认分区：当前月 + 未来 3 个月
CREATE TABLE audit_logs_default PARTITION OF audit_logs DEFAULT;

-- ----------------------------------------------------------------------------
-- 6. 看板聚合缓存（仅 M-04 骨架）
-- ----------------------------------------------------------------------------
CREATE TABLE dashboards_cache (
    school_id      UUID NOT NULL,
    scope_type     VARCHAR(16) NOT NULL,   -- SCHOOL / COLLEGE / MAJOR
    scope_id       UUID NOT NULL,
    payload_json   JSONB NOT NULL DEFAULT '{}'::jsonb,
    generated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (school_id, scope_type, scope_id)
);

-- ----------------------------------------------------------------------------
-- 7. 元数据：schema 版本
-- ----------------------------------------------------------------------------
CREATE TABLE schema_versions (
    id              UUID PRIMARY KEY,
    component       VARCHAR(64) NOT NULL,   -- db / api / ai-prompt / report-template
    version         VARCHAR(32) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (component, version)
);

-- ----------------------------------------------------------------------------
-- 8. 种子数据：内置角色
-- ----------------------------------------------------------------------------
INSERT INTO roles (code, name, description, is_system) VALUES
    ('SYSTEM_ADMIN',      '系统管理员',     '平台级初始化、跨学校运维',         TRUE),
    ('SCHOOL_ADMIN',      '学校管理员',     '学校空间内组织/用户/配置/审计',    TRUE),
    ('COLLEGE_MANAGER',   '学院管理者',     '学院范围内专业管理',                TRUE),
    ('MAJOR_OWNER',       '专业负责人',     '本专业建设/OBE/课程体系/发布',     TRUE),
    ('ACADEMIC_ADMIN',    '教务人员',       '培养方案/课程计划辅助',             TRUE),
    ('TEACHER',           '任课教师',       '所授课程/课程内容/证据审核',        TRUE),
    ('MENTOR',            '导师/班主任',    '所指导学生画像/预警/能力地图',     TRUE),
    ('STUDENT',           '学生',           '本人画像/学习证据/能力地图/授权',   TRUE),
    ('EMPLOYER_MENTOR',   '企业导师/用人单位', '邀请制企业账号/岗位/评价/授权地图', TRUE),
    ('KNOWLEDGE_ADMIN',   '知识库管理员',   '课程知识库资料审核/版权/禁用',     TRUE);

-- 初始 schema 版本记录
INSERT INTO schema_versions (id, component, version, notes) VALUES
    (gen_random_uuid(), 'db', 'V1', 'MVP-1 第一批 P0 基础表');

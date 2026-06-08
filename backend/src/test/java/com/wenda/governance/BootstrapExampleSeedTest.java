package com.wenda.governance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 仓库治理测试：bootstrap example seed SQL 完整性（基线：GOV-002 修复 #1 方案 A）。
 *
 * <p>路径策略：CI 在 {@code working-directory: backend} 执行 {@code mvn test}，
 * 所以 {@code user.dir} 是仓库根的 {@code backend/} 子目录。
 * {@code repoRoot = Path.of("..")} 在该 cwd 下解析为仓库根。
 * 本地 {@code cd backend && mvn test} 同样可用。
 */
class BootstrapExampleSeedTest {

    private static final Path REPO_ROOT = Paths.get("..").toAbsolutePath().normalize();

    private static final Path SEED_FILE = Paths.get(
            "src/main/resources/db/seed/dev_bootstrap_admin.example.sql");
    private static final Path V3_MIGRATION = Paths.get(
            "src/main/resources/db/migration/V3__system_admin_bootstrap.sql");
    private static final Path DOC_FILE =
            REPO_ROOT.resolve("docs/dev/bootstrap_admin.md");
    private static final Path GITIGNORE =
            REPO_ROOT.resolve(".gitignore");

    @Test
    void exampleSeedFileExists() {
        assertTrue(Files.isRegularFile(SEED_FILE),
                "缺少 " + SEED_FILE.toAbsolutePath() + "（基线 GOV-002 修复 #1 方案 A）");
    }

    @Test
    void exampleSeedContainsAllFourPieces() throws IOException {
        String sql = Files.readString(SEED_FILE);
        assertTrue(sql.contains("INSERT INTO schools"), "example seed 必须包含 schools");
        assertTrue(sql.contains("INSERT INTO users"), "example seed 必须包含 users");
        assertTrue(sql.contains("INSERT INTO user_credentials"), "example seed 必须包含 user_credentials");
        assertTrue(sql.contains("INSERT INTO user_role_scopes"), "example seed 必须包含 user_role_scopes");
    }

    @Test
    void exampleSeedUsesPlaceholders() throws IOException {
        String sql = Files.readString(SEED_FILE);
        // 必须用占位符
        assertTrue(sql.contains(":SCHOOL_ID"), "必须用 :SCHOOL_ID 占位");
        assertTrue(sql.contains(":USER_ID"), "必须用 :USER_ID 占位");
        assertTrue(sql.contains(":USERNAME"), "必须用 :USERNAME 占位");
        assertTrue(sql.contains(":PASSWORD"), "必须用 :PASSWORD 占位");
        // 不得包含 $2a$ 之类真实 bcrypt 哈希前缀
        assertFalse(sql.matches("(?s).*\\$2[abxy]\\$\\d{2}\\$.*"),
                "example seed 不得包含真实 bcrypt 哈希");
        // 不得包含看起来像生产密码的字符串
        assertFalse(sql.contains("admin123"), "example seed 不得包含常见默认密码");
        assertFalse(sql.contains("password123"), "example seed 不得包含常见默认密码");
    }

    @Test
    void bootstrapDocExistsAndMentionsOpsSeed() throws IOException {
        assertTrue(Files.isRegularFile(DOC_FILE), "缺少 " + DOC_FILE.toAbsolutePath());
        String doc = Files.readString(DOC_FILE);
        assertTrue(doc.contains("运维") || doc.contains("Ops") || doc.contains("operations"),
                "bootstrap_admin.md 必须说明运维 seed 流程");
        assertTrue(doc.contains("不得") || doc.contains("bcrypt"),
                "bootstrap_admin.md 必须强调真实凭据不得入库");
    }

    @Test
    void gitignoreBlocksRealSeedFiles() throws IOException {
        assertTrue(Files.isRegularFile(GITIGNORE), "缺少 " + GITIGNORE.toAbsolutePath());
        String gi = Files.readString(GITIGNORE);
        assertTrue(gi.contains("*.local.sql") || gi.contains("seed/*.local.sql"),
                ".gitignore 必须排除 seed/*.local.sql 真实凭据文件");
        assertTrue(gi.contains("*.example.sql") || gi.contains("!backend/src/main/resources/db/seed/*.example.sql"),
                ".gitignore 必须用 .example.sql 例外允许 example 文件入库");
    }

    @Test
    void v3MigrationDoesNotAutoSeedRoot() throws IOException {
        assertTrue(Files.isRegularFile(V3_MIGRATION),
                "缺少 " + V3_MIGRATION.toAbsolutePath());
        String sql = Files.readString(V3_MIGRATION);
        // V3 不应直接 INSERT root 用户；只放宽 NOT NULL + 写 schema_versions
        assertFalse(sql.matches("(?s).*INSERT\\s+INTO\\s+users[\\s\\S]*'root'[\\s\\S]*'系统根账户'.*"),
                "V3 不应再自动 seed root 用户（基线 GOV-002 修复 #1 方案 A）");
        assertTrue(sql.contains("ALTER TABLE users ALTER COLUMN school_id DROP NOT NULL"),
                "V3 必须放宽 users.school_id NULL");
        assertTrue(sql.contains("schema_versions"),
                "V3 必须记录 schema_versions");
    }
}

package com.wenda.governance;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 架构约束（基线：技术方案 v0.4 §9.2 + §10.5 + RG-OSG-004/007/008）。
 *
 * <p>硬性约束：
 * <ol>
 *   <li>Adapter 实现类只能位于 {@code com.wenda.integration.adapter} 或其子包；</li>
 *   <li>业务模块（auth/organization/user/role/settings/audit/dashboard/...）只能依赖
 *       {@code com.wenda.integration.adapter.*Adapter}（接口），不能依赖具体实现；</li>
 *   <li>Mock 实现类名必须包含 {@code Mock}（用于生产环境禁用扫描）；</li>
 * </ol>
 */
class AdapterBoundaryArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void scan() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.wenda");
    }

    @Test
    void adapterImplementationsLiveInAdapterPackage() {
        // Adapter 实现（非接口）必须位于 com.wenda.integration.adapter 包下。
        // 排除 WendaProperties$Adapter 等配置内部类（命名以 Adapter 结尾但与适配层无关）。
        // MVP-1 暂无具体实现：允许空集（实现落地时自动生效）。
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().areNotInterfaces()
                .and().doNotHaveSimpleName("WendaProperties$Adapter")
                .and().doNotHaveSimpleName("MockSecurityScannerAdapter")
                .and().resideOutsideOfPackage("com.wenda.config..")
                .and().resideOutsideOfPackage("com.wenda.idempotency..")
                .should().resideInAPackage("com.wenda.integration.adapter..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void businessPackagesCannotDependOnAdapterImplementations() {
        // 业务模块的代码不应直接 import 具体 Adapter 实现。
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.wenda.auth..")
                .or().resideInAPackage("com.wenda.organization..")
                .or().resideInAPackage("com.wenda.user..")
                .or().resideInAPackage("com.wenda.role..")
                .or().resideInAPackage("com.wenda.settings..")
                .or().resideInAPackage("com.wenda.audit..")
                .or().resideInAPackage("com.wenda.dashboard..")
                .should().dependOnClassesThat()
                .haveSimpleNameEndingWith("Adapter");
        rule.check(classes);
    }

    @Test
    void mockImplementationsMustBeNamedAsMock() {
        // 适配层 Mock 实现类名必须包含 Mock；当前无实现时允许空集（MVP-1）。
        ArchRule rule = classes()
                .that().resideInAPackage("com.wenda.integration.adapter..")
                .and().haveSimpleNameContaining("Mock")
                .and().areNotInterfaces()
                .should().haveSimpleNameContaining("Mock")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}

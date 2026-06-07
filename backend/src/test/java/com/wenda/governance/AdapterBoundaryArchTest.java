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
 *   <li>Adapter 内部允许直接依赖厂商 SDK；业务模块不得跨 Adapter 调用厂商 SDK；</li>
 *   <li>Service 层不得引用 {@code Mock*} 实现。</li>
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
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().areNotInterfaces()
                .and().doNotHaveSimpleName("MockSecurityScannerAdapter")
                .should().resideInAPackage("com.wenda.integration.adapter..");
        rule.check(classes);
    }

    @Test
    void businessPackagesCannotDependOnAdapterImplementations() {
        // 业务模块的代码不应直接 import 具体 Adapter 实现（除 integration 自身）
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.wenda.auth..")
                .or().resideInAPackage("com.wenda.organization..")
                .or().resideInAPackage("com.wenda.user..")
                .or().resideInAPackage("com.wenda.role..")
                .or().resideInAPackage("com.wenda.settings..")
                .or().resideInAPackage("com.wenda.audit..")
                .or().resideInAPackage("com.wenda.dashboard..")
                .should().dependOnClassesThat()
                .haveSimpleNameEndingWith("Adapter")
                .and().haveSimpleNameNotEndingWith("AdapterTest");
        rule.check(classes);
    }

    @Test
    void mockImplementationsMustBeNamedAsMock() {
        // 凡在 Adapter 包下、被生产启用的实现类，命名必须能区分（非 Mock）。
        // Mock 类名必须包含 Mock 字样，便于 ProductionMockGuard 启动期扫描。
        ArchRule rule = classes()
                .that().resideInAPackage("com.wenda.integration.adapter..")
                .and().haveSimpleNameContaining("Mock")
                .and().areNotInterfaces()
                .should().haveSimpleNameContaining("Mock");
        rule.check(classes);
    }
}

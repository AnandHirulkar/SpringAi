package io.github.anandhirulkar.supportiq;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Executable architecture documentation: controllers stay thin, services never depend upwards,
 * and the feature packages remain free of cycles.
 */
@AnalyzeClasses(
        packages = "io.github.anandhirulkar.supportiq",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_do_not_use_repositories_directly = noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule services_do_not_depend_on_controllers = noClasses()
            .that()
            .haveSimpleNameEndingWith("Service")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule feature_slices_have_no_cycles = com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
            .slices()
            .matching("io.github.anandhirulkar.supportiq.(*)..")
            .should()
            .beFreeOfCycles();
}

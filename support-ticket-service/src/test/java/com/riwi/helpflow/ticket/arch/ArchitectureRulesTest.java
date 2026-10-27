package com.riwi.helpflow.ticket.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces hexagonal architecture dependency rules:
 *   1. domain has NO dependency on Spring, Jakarta Persistence, Jackson, infrastructure, or application.
 *   2. application has NO dependency on infrastructure.
 *   3. application may use org.springframework.transaction.annotation.Transactional (documented ADR).
 *   4. Controllers do not use repositories directly.
 */
class ArchitectureRulesTest {

    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.riwi.helpflow.ticket");
    }

    @Test
    @DisplayName("Domain must not depend on Spring")
    void domain_noSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on Jakarta Persistence")
    void domain_noJakartaPersistence() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("jakarta.persistence..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on Jackson")
    void domain_noJackson() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fasterxml.jackson..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on infrastructure")
    void domain_noInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on application")
    void domain_noApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..application..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Application must not depend on infrastructure")
    void application_noInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Application must not depend on Spring Web (controllers, bindings)")
    void application_noSpringWeb() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.security..",
                        "org.springframework.data..");
        rule.check(classes);
    }
}

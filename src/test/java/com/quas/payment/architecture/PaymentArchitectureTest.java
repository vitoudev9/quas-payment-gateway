package com.quas.payment.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.quas.payment")
class PaymentArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_api = noClasses()
            .that()
            .resideInAPackage("..payment.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..payment.api..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure = noClasses()
            .that()
            .resideInAPackage("..payment.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..payment.infrastructure..")
            .allowEmptyShould(true);
}

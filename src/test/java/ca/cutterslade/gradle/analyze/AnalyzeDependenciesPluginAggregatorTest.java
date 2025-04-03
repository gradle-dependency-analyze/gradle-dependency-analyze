package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnalyzeDependenciesPluginAggregatorTest extends AnalyzeDependenciesPluginBaseTest {

  @ParameterizedTest
  @MethodSource("provideAggregatorDependencyParameters")
  void aggregatorDependencyDeclaredInConfigAndUsedInBuildResultsInExpectedResult(
      final String configuration, final String expectedResult) throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("Dependent")
                .usesClass("org.springframework.context.annotation.ComponentScan")
                .usesClass("org.springframework.beans.factory.annotation.Autowired"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withDependency(
            new GradleDependency().setConfiguration(configuration).setProject("dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult);

    // Then
    assertBuildResult(result, expectedResult);
  }

  private static Stream<Arguments> provideAggregatorDependencyParameters() {
    return Stream.of(
        Arguments.of("implementation", SUCCESS), Arguments.of("runtimeOnly", BUILD_FAILURE));
  }

  @ParameterizedTest
  @MethodSource("provideUnusedAggregatorDependencyParameters")
  void aggregatorDependencyNotDeclaredInConfigAndUsedInBuildShouldReportUnusedAggregator(
      final String configuration,
      final String expectedResult,
      final String[] usedUndeclaredArtifacts,
      final String[] unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("Dependent")
                .usesClass("org.springframework.context.annotation.ComponentScan")
                .usesClass("org.springframework.beans.factory.annotation.Autowired"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withDependency(
            new GradleDependency().setConfiguration(configuration).setProject("dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult);

    // Then
    assertBuildResult(
        result,
        expectedResult,
        java.util.Arrays.asList(usedUndeclaredArtifacts),
        java.util.Arrays.asList(unusedDeclaredArtifacts));
  }

  private static Stream<Arguments> provideUnusedAggregatorDependencyParameters() {
    return Stream.of(
        Arguments.of(
            "implementation",
            VIOLATIONS,
            new String[] {
              "org.springframework:spring-beans:5.2.11.RELEASE",
              "org.springframework:spring-context:5.2.11.RELEASE"
            },
            new String[] {"org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"}),
        Arguments.of("runtimeOnly", BUILD_FAILURE, new String[] {}, new String[] {}));
  }

  @ParameterizedTest
  @MethodSource("provideMissingDependencyParameters")
  void aggregatorDependencyDeclaredInConfigAndNotUsedInBuildShouldReportMissingDependency(
      final String configuration,
      final String expectedResult,
      final String[] usedUndeclaredArtifacts,
      final String[] unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework:spring-context:5.2.11.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("Dependent")
                .usesClass("org.springframework.context.annotation.ComponentScan")
                .usesClass("org.springframework.beans.factory.annotation.Autowired"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withDependency(
            new GradleDependency().setConfiguration(configuration).setProject("dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult);

    // Then
    assertBuildResult(
        result,
        expectedResult,
        java.util.Arrays.asList(usedUndeclaredArtifacts),
        java.util.Arrays.asList(unusedDeclaredArtifacts));
  }

  private static Stream<Arguments> provideMissingDependencyParameters() {
    return Stream.of(
        Arguments.of(
            "implementation",
            VIOLATIONS,
            new String[] {"org.springframework:spring-beans:5.2.11.RELEASE"},
            new String[] {}),
        Arguments.of("runtimeOnly", BUILD_FAILURE, new String[] {}, new String[] {}));
  }

  @ParameterizedTest
  @MethodSource("provideExplicitDependencyParameters")
  void
      aggregatorDependencyDeclaredInConfigAndUsedInBuildWithDedicatedDependencyShouldReportExplicitDependencyAsUnused(
          final String configuration,
          final String expectedResult,
          final String[] usedUndeclaredArtifacts,
          final String[] unusedDeclaredArtifacts,
          final String[] superfluousDeclaredArtifacts)
          throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework:spring-context:5.2.11.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("Dependent")
                .usesClass("org.springframework.context.annotation.ComponentScan")
                .usesClass("org.springframework.beans.factory.annotation.Autowired"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withDependency(
            new GradleDependency().setConfiguration(configuration).setProject("dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult);

    // Then
    assertBuildResult(
        result,
        expectedResult,
        java.util.Arrays.asList(usedUndeclaredArtifacts),
        java.util.Arrays.asList(unusedDeclaredArtifacts),
        Collections.emptyList(),
        java.util.Arrays.asList(superfluousDeclaredArtifacts));
  }

  private static Stream<Arguments> provideExplicitDependencyParameters() {
    return Stream.of(
        Arguments.of(
            "implementation",
            VIOLATIONS,
            new String[] {},
            new String[] {},
            new String[] {"org.springframework:spring-context:5.2.11.RELEASE"}),
        Arguments.of(
            "runtimeOnly", BUILD_FAILURE, new String[] {}, new String[] {}, new String[] {}));
  }

  @Test
  void
      multipleAggregatorDependenciesDeclaredAndDependencyAvailableInBothAggregatorsShouldChooseAggregatorWithLessDependencies()
          throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("org.springframework.context.annotation.ComponentScan")
                .usesClass("org.springframework.beans.factory.annotation.Autowired"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // Then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.singletonList(
            "org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"));
  }

  @Test
  void
      multipleAggregatorDependenciesDeclaredInConfigAndDependencyAvailableInBothAggregatorsPlusOneAdditionalShouldChooseAggregatorWithLessDependencies()
          throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE"))
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("org.springframework.context.annotation.ComponentScan")
                .usesClass("org.springframework.beans.factory.annotation.Autowired")
                .usesClass("org.springframework.jdbc.BadSqlGrammarException"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // Then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        java.util.Arrays.asList(
            "org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE",
            "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"));
  }

  @Test
  void
      multipleAggregatorDependenciesDeclaredInConfigAndDependencyAvailableInBothAggregatorsPlusOneAdditionalShouldKeepOnlyUsedDistinctAggregators()
          throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE"))
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-validation:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-validation:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("org.springframework.context.annotation.ComponentScan")
                .usesClass("org.springframework.beans.factory.annotation.Autowired")
                .usesClass("org.springframework.jdbc.BadSqlGrammarException")
                .usesClass("org.springframework.web.bind.annotation.RestController")
                .usesClass("javax.validation.ConstraintValidator"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // Then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.singletonList("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"));
  }

  @Test
  void multipleAggregatorDependenciesDeclaredInConfigAndAnotherAggregatorIsSmaller()
      throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE"))
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("com.fasterxml.jackson.databind.ObjectMapper")
                .usesClass("org.springframework.beans.factory.annotation.Autowired")
                .usesClass("org.springframework.web.context.request.RequestContextHolder"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // Then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.singletonList(
            "org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE"),
        java.util.Arrays.asList(
            "org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE",
            "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"));
  }

  @Test
  void aggregatorFromProject() throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency().setConfiguration("permitAggregatorUse").setProject("bom"))
        .withDependency(new GradleDependency().setConfiguration("implementation").setProject("bom"))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("dependent"))
        .withSubProject(
            subProject("bom")
                .withPlugin("java-library")
                .withDependency(
                    new GradleDependency().setConfiguration("api").setProject("dependent")))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // Then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.singletonList("project :dependent"));
  }

  @Test
  void aggregatorFromProjectWithUsedJarDependency() throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency().setConfiguration("permitTestAggregatorUse").setProject("spring"))
        .withSubProject(
            subProject("spring")
                .withPlugin("java-library")
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("api")
                        .setId("org.springframework:spring-beans:5.2.11.RELEASE"))
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("api")
                        .setId("org.springframework:spring-context:5.2.11.RELEASE")))
        .withSubProject(
            subProject("tests")
                .withMavenRepositories()
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("testImplementation")
                        .setProject("spring"))
                .withTestClass(
                    new GroovyClass("Main")
                        .usesClass("org.springframework.context.annotation.ComponentScan")))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void aggregatorWithApiAndImplementationFromProjectWithUsedJarDependency() throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withAggregator(
            new GradleDependency().setConfiguration("permitTestAggregatorUse").setProject("spring"))
        .withSubProject(
            subProject("spring")
                .withPlugin("java-library")
                .withMavenRepositories()
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("api")
                        .setId("org.apache.commons:commons-collections4:4.4"))
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("implementation")
                        .setId("org.springframework:spring-context:5.2.11.RELEASE")))
        .withSubProject(
            subProject("tests")
                .withMavenRepositories()
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("testImplementation")
                        .setProject("spring"))
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("testImplementation")
                        .setId("org.springframework:spring-aop:5.2.11.RELEASE"))
                .withTestClass(
                    new GroovyClass("Main")
                        .usesClass("org.springframework.aop.Advisor")
                        .usesClass("org.apache.commons.collections4.BidiMap")))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void aggregatorWithImplementationDependencyAndAdditionalOverlappingApiDependency()
      throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withPlugin("java-library")
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("api")
                .setId("org.springframework:spring-web:5.2.11.RELEASE"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("org.springframework.beans.factory.annotation.Autowired")
                .usesClass("org.springframework.web.context.request.RequestContextHolder"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void
      aggregatorWithImplementationDependencyAndAdditionalOverlappingApiDependencyWhenVersionsManagedByPlatform()
          throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withSubProject(
            platformProject("platform")
                .withDependency(
                    new GradleDependency()
                        .setConfiguration("api")
                        .setReference(
                            "enforcedPlatform(\"org.springframework.boot:spring-boot-dependencies:2.3.6.RELEASE\")")))
        .withPlugin("java-library")
        .withAggregator(
            new GradleDependency()
                .setConfiguration("permitAggregatorUse")
                .setId("org.springframework.boot:spring-boot-starter-web"))
        .withDependency(
            new GradleDependency().setConfiguration("api").setId("org.springframework:spring-web"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter-web"))
        .withMainClass(
            new GroovyClass("Main")
                .usesClass("org.springframework.beans.factory.annotation.Autowired")
                .usesClass("org.springframework.web.context.request.RequestContextHolder"))
        .applyPlatformConfiguration()
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }
}

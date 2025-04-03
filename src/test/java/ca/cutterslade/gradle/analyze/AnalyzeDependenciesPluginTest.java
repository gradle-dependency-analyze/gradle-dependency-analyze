package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnalyzeDependenciesPluginTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void simpleBuildWithoutDependenciesResultsInSuccess() throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @ParameterizedTest
  @MethodSource("provideDependencyConfigurationParameters")
  void usedMainDependencyDeclaredWithConfigurationResultsInExpectedResult(
      final String configuration, final String expectedResult) throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withDependency(
            new GradleDependency().setConfiguration(configuration).setProject("dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult);

    // Then
    assertBuildResult(result, expectedResult);
  }

  private static Stream<Arguments> provideDependencyConfigurationParameters() {
    return Stream.of(
        Arguments.of("implementation", SUCCESS), Arguments.of("runtimeOnly", BUILD_FAILURE));
  }

  @ParameterizedTest
  @MethodSource("provideTransientMainDependencyParameters")
  void usedTransientMainDependencyDeclaredWithConfigurationResultsInExpectedResult(
      final String configuration,
      final String expectedResult,
      final String[] usedUndeclaredArtifacts,
      final String[] unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("Transient"))
        .withSubProject(
            subProject("dependent")
                .withPlugin("java-library")
                .withDependency(
                    new GradleDependency().setConfiguration("api").setProject("transient")))
        .withSubProject(subProject("transient").withMainClass(new GroovyClass("Transient")))
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

  private static Stream<Arguments> provideTransientMainDependencyParameters() {
    return Stream.of(
        Arguments.of(
            "implementation",
            VIOLATIONS,
            new String[] {"project :transient"},
            new String[] {"project :dependent"}),
        Arguments.of("runtimeOnly", BUILD_FAILURE, new String[] {}, new String[] {}));
  }

  @ParameterizedTest
  @MethodSource("provideUnusedMainDependencyParameters")
  void unusedMainDependencyDeclaredWithConfigurationResultsInExpectedResult(
      final String configuration,
      final String expectedResult,
      final String[] usedUndeclaredArtifacts,
      final String[] unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main"))
        .withSubProject(subProject("independent").withMainClass(new GroovyClass("Independent")))
        .withDependency(
            new GradleDependency().setConfiguration(configuration).setProject("independent"))
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

  private static Stream<Arguments> provideUnusedMainDependencyParameters() {
    return Stream.of(
        Arguments.of(
            "implementation", VIOLATIONS, new String[] {}, new String[] {"project :independent"}),
        Arguments.of("runtimeOnly", SUCCESS, new String[] {}, new String[] {}));
  }

  @ParameterizedTest
  @MethodSource("provideUsedTestDependencyParameters")
  void usedTestDependencyDeclaredWithConfigurationResultsInExpectedResult(
      final String configuration, final String expectedResult) throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("Test").usesClass("Dependent"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withDependency(
            new GradleDependency().setConfiguration(configuration).setProject("dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult);

    // Then
    assertBuildResult(result, expectedResult);
  }

  private static Stream<Arguments> provideUsedTestDependencyParameters() {
    return Stream.of(
        Arguments.of("testImplementation", SUCCESS),
        Arguments.of("testRuntimeOnly", TEST_BUILD_FAILURE));
  }

  @ParameterizedTest
  @MethodSource("provideUnusedTestDependencyParameters")
  void unusedTestDependencyDeclaredWithConfigurationResultsInExpectedResult(
      final String configuration,
      final String expectedResult,
      final String[] usedUndeclaredArtifacts,
      final String[] unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("Test"))
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

  private static Stream<Arguments> provideUnusedTestDependencyParameters() {
    return Stream.of(
        Arguments.of(
            "testImplementation", VIOLATIONS, new String[] {}, new String[] {"project :dependent"}),
        Arguments.of("testRuntimeOnly", SUCCESS, new String[] {}, new String[] {}));
  }

  @ParameterizedTest
  @MethodSource("provideTransientTestDependencyParameters")
  void usedTransientTestDependencyDeclaredWithConfigurationResultsInExpectedResult(
      final String configuration,
      final String expectedResult,
      final String[] usedUndeclaredArtifacts,
      final String[] unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("Test").usesClass("Transient"))
        .withSubProject(
            subProject("dependent")
                .withPlugin("java-library")
                .withMainClass(new GroovyClass("Dependent"))
                .withDependency(
                    new GradleDependency().setConfiguration("api").setProject("transient")))
        .withSubProject(subProject("transient").withMainClass(new GroovyClass("Transient")))
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

  private static Stream<Arguments> provideTransientTestDependencyParameters() {
    return Stream.of(
        Arguments.of(
            "testImplementation",
            VIOLATIONS,
            new String[] {"project :transient"},
            new String[] {"project :dependent"}),
        Arguments.of("testRuntimeOnly", TEST_BUILD_FAILURE, new String[] {}, new String[] {}));
  }
}

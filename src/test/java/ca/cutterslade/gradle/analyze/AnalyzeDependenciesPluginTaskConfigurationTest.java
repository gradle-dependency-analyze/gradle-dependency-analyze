package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnalyzeDependenciesPluginTaskConfigurationTest extends AnalyzeDependenciesPluginBaseTest {

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  void testProjectDependencyConfiguration(
      final boolean warnUsedUndeclared,
      final boolean warnUnusedDeclared,
      final String expectedResult,
      final String[] usedUndeclaredArtifacts,
      final String[] unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("Dependent2"))
        .withSubProject(
            subProject("dependent1")
                .withPlugin("java-library")
                .withDependency(
                    new GradleDependency().setConfiguration("api").setProject("dependent2")))
        .withSubProject(subProject("dependent2").withMainClass(new GroovyClass("Dependent2")))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("dependent1"))
        .withIgnoreUsedUndeclared(false)  // Explicitly check for used undeclared violations
        .withWarnUsedUndeclared(warnUsedUndeclared)
        .withWarnUnusedDeclared(warnUnusedDeclared)
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

  private static Stream<Arguments> provideTestParameters() {
    return Stream.of(
        Arguments.of(
            false,
            false,
            VIOLATIONS,
            new String[] {"project :dependent2"},
            new String[] {"project :dependent1"}),
        Arguments.of(
            false, true, VIOLATIONS, new String[] {"project :dependent2"}, new String[] {}),
        Arguments.of(
            true, false, VIOLATIONS, new String[] {}, new String[] {"project :dependent1"}),
        Arguments.of(
            true,
            true,
            SUCCESS,
            new String[] {"project :dependent2"},
            new String[] {"project :dependent1"}));
  }
}

package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginIgnoreUsedUndeclaredTest
    extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void testIgnoreUsedUndeclaredDoesNotWarn() throws IOException {
    // Setup: Create a project with a used undeclared dependency
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
        .withIgnoreUsedUndeclared(true)
        .create(projectDir);

    // When: Build the project
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then: Build should succeed and there should be no warnings about usedUndeclared
    assertBuildSuccess(result);
    // The output should not contain "usedUndeclaredArtifacts"
    org.assertj.core.api.Assertions.assertThat(result.getOutput())
        .doesNotContain("usedUndeclaredArtifacts");
  }

  @Test
  void testIgnoreUsedUndeclaredWithUnusedDeclared() throws IOException {
    // Setup: Create a project with both used undeclared and unused declared dependencies
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
        .withIgnoreUsedUndeclared(true)
        .create(projectDir);

    // When: Build the project
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // Then: Build should fail due to unusedDeclared but not mention usedUndeclared
    assertBuildResult(
        result,
        VIOLATIONS,
        java.util.Collections.emptyList(), // usedUndeclared should be ignored
        java.util.Collections.singletonList("project :dependent1"));
  }
}

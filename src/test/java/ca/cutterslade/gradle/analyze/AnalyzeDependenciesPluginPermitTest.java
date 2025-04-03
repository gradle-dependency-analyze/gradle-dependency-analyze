package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginPermitTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void projectWithUnusedDependencyButPermitted() throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withSubProject(subProject("independent").withMainClass(new GroovyClass("Independent")))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("dependent"))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("independent"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("permitUnusedDeclared")
                .setProject("independent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void projectWithUsedDependencyButPermitted() throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withSubProject(
            subProject("independent")
                .withPlugin("java-library")
                .withMainClass(new GroovyClass("Independent"))
                .withDependency(
                    new GradleDependency().setConfiguration("api").setProject("dependent")))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("independent"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("permitUsedUndeclared")
                .setProject("independent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }
}

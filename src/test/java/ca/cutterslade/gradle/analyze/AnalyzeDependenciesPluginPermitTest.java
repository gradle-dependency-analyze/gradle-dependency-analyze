package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginPermitTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void projectWithUnusedDependencyButPermitted() {
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
    BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void projectWithUsedDependencyButPermitted() {
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
    BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }
}

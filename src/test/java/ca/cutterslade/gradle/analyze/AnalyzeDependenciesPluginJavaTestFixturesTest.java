package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import java.util.Collections;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginJavaTestFixturesTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void projectOnlyWithTestFixtureClassesResultsInSuccess() throws IOException {
    // Setup
    rootProject()
        .withPlugin("java-test-fixtures")
        .withTestFixturesClass(new GroovyClass("MainTestFixture"))
        .withGradleDependency("testFixturesImplementation")
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void projectWithMainClassAndTestFixtureClassesResultsInSuccess() throws IOException {
    // Setup
    rootProject()
        .withPlugin("java-test-fixtures")
        .withMainClass(new GroovyClass("Main"))
        .withTestFixturesClass(new GroovyClass("MainTestFixture").usesClass("Main"))
        .withGradleDependency("testFixturesImplementation")
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void projectWithTestFixtureClassUsingMainClassFromDifferentModuleResultsInSuccess()
      throws IOException {
    // Setup
    rootProject()
        .withPlugin("java-test-fixtures")
        .withTestFixturesClass(new GroovyClass("MainTestFixture").usesClass("DependentMain"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("DependentMain")))
        .withDependency(
            new GradleDependency()
                .setConfiguration("testFixturesImplementation")
                .setProject("dependent"))
        .withGradleDependency("testFixturesImplementation")
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }

  @Test
  void projectWithTestFixtureClassAndMainClassFromDifferentModuleNotUsedResultsInFailure()
      throws IOException {
    // Setup
    rootProject()
        .withPlugin("java-test-fixtures")
        .withTestFixturesClass(new GroovyClass("MainTestFixture"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("DependentMain")))
        .withDependency(
            new GradleDependency()
                .setConfiguration("testFixturesImplementation")
                .setProject("dependent"))
        .withGradleDependency("testFixturesImplementation")
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(BUILD_FAILURE);

    // Then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.singletonList("project :dependent"));
  }

  @Test
  void projectWithTestTestFixtureAndMainClassAndMainClassFromDifferentModuleIsUsedResultsInSuccess()
      throws IOException {
    // Setup
    rootProject()
        .withPlugin("java-test-fixtures")
        .withMainClass(new GroovyClass("Main").usesClass("DependentMain"))
        .withTestClass(new GroovyClass("Test").usesClass("DependentMain"))
        .withTestFixturesClass(new GroovyClass("MainTestFixture"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("DependentMain")))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("dependent"))
        .withGradleDependency("testFixturesImplementation")
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(SUCCESS);

    // Then
    assertBuildSuccess(result);
  }
}

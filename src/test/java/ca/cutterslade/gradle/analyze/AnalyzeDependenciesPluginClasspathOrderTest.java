package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import java.util.Collections;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginClasspathOrderTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void projectWithTwoDependenciesOrderedFirstSecond() throws IOException {
    // setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("One").usesClass("Two"))
        .withSubProject(subProject("first").withMainClass(new GroovyClass("One")))
        .withSubProject(
            subProject("second")
                .withMainClass(new GroovyClass("One"))
                .withMainClass(new GroovyClass("Two")))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("first"))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("second"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildResult(result, SUCCESS);
  }

  @Test
  void projectWithTwoDependenciesOrderedSecondFirst() throws IOException {
    // setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("One").usesClass("Two"))
        .withSubProject(subProject("first").withMainClass(new GroovyClass("One")))
        .withSubProject(
            subProject("second")
                .withMainClass(new GroovyClass("One"))
                .withMainClass(new GroovyClass("Two")))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("second"))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("first"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // then
    assertBuildResult(
        result, VIOLATIONS, Collections.emptyList(), Collections.singletonList("project :first"));
  }
}

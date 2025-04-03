package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import java.util.Collections;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginNarDependencyTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void projectWithNarDependency() throws IOException {
    // setup
    rootProject()
        .withMainClass(new GroovyClass("Main"))
        .withMavenRepositories()
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.apache.bookkeeper:bookkeeper-common:4.14.4"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.singletonList("org.apache.bookkeeper:bookkeeper-common:4.14.4"));
  }
}

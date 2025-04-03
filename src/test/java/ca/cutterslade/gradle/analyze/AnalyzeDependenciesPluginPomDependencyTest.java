package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginPomDependencyTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  @DisplayName("dependency on a POM artifact containing dependencies should not fail")
  void pomDependency() throws IOException {
    // setup
    rootProject()
        .withMavenRepositories()
        .withMainClass(new GroovyClass("Foo").usesClass("javax.money.Monetary"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.javamoney:moneta:1.4.4"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildResult(result, SUCCESS);
  }
}

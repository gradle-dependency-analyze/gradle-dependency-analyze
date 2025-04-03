package ca.cutterslade.gradle.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginFileLoggingTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void simpleBuildWithoutDependenciesResultsInSuccess() throws IOException {
    // setup
    rootProject()
        .logDependencyInformationToFiles()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildSuccess(result);
  }

  @Test
  void simpleBuildWithUnusedDependenciesResultsInViolation()
      throws URISyntaxException, IOException {
    // setup
    rootProject()
        .logDependencyInformationToFiles()
        .withMavenRepositories()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(VIOLATIONS);

    // then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.singletonList("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"));

    assertThat(
            projectDir
                .resolve("build/reports/dependency-analyze/analyzeClassesDependencies.log")
                .toFile())
        .hasSameTextualContentAs(
            new File(getClass().getResource("/analyzeClassesDependencies.log").toURI()));
  }

  @Test
  void simpleBuildWithUnusedDependenciesResultsInSuccessWhenWarnIsTrue()
      throws URISyntaxException, IOException {
    // setup
    rootProject()
        .logDependencyInformationToFiles()
        .withMavenRepositories()
        .withWarnUnusedDeclared(true)
        .withWarnUsedUndeclared(true)
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildSuccess(result);

    assertThat(
            projectDir
                .resolve("build/reports/dependency-analyze/analyzeClassesDependencies.log")
                .toFile())
        .hasSameTextualContentAs(
            new File(getClass().getResource("/analyzeClassesDependencies.log").toURI()));
  }

  @Test
  void buildWithDependencyDeclaredInConfigAndUsedInBuild() throws URISyntaxException, IOException {
    // setup
    rootProject()
        .withMavenRepositories()
        .logDependencyInformationToFiles()
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
            new GradleDependency().setConfiguration("implementation").setProject("dependent"))
        .create(projectDir);

    // when
    final BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildSuccess(result);

    assertThat(
            projectDir
                .resolve("build/reports/dependency-analyze/analyzeClassesDependencies.log")
                .toFile())
        .hasSameTextualContentAs(
            new File(getClass().getResource("/complex_analyzeDependencies.log").toURI()));
  }
}

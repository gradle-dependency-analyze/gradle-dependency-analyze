package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.util.Collections;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnalyzeDependenciesPluginCompileOnlyTest extends AnalyzeDependenciesPluginBaseTest {

  @ParameterizedTest
  @CsvSource({"true, org.projectlombok:lombok:1.18.22", "false, "})
  void buildWithCompileOnlyDependencyNotNeededInRuntimeAndWarnCompileOnly(
      boolean warnCompileOnly, String compileOnlyArtifact) {
    // setup
    rootProject()
        .withMavenRepositories()
        .withWarnCompileOnly(warnCompileOnly)
        .withMainClass(
            new GroovyClass("Foo")
                .withClassAnnotation("lombok.Data", "")
                .usesClass("java.lang.Integer"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("compileOnly")
                .setId("org.projectlombok:lombok:1.18.22"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("annotationProcessor")
                .setId("org.projectlombok:lombok:1.18.22"))
        .create(projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    List<String> compileOnlyArtifacts =
        compileOnlyArtifact == null || compileOnlyArtifact.isEmpty()
            ? Collections.emptyList()
            : Collections.singletonList(compileOnlyArtifact);

    assertBuildResult(
        result,
        SUCCESS,
        Collections.emptyList(),
        Collections.emptyList(),
        compileOnlyArtifacts,
        Collections.emptyList());
  }

  @ParameterizedTest
  @CsvSource({
    "true, violations, org.projectlombok:lombok:1.18.22, org.projectlombok:lombok:1.18.22",
    "false, success, , "
  })
  void buildWithCompileOnlyDependencyNeededAlsoInRuntimeAndWarnCompileOnly(
      boolean warnCompileOnly,
      String buildResult,
      String usedUndeclaredArtifact,
      String compileOnlyArtifact) {
    // setup
    rootProject()
        .withMavenRepositories()
        .withWarnCompileOnly(warnCompileOnly)
        .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("compileOnly")
                .setId("org.projectlombok:lombok:1.18.22"))
        .create(projectDir);

    // when
    BuildResult result = buildGradleProject(buildResult.equals("success") ? SUCCESS : VIOLATIONS);

    // then
    List<String> usedUndeclaredArtifacts =
        usedUndeclaredArtifact == null || usedUndeclaredArtifact.isEmpty()
            ? Collections.emptyList()
            : Collections.singletonList(usedUndeclaredArtifact);

    List<String> compileOnlyArtifacts =
        compileOnlyArtifact == null || compileOnlyArtifact.isEmpty()
            ? Collections.emptyList()
            : Collections.singletonList(compileOnlyArtifact);

    assertBuildResult(
        result,
        buildResult.equals("success") ? SUCCESS : VIOLATIONS,
        usedUndeclaredArtifacts,
        Collections.emptyList(),
        compileOnlyArtifacts,
        Collections.emptyList());
  }

  @ParameterizedTest
  @CsvSource({
    "true, violations, org.projectlombok:lombok:1.18.22, org.projectlombok:lombok:1.18.22",
    "false, success, , "
  })
  void buildWithCompileOnlyDependencyNeededAlsoInRuntimeAndCompileOnlyAndWarnCompileOnly(
      boolean warnCompileOnly,
      String buildResult,
      String usedUndeclaredArtifact,
      String compileOnlyArtifact) {
    // setup
    rootProject()
        .withMavenRepositories()
        .withWarnCompileOnly(warnCompileOnly)
        .withMainClass(
            new GroovyClass("Foo")
                .usesClass("lombok.NonNull")
                .withClassAnnotation("lombok.Data", ""))
        .withDependency(
            new GradleDependency()
                .setConfiguration("compileOnly")
                .setId("org.projectlombok:lombok:1.18.22"))
        .create(projectDir);

    // when
    BuildResult result = buildGradleProject(buildResult.equals("success") ? SUCCESS : VIOLATIONS);

    // then
    List<String> usedUndeclaredArtifacts =
        usedUndeclaredArtifact == null || usedUndeclaredArtifact.isEmpty()
            ? Collections.emptyList()
            : Collections.singletonList(usedUndeclaredArtifact);

    List<String> compileOnlyArtifacts =
        compileOnlyArtifact == null || compileOnlyArtifact.isEmpty()
            ? Collections.emptyList()
            : Collections.singletonList(compileOnlyArtifact);

    assertBuildResult(
        result,
        buildResult.equals("success") ? SUCCESS : VIOLATIONS,
        usedUndeclaredArtifacts,
        Collections.emptyList(),
        compileOnlyArtifacts,
        Collections.emptyList());
  }

  @ParameterizedTest
  @CsvSource({"true, ", "false, "})
  void buildWithCompileOnlyDependencyNeededAlsoInImplementationAndCompileOnlyAndWarnCompileOnly(
      boolean warnCompileOnly, String compileOnlyArtifact) {
    // setup
    rootProject()
        .withMavenRepositories()
        .withWarnCompileOnly(warnCompileOnly)
        .withMainClass(
            new GroovyClass("Foo")
                .usesClass("lombok.NonNull")
                .withClassAnnotation("lombok.Data", ""))
        .withDependency(
            new GradleDependency()
                .setConfiguration("implementation")
                .setId("org.projectlombok:lombok:1.18.22"))
        .create(projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    List<String> compileOnlyArtifacts =
        compileOnlyArtifact == null || compileOnlyArtifact.isEmpty()
            ? Collections.emptyList()
            : Collections.singletonList(compileOnlyArtifact);

    assertBuildResult(
        result,
        SUCCESS,
        Collections.emptyList(),
        Collections.emptyList(),
        compileOnlyArtifacts,
        Collections.emptyList());
  }

  @ParameterizedTest
  @CsvSource({"true, org.projectlombok:lombok:1.18.22", "false, "})
  void buildWithCompileOnlyDependencyNeededInRuntimeAndCompileOnlyAndWarnCompileOnly(
      boolean warnCompileOnly, String compileOnlyArtifact) {
    // setup
    rootProject()
        .withMavenRepositories()
        .withWarnCompileOnly(warnCompileOnly)
        .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("compileOnly")
                .setId("org.projectlombok:lombok:1.18.22"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("permitUsedUndeclared")
                .setId("org.projectlombok:lombok:1.18.22"))
        .create(projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    List<String> compileOnlyArtifacts =
        compileOnlyArtifact == null || compileOnlyArtifact.isEmpty()
            ? Collections.emptyList()
            : Collections.singletonList(compileOnlyArtifact);

    assertBuildResult(
        result,
        SUCCESS,
        Collections.emptyList(),
        Collections.emptyList(),
        compileOnlyArtifacts,
        Collections.emptyList());
  }
}

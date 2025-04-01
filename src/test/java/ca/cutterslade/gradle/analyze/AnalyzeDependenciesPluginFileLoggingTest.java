package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginFileLoggingTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void simpleBuildWithoutDependenciesResultsInSuccess() {
    // setup
    rootProject()
        .logDependencyInformationToFiles()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
        .create(projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildSuccess(result);
  }

  @Test
  void simpleBuildWithUnusedDependenciesResultsInViolation() throws IOException {
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
    BuildResult result = buildGradleProject(VIOLATIONS);
    FileContent files =
        getFileContent("analyzeClassesDependencies.log", "analyzeClassesDependencies.log");

    // then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.singletonList("org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"));

    if (!Objects.equals(files.v1, files.v2)) {
      System.out.println(
          "Files differ:" + System.lineSeparator() + generateDiff(files.v1, files.v2));
    }

    Assertions.assertEquals(files.v1, files.v2);
  }

  @Test
  void simpleBuildWithUnusedDependenciesResultsInSuccessWhenWarnIsTrue() throws IOException {
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
    BuildResult result = buildGradleProject(SUCCESS);
    FileContent files =
        getFileContent("analyzeClassesDependencies.log", "analyzeClassesDependencies.log");

    // then
    assertBuildSuccess(result);
    if (!Objects.equals(files.v1, files.v2)) {
      System.out.println(
          "Files differ:" + System.lineSeparator() + generateDiff(files.v1, files.v2));
    }

    Assertions.assertEquals(files.v1, files.v2);
  }

  @Test
  void buildWithDependencyDeclaredInConfigAndUsedInBuild() throws IOException {
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
    BuildResult result = buildGradleProject(SUCCESS);
    FileContent files =
        getFileContent("complex_analyzeDependencies.log", "analyzeClassesDependencies.log");

    // then
    assertBuildSuccess(result);
    if (!Objects.equals(files.v1, files.v2)) {
      System.out.println(
          "Files differ:" + System.lineSeparator() + generateDiff(files.v1, files.v2));
    }

    Assertions.assertEquals(files.v1, files.v2);
  }

  private FileContent getFileContent(String fileWithExpectedContent, String fileName)
      throws IOException {
    // Read actual file
    StringBuilder actualContent = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(
            new FileReader(new File(projectDir, "build/reports/dependency-analyze/" + fileName)))) {
      String line;
      while ((line = reader.readLine()) != null) {
        actualContent.append(line).append(System.lineSeparator());
      }
    }
    String actual = actualContent.toString();

    // Read expected file from resources
    StringBuilder expectedContent = new StringBuilder();
    try (InputStream inputStream = getClass().getResourceAsStream("/" + fileWithExpectedContent);
        BufferedReader expectedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = expectedReader.readLine()) != null) {
        expectedContent.append(line).append(System.lineSeparator());
      }
    }
    String expected = expectedContent.toString().replaceAll("\\r\\n", System.lineSeparator());

    return new FileContent(actual, expected);
  }

  private String generateDiff(String content1, String content2) {
    List<String> lines1 = readLines(content1);
    List<String> lines2 = readLines(content2);

    com.github.difflib.patch.Patch<String> diff = DiffUtils.diff(lines1, lines2);

    return diff.getDeltas().stream()
        .map(AbstractDelta::toString)
        .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
  }

  private List<String> readLines(String content) {
    return java.util.Arrays.stream(content.split("\\r?\\n"))
        .collect(java.util.stream.Collectors.toList());
  }

  private static class FileContent {
    final String v1;
    final String v2;

    FileContent(String v1, String v2) {
      this.v1 = v1;
      this.v2 = v2;
    }
  }
}

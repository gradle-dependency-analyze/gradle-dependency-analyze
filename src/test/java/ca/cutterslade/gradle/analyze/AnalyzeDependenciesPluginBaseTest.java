package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleProject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

public abstract class AnalyzeDependenciesPluginBaseTest {
  protected static final String SUCCESS = "success";
  protected static final String BUILD_FAILURE = "build failure";
  protected static final String TEST_BUILD_FAILURE = "test build failure";
  protected static final String VIOLATIONS = "violations";
  protected static final String WARNING = "warning";

  @TempDir public File projectDir;

  private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

  protected static GradleProject rootProject() {
    return new GradleProject("project", true)
        .withPlugin("groovy")
        .withPlugin("ca.cutterslade.analyze")
        .withGradleDependency("implementation");
  }

  protected static GradleProject platformProject(String name) {
    return new GradleProject(name).withPlugin("java-platform");
  }

  protected static GradleProject subProject(String name) {
    return new GradleProject(name).withPlugin("groovy").withGradleDependency("implementation");
  }

  private GradleRunner gradleProject(boolean withCodeCoverage) {
    if (withCodeCoverage) {
      try (FileOutputStream outputStream =
              new FileOutputStream(new File(projectDir, "gradle.properties"));
          InputStream inputStream =
              getClass().getClassLoader().getResourceAsStream("testkit-gradle.properties")) {
        if (inputStream != null) {
          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException("Failed to set up gradle.properties", e);
      }
    }

    GradleRunner runner =
        GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().forwardOutput();

    if (runtimeMXBean.getInputArguments().stream()
        .anyMatch(arg -> arg.startsWith("-agentlib:jdwp="))) {
      runner.withDebug(true);
    }
    return runner;
  }

  protected static void assertBuildSuccess(BuildResult result) {
    BuildTask task = result.task(":build");
    if (task == null) {
      throw new AssertionFailedError(
          "Build task not run: " + System.lineSeparator() + result.getOutput());
    }
    Assertions.assertEquals(TaskOutcome.SUCCESS, task.getOutcome());
  }

  protected final BuildResult buildGradleProject(String expectedResult) {
    return buildGradleProject(expectedResult, null, true);
  }

  protected final BuildResult buildGradleProject(
      String expectedResult, GradleVersion gradleVersion, boolean withCodeCoverage) {
    GradleRunner project = gradleProject(withCodeCoverage);
    if (gradleVersion != null) {
      project.withGradleVersion(gradleVersion.getVersion());
    }
    if (expectedResult.equals(SUCCESS)) {
      return project.build();
    }
    return project.buildAndFail();
  }

  protected static void assertBuildResult(BuildResult result, String expectedResult) {
    assertBuildResult(
        result,
        expectedResult,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  protected static void assertBuildResult(
      BuildResult result,
      String expectedResult,
      List<String> usedUndeclaredArtifacts,
      List<String> unusedDeclaredArtifacts) {
    assertBuildResult(
        result,
        expectedResult,
        usedUndeclaredArtifacts,
        unusedDeclaredArtifacts,
        Collections.emptyList(),
        Collections.emptyList());
  }

  protected static void assertBuildResult(
      BuildResult result,
      String expectedResult,
      List<String> usedUndeclaredArtifacts,
      List<String> unusedDeclaredArtifacts,
      List<String> compileOnlyArtifacts,
      List<String> superfluousDeclaredArtifacts) {
    switch (expectedResult) {
      case SUCCESS:
        {
          StringBuilder violations = new StringBuilder();
          if (!compileOnlyArtifacts.isEmpty()) {
            String spacer = "";
            violations
                .append(spacer)
                .append("compileOnlyDeclaredArtifacts")
                .append(System.lineSeparator());
            compileOnlyArtifacts.forEach(
                artifact ->
                    violations
                        .append(spacer)
                        .append(" - ")
                        .append(artifact)
                        .append(System.lineSeparator()));
            Assertions.assertTrue(
                result.getOutput().contains(violations),
                "Expected output to contain: " + violations);
          }
          Assertions.assertTrue(
              result.getTasks().stream().anyMatch(task -> task.getOutcome() != TaskOutcome.SUCCESS),
              "Expected at least one task with outcome different than SUCCESS");
          break;
        }
      case BUILD_FAILURE:
      case TEST_BUILD_FAILURE:
        Assertions.assertTrue(
            result.getTasks().stream().anyMatch(task -> task.getOutcome() == TaskOutcome.FAILED),
            "Expected at least one failed task");
        break;
      case VIOLATIONS:
      case WARNING:
        {
          StringBuilder violations =
              expectedResult.equals(WARNING) ? new StringBuilder() : new StringBuilder("> ");
          violations.append("Dependency analysis found issues.").append(System.lineSeparator());
          String spacer = expectedResult.equals(WARNING) ? "" : "  ";
          if (!usedUndeclaredArtifacts.isEmpty()) {
            violations
                .append(spacer)
                .append("usedUndeclaredArtifacts")
                .append(System.lineSeparator());
            usedUndeclaredArtifacts.forEach(
                artifact ->
                    violations
                        .append(spacer)
                        .append(" - ")
                        .append(artifact)
                        .append(System.lineSeparator()));
          }
          if (!unusedDeclaredArtifacts.isEmpty()) {
            violations
                .append(spacer)
                .append("unusedDeclaredArtifacts")
                .append(System.lineSeparator());
            unusedDeclaredArtifacts.forEach(
                artifact ->
                    violations
                        .append(spacer)
                        .append(" - ")
                        .append(artifact)
                        .append(System.lineSeparator()));
          }
          if (!compileOnlyArtifacts.isEmpty()) {
            violations
                .append(spacer)
                .append("compileOnlyDeclaredArtifacts")
                .append(System.lineSeparator());
            compileOnlyArtifacts.forEach(
                artifact ->
                    violations
                        .append(spacer)
                        .append(" - ")
                        .append(artifact)
                        .append(System.lineSeparator()));
          }
          if (!superfluousDeclaredArtifacts.isEmpty()) {
            violations
                .append(spacer)
                .append("superfluousDeclaredArtifacts")
                .append(System.lineSeparator());
            superfluousDeclaredArtifacts.forEach(
                artifact ->
                    violations
                        .append(spacer)
                        .append(" - ")
                        .append(artifact)
                        .append(System.lineSeparator()));
          }
          violations.append(System.lineSeparator());
          Assertions.assertTrue(
              result.getOutput().contains(violations), "Expected output to contain: " + violations);
          break;
        }
      default:
        Assertions.assertTrue(
            result.getOutput().contains(expectedResult),
            "Expected output to contain: " + expectedResult);
        break;
    }
  }

  protected void copyProjectToTestFolder(String sourcePath, File destFolder)
      throws URISyntaxException, IOException {
    URL resourceUrl = this.getClass().getResource("/" + sourcePath);
    if (resourceUrl == null) {
      throw new IllegalStateException(
          "Resource folder '" + sourcePath + "' not found in classpath");
    }
    File sourceFolder = new File(resourceUrl.toURI());

    if (!sourceFolder.exists()) {
      throw new IllegalStateException("Source folder does not exist at " + sourceFolder);
    }

    // Make sure destination is a directory and not a file
    if (!destFolder.exists()) {
      destFolder.mkdirs();
    } else if (!destFolder.isDirectory()) {
      throw new IllegalArgumentException("Destination must be a directory");
    }

    FileUtils.copyDirectory(sourceFolder, destFolder);
  }
}

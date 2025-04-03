package ca.cutterslade.gradle.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ca.cutterslade.gradle.analyze.helper.GradleProject;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

public abstract class AnalyzeDependenciesPluginBaseTest {
  protected static final String SUCCESS = "success";
  protected static final String BUILD_FAILURE = "build failure";
  protected static final String TEST_BUILD_FAILURE = "test build failure";
  protected static final String VIOLATIONS = "violations";
  protected static final String WARNING = "warning";

  @TempDir public Path projectDir;

  private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

  protected static GradleProject rootProject() {
    return new GradleProject("project", true)
        .withPlugin("groovy")
        .withPlugin("ca.cutterslade.analyze")
        .withGradleDependency("implementation");
  }

  protected static GradleProject platformProject(final String name) {
    return new GradleProject(name).withPlugin("java-platform");
  }

  protected static GradleProject subProject(final String name) {
    return new GradleProject(name).withPlugin("groovy").withGradleDependency("implementation");
  }

  private GradleRunner gradleProject(final boolean withCodeCoverage) {
    if (withCodeCoverage) {
      try (final InputStream inputStream =
          getClass().getClassLoader().getResourceAsStream("testkit-gradle.properties")) {
        if (inputStream != null) {
          Files.copy(
              inputStream,
              projectDir.resolve("gradle.properties"),
              StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (final IOException e) {
        throw new RuntimeException("Failed to set up gradle.properties", e);
      }
    }

    final GradleRunner runner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .forwardOutput();

    if (runtimeMXBean.getInputArguments().stream()
        .anyMatch(arg -> arg.startsWith("-agentlib:jdwp="))) {
      runner.withDebug(true);
    }
    return runner;
  }

  protected static void assertBuildSuccess(final BuildResult result) {
    final BuildTask task = result.task(":build");
    if (task == null) {
      throw new AssertionFailedError(
          "Build task not run: " + System.lineSeparator() + result.getOutput());
    }
    assertThat(task.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

  protected final BuildResult buildGradleProject(final String expectedResult) {
    return buildGradleProject(expectedResult, null, true);
  }

  protected final BuildResult buildGradleProject(
      final String expectedResult,
      final GradleVersion gradleVersion,
      final boolean withCodeCoverage) {
    final GradleRunner project = gradleProject(withCodeCoverage);
    if (gradleVersion != null) {
      project.withGradleVersion(gradleVersion.getVersion());
    }
    if (expectedResult.equals(SUCCESS)) {
      return project.build();
    }
    return project.buildAndFail();
  }

  protected static void assertBuildResult(final BuildResult result, final String expectedResult) {
    assertBuildResult(
        result,
        expectedResult,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  protected static void assertBuildResult(
      final BuildResult result,
      final String expectedResult,
      final List<String> usedUndeclaredArtifacts,
      final List<String> unusedDeclaredArtifacts) {
    assertBuildResult(
        result,
        expectedResult,
        usedUndeclaredArtifacts,
        unusedDeclaredArtifacts,
        Collections.emptyList(),
        Collections.emptyList());
  }

  protected static void assertBuildResult(
      final BuildResult result,
      final String expectedResult,
      final List<String> usedUndeclaredArtifacts,
      final List<String> unusedDeclaredArtifacts,
      final List<String> compileOnlyArtifacts,
      final List<String> superfluousDeclaredArtifacts) {
    switch (expectedResult) {
      case SUCCESS:
        {
          final StringBuilder violations = new StringBuilder();
          if (!compileOnlyArtifacts.isEmpty()) {
            final String spacer = "";
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
            assertThat(result.getOutput()).contains(violations);
          }
          assertThat(result.getTasks()).anyMatch(task -> task.getOutcome() != TaskOutcome.SUCCESS);
          break;
        }
      case BUILD_FAILURE:
      case TEST_BUILD_FAILURE:
        assertThat(result.getTasks()).anyMatch(task -> task.getOutcome() == TaskOutcome.FAILED);
        break;
      case VIOLATIONS:
      case WARNING:
        {
          final StringBuilder violations =
              expectedResult.equals(WARNING) ? new StringBuilder() : new StringBuilder("> ");
          violations.append("Dependency analysis found issues.").append(System.lineSeparator());
          final String spacer = expectedResult.equals(WARNING) ? "" : "  ";
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
          assertThat(result.getOutput()).contains(violations);
          break;
        }
      default:
        assertThat(result.getOutput()).contains(expectedResult);
        break;
    }
  }

  protected void copyProjectToTestFolder(final String sourcePath, final Path destFolder)
      throws URISyntaxException, IOException {
    final URL resourceUrl = this.getClass().getResource("/" + sourcePath);
    if (resourceUrl == null) {
      throw new IllegalStateException(
          "Resource folder '" + sourcePath + "' not found in classpath");
    }

    final Path sourceFolder = new File(resourceUrl.toURI()).toPath();
    if (!Files.exists(sourceFolder)) {
      throw new IllegalStateException("Source folder does not exist at " + sourceFolder);
    }

    // Make sure destination is a directory and not a file
    if (Files.exists(destFolder) && !Files.isDirectory(destFolder)) {
      throw new IllegalArgumentException("Destination must not exist");
    }

    if (!isFolderEmpty(destFolder)) {
      throw new IllegalStateException("Destination folder exists but is not empty");
    }

    copyDirectory(sourceFolder, destFolder);
  }

  private static boolean isFolderEmpty(final Path path) throws IOException {
    if (Files.isDirectory(path)) {
      try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
        return !directoryStream.iterator().hasNext();
      }
    }
    return false; // Not a directory
  }

  private static void copyDirectory(final Path source, final Path target) throws IOException {
    // Create target directory if it doesn't exist
    Files.createDirectories(target);

    // Walk through the source directory
    Files.walkFileTree(
        source,
        EnumSet.of(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
              throws IOException {
            final Path targetDir = target.resolve(source.relativize(dir));
            Files.createDirectories(targetDir);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
              throws IOException {
            Files.copy(
                file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}

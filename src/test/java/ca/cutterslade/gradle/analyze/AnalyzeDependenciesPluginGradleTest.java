package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import ca.cutterslade.gradle.analyze.util.GradleVersionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnalyzeDependenciesPluginGradleTest extends AnalyzeDependenciesPluginBaseTest {

  private static final String maxVersion = "9.0";

  @ParameterizedTest
  @MethodSource("provideBuildWithoutDependenciesParameters")
  void simpleBuildWithoutDependenciesResultsInExpectedResult(
      final GradleVersion gradleVersion, final String expectedResult) throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult, gradleVersion, false);

    // Then
    assertBuildResult(result, expectedResult);
  }

  @ParameterizedTest
  @MethodSource("provideUsedMainDependencyParameters")
  void usedMainDependencyDeclaredResultsInExpectedResult(
      final GradleVersion gradleVersion, final String expectedResult) throws IOException {
    // Setup
    rootProject()
        .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
        .withSubProject(subProject("dependent").withMainClass(new GroovyClass("Dependent")))
        .withDependency(
            new GradleDependency().setConfiguration("implementation").setProject("dependent"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult, gradleVersion, false);

    // Then
    assertBuildResult(result, expectedResult);
  }

  @ParameterizedTest
  @MethodSource("provideTestFixtureClassesParameters")
  void projectOnlyWithTestFixtureClassesResultsInExpectedResult(
      final GradleVersion gradleVersion, final String expectedResult) throws IOException {
    // Setup
    rootProject()
        .withPlugin("java-test-fixtures")
        .withTestFixturesClass(new GroovyClass("MainTestFixture"))
        .withGradleDependency("testFixturesImplementation")
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult, gradleVersion, false);

    // Then
    assertBuildResult(result, expectedResult);
  }

  @ParameterizedTest
  @MethodSource("provideProvidedRuntimeParameters")
  void projectWithProvidedRuntimeResultsInExpectedResult(
      final GradleVersion gradleVersion,
      final String expectedResult,
      final List<String> unusedDeclaredArtifacts)
      throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withPlugin("war")
        .withMainClass(new GroovyClass("Main"))
        .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
        .withDependency(
            new GradleDependency()
                .setConfiguration("providedRuntime")
                .setId("org.springframework.boot:spring-boot-starter-tomcat:2.3.6.RELEASE"))
        .create(projectDir);

    // When
    final BuildResult result = buildGradleProject(expectedResult, gradleVersion, false);

    // Then
    assertBuildResult(result, expectedResult, Collections.emptyList(), unusedDeclaredArtifacts);
    final boolean isWarPluginBroken =
        GradleVersionUtil.isWarPluginBrokenWhenUsingProvidedRuntime(gradleVersion);
    assert result.getOutput().contains("https://github.com/gradle/gradle/issues/17415")
        == isWarPluginBroken;
  }

  @ParameterizedTest
  @MethodSource("provideAggregatorDependencyParameters")
  void aggregatorDependencyDeclaredInConfigAndUsedInBuildResultsInExpectedResult(
      final GradleVersion gradleVersion, final String expectedResult) throws IOException {
    // Setup
    rootProject()
        .withMavenRepositories()
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

    // When
    final BuildResult result = buildGradleProject(expectedResult, gradleVersion, false);

    // Then
    assertBuildResult(result, expectedResult);
  }

  private static Stream<Arguments> provideBuildWithoutDependenciesParameters() {
    return determineMinorVersions("5.3").stream().map(pair -> Arguments.of(pair.v1, pair.v2));
  }

  private static Stream<Arguments> provideUsedMainDependencyParameters() {
    return determineMinorVersions("5.3").stream().map(pair -> Arguments.of(pair.v1, pair.v2));
  }

  private static Stream<Arguments> provideTestFixtureClassesParameters() {
    return determineMinorVersions("5.6").stream().map(pair -> Arguments.of(pair.v1, pair.v2));
  }

  private static Stream<Arguments> provideProvidedRuntimeParameters() {
    return determineMinorVersions("6.9", "7.3").stream()
        .map(
            pair ->
                Arguments.of(
                    pair.v1,
                    GradleVersionUtil.isWarPluginBrokenWhenUsingProvidedRuntime(pair.v1)
                        ? VIOLATIONS
                        : SUCCESS,
                    GradleVersionUtil.isWarPluginBrokenWhenUsingProvidedRuntime(pair.v1)
                        ? Collections.singletonList(
                            "org.springframework.boot:spring-boot-starter-tomcat:2.3.6.RELEASE")
                        : Collections.emptyList()));
  }

  private static Stream<Arguments> provideAggregatorDependencyParameters() {
    return determineMinorVersions("5.3").stream().map(pair -> Arguments.of(pair.v1, pair.v2));
  }

  private static List<Tuple2<GradleVersion, String>> determineMinorVersions(
      final String minVersion) {
    return determineMinorVersions(minVersion, maxVersion);
  }

  private static List<Tuple2<GradleVersion, String>> determineMinorVersions(
      final String minVersion, final String maxVersion) {
    try {
      final URL serviceUrl = new URL("https://services.gradle.org/versions/all");
      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode versions = mapper.readValue(serviceUrl, JsonNode.class);
      final GradleVersion minGradleVersion = GradleVersion.version(minVersion);
      final GradleVersion maxGradleVersion = GradleVersion.version(maxVersion);

      return StreamSupport.stream(versions.spliterator(), false)
          .filter(node -> !node.get("broken").asBoolean())
          .map(node -> GradleVersion.version(node.get("version").asText()))
          .filter(
              version ->
                  version.compareTo(minGradleVersion) >= 0
                      && version.compareTo(maxGradleVersion) <= 0)
          .filter(version -> version.equals(version.getBaseVersion()))
          .sorted()
          .collect(
              Collectors.groupingBy(
                  version -> {
                    final String versionStr = version.toString();
                    final int idx = ordinalIndexOf(versionStr, 2);
                    return idx == -1 ? versionStr : versionStr.substring(0, idx);
                  }))
          .values()
          .stream()
          .map(
              versions1 ->
                  versions1.stream()
                      .max(Comparator.naturalOrder())
                      .orElseThrow(
                          () -> new RuntimeException("Could not determine the minimum version")))
          .map(version -> new Tuple2<>(version, AnalyzeDependenciesPluginBaseTest.SUCCESS))
          .collect(Collectors.toList());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Utility method to find the nth occurrence of a substring */
  private static int ordinalIndexOf(final String str, int n) {
    int pos = -1;
    while (true) {
      final int newPos = str.indexOf(".", pos + 1);
      if (newPos == -1) {
        pos = -1;
        break;
      } else {
        pos = newPos;
        if (--n <= 0) {
          break;
        }
      }
    }
    return pos;
  }

  private static class Tuple2<V1, V2> {
    private final V1 v1;
    private final V2 v2;

    public Tuple2(final V1 v1, final V2 v2) {
      this.v1 = v1;
      this.v2 = v2;
    }
  }
}

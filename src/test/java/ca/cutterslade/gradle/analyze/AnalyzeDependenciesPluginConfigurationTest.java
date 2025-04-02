package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.util.Arrays;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnalyzeDependenciesPluginConfigurationTest extends AnalyzeDependenciesPluginBaseTest {

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  void forceConfigurationResolutionTaskShouldNotCausePluginFailure(
      String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
    // Setup
    rootProject()
        .withMavenRepositories()
        .withMainClass(new GroovyClass("Main"))
        .withPlugin("java-library")
        .withDependency(
            new GradleDependency().setConfiguration("api").setId("com.google.guava:guava:30.1-jre"))
        .withAdditionalTask(
            "forceConfigurationResolution",
            "// Minimal example from our internal plugins that force resolution early\n"
                + "import org.gradle.api.artifacts.Configuration\n"
                + "\n"
                + "tasks.register(\"forceConfigurationResolution\") {\n"
                + "  doLast() {\n"
                + "    tasks.forEach { Task task ->\n"
                + "      task.metaPropertyValues.forEach { PropertyValue prop ->\n"
                + "        def value\n"
                + "        try {\n"
                + "          value = prop.value\n"
                + "        } catch (e) {\n"
                + "          // Log the failure, then skip over it\n"
                + "          project.logger.debug(\"Failed to resolve property \" + prop.name + \""
                + " on \" + task.name, e)\n"
                + "          return\n"
                + "        }\n"
                + "\n"
                + "        // If this blows up, it should throw, not get swallowed up\n"
                + "        if (value instanceof Configuration) {\n"
                + "          // The return value is not important, just calling it will make some"
                + " lazily initialized stuff happen\n"
                + "          value.resolvedConfiguration\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}")
        .create(projectDir);

    // When
    BuildResult result = buildGradleProject(expectedResult);

    // Then
    assertBuildResult(
        result,
        expectedResult,
        Arrays.asList(usedUndeclaredArtifacts),
        Arrays.asList(unusedDeclaredArtifacts));
  }

  private static Stream<Arguments> provideTestParameters() {
    return Stream.of(
        Arguments.of(
            VIOLATIONS, new String[] {}, new String[] {"com.google.guava:guava:30.1-jre"}));
  }
}

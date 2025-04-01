package ca.cutterslade.gradle.analyze.projects;

import ca.cutterslade.gradle.analyze.AnalyzeDependenciesPluginBaseTest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginProjectsTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void issue_527() throws URISyntaxException, IOException {
    // setup
    copyProjectToTestFolder("projects/issue_527", projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildResult(result, SUCCESS);
  }

  @Test
  void issue_400() throws URISyntaxException, IOException {
    // setup
    copyProjectToTestFolder("projects/issue_400", projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildResult(result, SUCCESS);
  }

  @Test
  void issue_288() throws URISyntaxException, IOException {
    // setup
    copyProjectToTestFolder("projects/issue_288", projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildResult(result, SUCCESS);
  }

  @Test
  void issue_528() throws URISyntaxException, IOException {
    // setup
    copyProjectToTestFolder("projects/issue_528", projectDir);

    // when
    BuildResult result = buildGradleProject(VIOLATIONS);

    // then
    assertBuildResult(
        result,
        VIOLATIONS,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.singletonList("jakarta.annotation:jakarta.annotation-api:2.1.1"));
  }
}

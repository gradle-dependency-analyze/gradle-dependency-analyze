package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.helper.GradleDependency;
import ca.cutterslade.gradle.analyze.helper.GroovyClass;
import java.util.Collections;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class AnalyzeDependenciesPluginConstantsTest extends AnalyzeDependenciesPluginBaseTest {

  @Test
  void buildWithConstantsUsageFromDependency() {
    // setup
    rootProject()
        .withAllProjectsPlugin("ca.cutterslade.analyze")
        .withSubProject(
            subProject("foo")
                .withWarnUnusedDeclared(true)
                .withMainClass(
                    new GroovyClass("FooClass")
                        .withClassAnnotation("FooAnnotation", "BarConstants.BAR_VALUE"))
                .withMainClass(new GroovyClass("FooAnnotation", true))
                .withDependency(
                    new GradleDependency().setConfiguration("implementation").setProject("bar")))
        .withSubProject(
            subProject("bar")
                .withMainClass(
                    new GroovyClass("BarConstants")
                        .addClassConstant("BAR_VALUE", "String", "\"dummy value\"")))
        .withSubProject(
            subProject("baz")
                .withMainClass(
                    new GroovyClass("BazClass")
                        .addClassConstant("BAZ_VALUE", "String", "BarConstants.BAR_VALUE", false))
                .withDependency(
                    new GradleDependency().setConfiguration("implementation").setProject("bar")))
        .create(projectDir);

    // when
    BuildResult result = buildGradleProject(SUCCESS);

    // then
    assertBuildResult(
        result, WARNING, Collections.emptyList(), Collections.singletonList("project :bar"));
  }
}

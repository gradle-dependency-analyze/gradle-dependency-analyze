package ca.cutterslade.gradle.analyze


import ca.cutterslade.gradle.analyze.helper.GradleProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification

abstract class AnalyzeDependenciesPluginBaseSpec extends Specification {
    protected static final String SUCCESS = 'success'
    protected static final String BUILD_FAILURE = 'build failure'
    protected static final String TEST_BUILD_FAILURE = 'test build failure'

    @Rule
    public TemporaryFolder projectDir

    protected static GradleProject rootProject() {
        new GradleProject('project', true)
                .withPlugin('ca.cutterslade.analyze')
                .withGradleDependency('implementation')
    }

    protected static GradleProject subProject(String name) {
        new GradleProject(name)
                .withGradleDependency('implementation')
    }

    protected GradleRunner gradleProject() {
        GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withPluginClasspath()
                .withArguments('build')
    }

    protected static void assertBuildSuccess(BuildResult result) {
        if (result.task(':build') == null) {
            throw new SpockAssertionError("Build task not run: \n${result.getOutput()}")
        }
        assert result.task(':build').getOutcome() == TaskOutcome.SUCCESS
    }

    protected BuildResult buildGradleProject(String expectedResult) {
        if (expectedResult == SUCCESS) {
            return gradleProject().build()
        }
        return gradleProject().buildAndFail()
    }

    protected static void assertBuildResult(BuildResult result, String expectedResult) {
        if (expectedResult == SUCCESS) {
            if (result.task(':build') == null) {
                throw new SpockAssertionError("Build task not run: \n${result.getOutput()}")
            }
            assert result.task(':build').getOutcome() == TaskOutcome.SUCCESS
        } else if (expectedResult == BUILD_FAILURE) {
            if (result.task(':compileGroovy') == null) {
                throw new SpockAssertionError("compileGroovy task not run: \n${result.getOutput()}")
            }
            assert result.task(':compileGroovy').getOutcome() == TaskOutcome.FAILED
        } else if (expectedResult == TEST_BUILD_FAILURE) {
            if (result.task(':compileTestGroovy') == null) {
                throw new SpockAssertionError("compileTestGroovy task not run: \n${result.getOutput()}")
            }
            assert result.task(':compileTestGroovy').getOutcome() == TaskOutcome.FAILED
        } else {
            assert result.output.contains(expectedResult)
        }
    }
}

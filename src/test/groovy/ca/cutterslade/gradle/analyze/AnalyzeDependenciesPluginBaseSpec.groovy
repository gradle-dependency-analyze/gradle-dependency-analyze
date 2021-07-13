package ca.cutterslade.gradle.analyze


import ca.cutterslade.gradle.analyze.helper.GradleProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification
import spock.lang.TempDir

abstract class AnalyzeDependenciesPluginBaseSpec extends Specification {
    protected static final def SUCCESS = 'success'
    protected static final def BUILD_FAILURE = 'build failure'
    protected static final def TEST_BUILD_FAILURE = 'test build failure'
    protected static final def VIOLATIONS = 'violations'
    protected static final def WARNING = 'warning'

    @TempDir
    public File projectDir

    protected static GradleProject rootProject() {
        new GradleProject('project', true)
                .withPlugin('groovy')
                .withPlugin('ca.cutterslade.analyze')
                .withGradleDependency('implementation')
    }

    protected static GradleProject platformProject(String name) {
        new GradleProject(name)
                .withPlugin('java-platform')
    }

    protected static GradleProject subProject(String name) {
        new GradleProject(name)
                .withPlugin('groovy')
                .withGradleDependency('implementation')
    }

    private GradleRunner gradleProject(boolean withCodeCoverage = true) {
        if (withCodeCoverage) {
            new FileOutputStream(new File(projectDir, 'gradle.properties')).withStream {
                it.write(getClass().classLoader.getResourceAsStream('testkit-gradle.properties').getBytes())
            }
        }
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()
                .withArguments()
    }

    protected static void assertBuildSuccess(BuildResult result) {
        if (result.task(':build') == null) {
            throw new SpockAssertionError("Build task not run: \n${result.getOutput()}")
        }
        assert result.task(':build').getOutcome() == TaskOutcome.SUCCESS
    }

    protected final BuildResult buildGradleProject(String expectedResult,
                                                   String gradleVersion = null,
                                                   boolean withCodeCoverage = true) {
        def project = gradleProject(withCodeCoverage)
        if (gradleVersion) {
            project.withGradleVersion(gradleVersion)
        }
        if (expectedResult == SUCCESS) {
            return project.build()
        }
        return project.buildAndFail()
    }

    protected static void assertBuildResult(BuildResult result,
                                            String expectedResult,
                                            List<String> usedUndeclaredArtifacts = [],
                                            List<String> unusedDeclaredArtifacts = []) {
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
        } else if (expectedResult == VIOLATIONS || expectedResult == WARNING) {
            def violations = expectedResult == WARNING ? new StringBuilder() : new StringBuilder('> ')
            violations.append('Dependency analysis found issues.\n')
            def spacer = expectedResult == WARNING ? '' : '  '
            if (!usedUndeclaredArtifacts.empty) {
                violations.append(spacer).append('usedUndeclaredArtifacts: \n')
                usedUndeclaredArtifacts.each { violations.append(spacer).append(" - ${it}\n") }
            }
            if (!unusedDeclaredArtifacts.empty) {
                violations.append(spacer).append('unusedDeclaredArtifacts: \n')
                unusedDeclaredArtifacts.each { violations.append(spacer).append(" - ${it}\n") }
            }
            violations.append('\n')
            assert result.output.contains(violations)
        } else {
            assert result.output.contains(expectedResult)
        }
    }
}

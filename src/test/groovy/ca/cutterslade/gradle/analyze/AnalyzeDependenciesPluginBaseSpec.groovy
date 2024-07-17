package ca.cutterslade.gradle.analyze


import ca.cutterslade.gradle.analyze.helper.GradleProject
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification
import spock.lang.TempDir

import java.lang.management.ManagementFactory

abstract class AnalyzeDependenciesPluginBaseSpec extends Specification {
    protected static final def SUCCESS = 'success'
    protected static final def BUILD_FAILURE = 'build failure'
    protected static final def TEST_BUILD_FAILURE = 'test build failure'
    protected static final def VIOLATIONS = 'violations'
    protected static final def WARNING = 'warning'

    @TempDir
    public File projectDir

    def runtimeMXBean = ManagementFactory.getRuntimeMXBean()

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
        def runner = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()
                .withArguments("--stacktrace")

        if (runtimeMXBean.inputArguments.any { it.startsWith('-agentlib:jdwp=') }) {
            runner.withDebug(true)
        }
        runner
    }

    protected static void assertBuildSuccess(BuildResult result) {
        if (result.task(':build') == null) {
            throw new SpockAssertionError("Build task not run: \n${result.getOutput()}")
        }
        assert result.task(':build').getOutcome() == TaskOutcome.SUCCESS
    }

    protected final BuildResult buildGradleProject(String expectedResult,
                                                   GradleVersion gradleVersion = null,
                                                   boolean withCodeCoverage = true) {
        def project = gradleProject(withCodeCoverage)
        if (gradleVersion) {
            project.withGradleVersion(gradleVersion.version)
        }
        if (expectedResult == SUCCESS) {
            return project.build()
        }
        return project.buildAndFail()
    }

    protected static void assertBuildResult(BuildResult result,
                                            String expectedResult,
                                            List<String> usedUndeclaredArtifacts = [],
                                            List<String> unusedDeclaredArtifacts = [],
                                            List<String> compileOnlyArtifacts = []) {
        if (expectedResult == SUCCESS) {
            def violations = expectedResult == SUCCESS ? new StringBuilder() : new StringBuilder('> ')
            if (!compileOnlyArtifacts.empty) {
                def spacer = expectedResult == SUCCESS ? '' : '  '
                violations.append(spacer).append('compileOnlyDeclaredArtifacts\n')
                compileOnlyArtifacts.each { violations.append(spacer).append(" - ${it}\n") }
                assert result.output.contains(violations)
            }
            assert result.tasks.count { it.outcome != TaskOutcome.SUCCESS } != 0
        } else if (expectedResult == BUILD_FAILURE || expectedResult == TEST_BUILD_FAILURE) {
            assert result.tasks.count { it.outcome == TaskOutcome.FAILED } != 0
        } else if (expectedResult == VIOLATIONS || expectedResult == WARNING) {
            def violations = expectedResult == WARNING ? new StringBuilder() : new StringBuilder('> ')
            violations.append('Dependency analysis found issues.\n')
            def spacer = expectedResult == WARNING ? '' : '  '
            if (!usedUndeclaredArtifacts.empty) {
                violations.append(spacer).append('usedUndeclaredArtifacts\n')
                usedUndeclaredArtifacts.each { violations.append(spacer).append(" - ${it}\n") }
            }
            if (!unusedDeclaredArtifacts.empty) {
                violations.append(spacer).append('unusedDeclaredArtifacts\n')
                unusedDeclaredArtifacts.each { violations.append(spacer).append(" - ${it}\n") }
            }
            if (!compileOnlyArtifacts.empty) {
                violations.append(spacer).append('compileOnlyDeclaredArtifacts\n')
                compileOnlyArtifacts.each { violations.append(spacer).append(" - ${it}\n") }
            }
            violations.append('\n')
            assert result.output.contains(violations)
        } else {
            assert result.output.contains(expectedResult)
        }
    }

    protected void copyProjectToTestFolder(String sourcePath, File destFolder) {
        URL resourceUrl = this.class.getResource("/$sourcePath")
        if (resourceUrl == null) {
            throw new IllegalStateException("Resource folder '$sourcePath' not found in classpath")
        }
        File sourceFolder = new File(resourceUrl.toURI())

        if (!sourceFolder.exists()) {
            throw new IllegalStateException("Source folder does not exist at $sourceFolder")
        }

        // Make sure destination is a directory and not a file
        if (!destFolder.exists()) {
            destFolder.mkdirs()
        } else if (!destFolder.isDirectory()) {
            throw new IllegalArgumentException("Destination must be a directory")
        }

        FileUtils.copyDirectory(sourceFolder, destFolder)
    }
}

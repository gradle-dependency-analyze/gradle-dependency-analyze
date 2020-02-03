package ca.cutterslade.gradle.analyze

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

class PluginSpec extends Specification {

    private static final String PROJECT_LOCATION = "src/test/resources/project"

    @Rule
    private TemporaryFolder projectDir

    def setup() {
        FileUtils.copyDirectory(new File(PROJECT_LOCATION), projectDir.getRoot())
    }

    def "successful standard build"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withPluginClasspath()
                .withArguments("build")
                .build()
        then:
        result.task(":build").getOutcome() == TaskOutcome.SUCCESS
    }

    @Unroll
    def "unusedDeclaredArtifacts in main classpath with configuration '#configuration'"(String configuration) {
        setup:
        new File(projectDir.getRoot(), "build.gradle").text = """
            plugins {
                id 'groovy'
                id 'ca.cutterslade.analyze'
            }
            
            dependencies {
                ${configuration} localGroovy()
                ${configuration} project(":dependent")
                ${configuration} project(":independent")
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withPluginClasspath()
                .withArguments("build")
                .buildAndFail()
        then:
        result.task(":analyzeClassesDependencies").getOutcome() == TaskOutcome.FAILED
        result.getOutput().contains("unusedDeclaredArtifacts")
        result.getOutput().contains("independent")

        where:
        configuration << ["compile", "implementation", "compileOnly"]
    }

    def "no unusedDeclaredArtifacts in main classpath with configuration 'runtimeOnly'"() {
        setup:
        new File(projectDir.getRoot(), "build.gradle").text = """
            plugins {
                id 'groovy'
                id 'ca.cutterslade.analyze'
            }
            
            dependencies {
                compile localGroovy()
                compile project(":dependent")
                runtimeOnly project(":independent")
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withPluginClasspath()
                .withArguments("build")
                .build()
        then:
        result.task(":analyzeClassesDependencies").getOutcome() == TaskOutcome.SUCCESS
    }

    def "usedUndeclaredArtifacts in main classpath"() {
        setup:
        Paths.get(projectDir.getRoot().absolutePath, "src/main/groovy/com/example", "OtherClass.groovy").toFile().text = """
            package com.example

            import com.example.Transient
            
            class OtherClass {
                def doSth() {
                    Transient.doSth()
                }
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withPluginClasspath()
                .withArguments("build")
                .buildAndFail()
        then:
        result.task(":analyzeClassesDependencies").getOutcome() == TaskOutcome.FAILED
        result.getOutput().contains("usedUndeclaredArtifacts")
        result.getOutput().contains("transient")
    }
}

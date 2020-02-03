package ca.cutterslade.gradle.analyze

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification
import spock.lang.Unroll

class AnalyzeDependenciesPluginSpec extends Specification {

    @Rule
    private TemporaryFolder projectDir

    def "successful standard build without dependencies"() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
                .create()

        when:
        def result = gradleProject().build()
        then:
        assertBuildSuccess(result)
    }

    @Unroll
    def "unusedDeclaredArtifacts in main classpath with configuration '#configuration'"(String configuration) {
        setup:
        rootProject()
                .withSubProject(subProject("independent"))
                .withDependency(new GradleDependency(configuration: configuration, project: "independent"))
                .create()

        when:
        def result = gradleProject().buildAndFail()
        then:
        result.task(":analyzeClassesDependencies").getOutcome() == TaskOutcome.FAILED
        result.getOutput().contains("unusedDeclaredArtifacts")
        result.getOutput().contains("independent")
        where:
        configuration << ["compile", "implementation", "compileOnly"]
    }


    @Unroll
    def "no unusedDeclaredArtifacts in main classpath with configuration '#configuration'"(String configuration) {
        setup:
        rootProject()
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
                .create()

        when:
        def result = gradleProject().build()
        then:
        assertBuildSuccess(result)
        where:
        configuration << ["compile", "implementation", "compileOnly"]
    }

    def "no unusedDeclaredArtifacts in main classpath with configuration 'runtimeOnly'"() {
        setup:
        rootProject()
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: "runtimeOnly", project: "dependent"))
                .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
                .create()

        when:
        def result = gradleProject().build()
        then:
        assertBuildSuccess(result)
        where:
        configuration << ["compile", "implementation", "compileOnly"]
    }

    private GradleProject rootProject() {
        new GradleProject(projectDir.getRoot(), "project")
                .withPlugin("ca.cutterslade.analyze")
                .withDependency(new GradleDependency(configuration: "compile", reference: "localGroovy()"))
    }

    private GradleProject subProject(String name) {
        new GradleProject(new File(projectDir.getRoot(), name), name)
                .withDependency(new GradleDependency(configuration: "compile", reference: "localGroovy()"))
    }

    private GradleRunner gradleProject() {
        GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withPluginClasspath()
                .withArguments("build")
    }

    private static void assertBuildSuccess(BuildResult result) {
        if (result.task(":build") == null) {
            throw new SpockAssertionError("Build task not run: \n${result.getOutput()}")
        }
        assert result.task(":build").getOutcome() == TaskOutcome.SUCCESS
    }
}

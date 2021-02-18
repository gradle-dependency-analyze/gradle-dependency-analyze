package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class AnalyzeDependenciesPluginSpec extends AnalyzeDependenciesPluginBaseSpec {
    def "simple build without dependencies results in success"() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
                .create(projectDir.getRoot())

        when:
        def result = gradleProject().build()

        then:
        assertBuildSuccess(result)
    }

    @Unroll
    def "used main dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration    | expectedResult
        "compile"        | SUCCESS
        "implementation" | SUCCESS
        "compileOnly"    | SUCCESS
        "runtimeOnly"    | BUILD_FAILURE
    }

    @Unroll
    def "used transient main dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main").usesClass("Transient"))
                .withSubProject(subProject("dependent")
                        .withDependency(new GradleDependency(configuration: "compile", project: "transient")))
                .withSubProject(subProject("transient")
                        .withMainClass(new GroovyClass("Transient")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration    | expectedResult
        "compile"        | "usedUndeclaredArtifacts"
        "implementation" | "usedUndeclaredArtifacts"
        "compileOnly"    | "usedUndeclaredArtifacts"
        "runtimeOnly"    | BUILD_FAILURE
    }

    @Unroll
    def "unused main dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withSubProject(subProject("independent")
                        .withMainClass(new GroovyClass("Independent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "independent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration    | expectedResult
        "compile"        | "unusedDeclaredArtifacts"
        "implementation" | "unusedDeclaredArtifacts"
        "compileOnly"    | "unusedDeclaredArtifacts"
        "runtimeOnly"    | SUCCESS
    }

    @Unroll
    def "used test dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("Test").usesClass("Dependent"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration        | expectedResult
        "testCompile"        | SUCCESS
        "testImplementation" | SUCCESS
        "testCompileOnly"    | SUCCESS
        "testRuntimeOnly"    | TEST_BUILD_FAILURE
    }

    @Unroll
    def "unused test dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("Test"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration        | expectedResult
        "testCompile"        | "unusedDeclaredArtifacts"
        "testImplementation" | "unusedDeclaredArtifacts"
        "testCompileOnly"    | "unusedDeclaredArtifacts"
        "testRuntimeOnly"    | SUCCESS
    }

    @Unroll
    def "used transient test dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("Test").usesClass("Transient"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent"))
                        .withDependency(new GradleDependency(configuration: "compile", project: "transient")))
                .withSubProject(subProject("transient")
                        .withMainClass(new GroovyClass("Transient")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration        | expectedResult
        "testCompile"        | "usedUndeclaredArtifacts"
        "testImplementation" | "usedUndeclaredArtifacts"
        "testCompileOnly"    | "usedUndeclaredArtifacts"
        "testRuntimeOnly"    | TEST_BUILD_FAILURE
    }
}

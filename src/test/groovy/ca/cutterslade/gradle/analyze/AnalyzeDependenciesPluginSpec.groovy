package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class AnalyzeDependenciesPluginSpec extends AnalyzeDependenciesPluginBaseSpec {
    def 'simple build without dependencies results in success'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('MainTest').usesClass('Main'))
                .create(projectDir.getRoot())

        when:
        def result = gradleProject().build()

        then:
        assertBuildSuccess(result)
    }

    @Unroll
    def 'used main dependency declared with #configuration results in #expectedResult'(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Dependent'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withDependency(new GradleDependency(configuration: configuration, project: 'dependent'))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration    | expectedResult
        'compile'        | SUCCESS
        'implementation' | SUCCESS
        'compileOnly'    | SUCCESS
        'runtimeOnly'    | BUILD_FAILURE
    }

    @Unroll
    def 'used transient main dependency declared with #configuration results in #expectedResult'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Transient'))
                .withSubProject(subProject('dependent')
                        .withDependency(new GradleDependency(configuration: 'compile', project: 'transient')))
                .withSubProject(subProject('transient')
                        .withMainClass(new GroovyClass('Transient')))
                .withDependency(new GradleDependency(configuration: configuration, project: 'dependent'))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts               | unusedDeclaredArtifacts
        'compile'        | VIOLATIONS     | ['project:transient:unspecified@jar'] | ['project:dependent:unspecified@jar']
        'implementation' | VIOLATIONS     | ['project:transient:unspecified@jar'] | ['project:dependent:unspecified@jar']
        'compileOnly'    | VIOLATIONS     | ['project:transient:unspecified@jar'] | ['project:dependent:unspecified@jar']
        'runtimeOnly'    | BUILD_FAILURE  | []                                    | []
    }

    @Unroll
    def 'unused main dependency declared with #configuration results in #expectedResult'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withSubProject(subProject('independent')
                        .withMainClass(new GroovyClass('Independent')))
                .withDependency(new GradleDependency(configuration: configuration, project: 'independent'))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        'compile'        | VIOLATIONS     | []                      | ['project:independent:unspecified@jar']
        'implementation' | VIOLATIONS     | []                      | ['project:independent:unspecified@jar']
        'compileOnly'    | VIOLATIONS     | []                      | ['project:independent:unspecified@jar']
        'runtimeOnly'    | SUCCESS        | []                      | []
    }

    @Unroll
    def 'used test dependency declared with #configuration results in #expectedResult'(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('Test').usesClass('Dependent'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withDependency(new GradleDependency(configuration: configuration, project: 'dependent'))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration        | expectedResult
        'testCompile'        | SUCCESS
        'testImplementation' | SUCCESS
        'testCompileOnly'    | SUCCESS
        'testRuntimeOnly'    | TEST_BUILD_FAILURE
    }

    @Unroll
    def 'unused test dependency declared with #configuration results in #expectedResult'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('Test'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withDependency(new GradleDependency(configuration: configuration, project: 'dependent'))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration        | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        'testCompile'        | VIOLATIONS     | []                      | ['project:dependent:unspecified@jar']
        'testImplementation' | VIOLATIONS     | []                      | ['project:dependent:unspecified@jar']
        'testCompileOnly'    | VIOLATIONS     | []                      | ['project:dependent:unspecified@jar']
        'testRuntimeOnly'    | SUCCESS        | []                      | []
    }

    @Unroll
    def 'used transient test dependency declared with #configuration results in #expectedResult'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('Test').usesClass('Transient'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent'))
                        .withDependency(new GradleDependency(configuration: 'compile', project: 'transient')))
                .withSubProject(subProject('transient')
                        .withMainClass(new GroovyClass('Transient')))
                .withDependency(new GradleDependency(configuration: configuration, project: 'dependent'))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration        | expectedResult     | usedUndeclaredArtifacts               | unusedDeclaredArtifacts
        'testCompile'        | VIOLATIONS         | ['project:transient:unspecified@jar'] | ['project:dependent:unspecified@jar']
        'testImplementation' | VIOLATIONS         | ['project:transient:unspecified@jar'] | ['project:dependent:unspecified@jar']
        'testCompileOnly'    | VIOLATIONS         | ['project:transient:unspecified@jar'] | ['project:dependent:unspecified@jar']
        'testRuntimeOnly'    | TEST_BUILD_FAILURE | []                                    | []
    }
}

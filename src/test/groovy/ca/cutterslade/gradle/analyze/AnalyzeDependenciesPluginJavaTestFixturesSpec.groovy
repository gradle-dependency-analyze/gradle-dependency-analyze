package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import spock.lang.Unroll

class AnalyzeDependenciesPluginJavaTestFixturesSpec extends AnalyzeDependenciesPluginBaseSpec {
    def 'project only with test fixture classes results in success'() {
        setup:
        rootProject()
                .withPlugin('java-test-fixtures')
                .withTestFixturesClass(new GroovyClass('MainTestFixture'))
                .withGradleDependency('testFixturesImplementation')
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    def 'project with main class and test fixture classes results in success'() {
        setup:
        rootProject()
                .withPlugin('java-test-fixtures')
                .withMainClass(new GroovyClass('Main'))
                .withTestFixturesClass(new GroovyClass('MainTestFixture').usesClass('Main'))
                .withGradleDependency('testFixturesImplementation')
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    def 'project with test fixture class using main class from different module results in success'() {
        setup:
        rootProject()
                .withPlugin('java-test-fixtures')
                .withTestFixturesClass(new GroovyClass('MainTestFixture').usesClass('DependentMain'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('DependentMain')))
                .withDependency(new GradleDependency(configuration: 'testFixturesImplementation', project: 'dependent'))
                .withGradleDependency('testFixturesImplementation')
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    @Unroll
    def 'project with test fixture class and main class from different module is not used results in failure'() {
        setup:
        rootProject()
                .withPlugin('java-test-fixtures')
                .withTestFixturesClass(new GroovyClass('MainTestFixture'))
                .withSubProject(subProject('dependent').withMainClass(new GroovyClass('DependentMain')))
                .withDependency(new GradleDependency(configuration: 'testFixturesImplementation', project: 'dependent'))
                .withGradleDependency('testFixturesImplementation')
                .create(projectDir)

        when:
        def result = buildGradleProject(BUILD_FAILURE)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        VIOLATIONS     | []                      | ['project :dependent']
    }

    def 'project with test, test fixture and main class and main class from different module is used results in success'() {
        setup:
        rootProject()
                .withPlugin('java-test-fixtures')
                .withMainClass(new GroovyClass('Main').usesClass('DependentMain'))
                .withTestClass(new GroovyClass('Test').usesClass('DependentMain'))
                .withTestFixturesClass(new GroovyClass('MainTestFixture'))
                .withSubProject(subProject('dependent').withMainClass(new GroovyClass('DependentMain')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent'))
                .withGradleDependency('testFixturesImplementation')
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }
}

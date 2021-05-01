package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import spock.lang.Unroll

class AnalyzeDependenciesPluginPermitSpec extends AnalyzeDependenciesPluginBaseSpec {
    @Unroll
    def 'project with unused dependency but permitted (#configuration) results in #expectedResult'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Dependent'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withSubProject(subProject('independent')
                        .withMainClass(new GroovyClass('Independent')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent'))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'independent'))
                .withDependency(new GradleDependency(configuration: configuration, project: 'independent'))
                .create(projectDir)

        when:
        def result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration          | expectedResult
        'permitUnusedDeclared' | SUCCESS
    }

    @Unroll
    def 'project with used dependency but permitted (#configuration) results in #expectedResult'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Dependent'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withSubProject(subProject('independent')
                        .withPlugin('java-library')
                        .withMainClass(new GroovyClass('Independent'))
                        .withDependency(new GradleDependency(configuration: 'api', project: 'dependent')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'independent'))
                .withDependency(new GradleDependency(configuration: configuration, project: 'independent'))
                .create(projectDir)

        when:
        def result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration        | expectedResult
        'permitUsedUndeclared' | SUCCESS
    }
}

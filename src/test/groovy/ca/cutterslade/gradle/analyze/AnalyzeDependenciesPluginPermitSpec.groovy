package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass

class AnalyzeDependenciesPluginPermitSpec extends AnalyzeDependenciesPluginBaseSpec {
    def 'project with unused dependency but permitted'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Dependent'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withSubProject(subProject('independent')
                        .withMainClass(new GroovyClass('Independent')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent'))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'independent'))
                .withDependency(new GradleDependency(configuration: 'permitUnusedDeclared', project: 'independent'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    def 'project with used dependency but permitted'() {
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
                .withDependency(new GradleDependency(configuration: 'permitUsedUndeclared', project: 'independent'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }
}

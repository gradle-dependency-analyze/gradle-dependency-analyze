package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass

class AnalyzeDependenciesPluginClasspathOrder extends AnalyzeDependenciesPluginBaseSpec {
    def 'project with two dependencies ordered: first-second'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main')
                        .usesClass('One')
                        .usesClass("Two"))
                .withSubProject(subProject('first')
                        .withMainClass(new GroovyClass('One')))
                .withSubProject(subProject('second')
                        .withMainClass(new GroovyClass('One'))
                        .withMainClass(new GroovyClass("Two")))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'first'))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'second'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, SUCCESS)
    }

    def 'project with two dependencies ordered: second-first'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main')
                        .usesClass('One')
                        .usesClass("Two"))
                .withSubProject(subProject('first')
                        .withMainClass(new GroovyClass('One')))
                .withSubProject(subProject('second')
                        .withMainClass(new GroovyClass('One'))
                        .withMainClass(new GroovyClass("Two")))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'second'))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'first'))
                .create(projectDir)

        when:
        def result = buildGradleProject(VIOLATIONS)

        then:
        assertBuildResult(result, VIOLATIONS, [], ['project:first:unspecified@jar'])
    }
}

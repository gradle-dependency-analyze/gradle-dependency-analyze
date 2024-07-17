package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass

class AnalyzeDependenciesPluginNarDependency extends AnalyzeDependenciesPluginBaseSpec {
    def 'project with a nar dependency'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withMavenRepositories()
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.apache.bookkeeper:bookkeeper-common:4.14.4'))
                .create(projectDir)

        when:
        def result = buildGradleProject(VIOLATIONS)

        then:
        assertBuildResult(result, VIOLATIONS, [], ['org.apache.bookkeeper:bookkeeper-common:4.14.4'])
    }
}

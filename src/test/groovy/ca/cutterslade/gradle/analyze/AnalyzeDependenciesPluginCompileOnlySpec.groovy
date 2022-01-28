package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass

class AnalyzeDependenciesPluginCompileOnlySpec extends AnalyzeDependenciesPluginBaseSpec {
    def 'build with constants usage from dependency results in #expectedResult'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withMainClass(new GroovyClass("Foo").withClassAnnotation("lombok.Data", "").usesClass("java.lang.Integer"))
                .withDependency(new GradleDependency(configuration: 'compileOnly', id: 'org.projectlombok:lombok:1.18.22'))
                .withDependency(new GradleDependency(configuration: 'annotationProcessor', id: 'org.projectlombok:lombok:1.18.22'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, WARNING, [], ["project:bar:unspecified@jar"])
    }

    def 'build with compileOnly dependency needed also in runtime'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withWarnCompileOnly(true)
                .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull"))
                .withDependency(new GradleDependency(configuration: 'compileOnly', id: 'org.projectlombok:lombok:1.18.22'))
                .create(projectDir)

        when:
        def result = buildGradleProject(VIOLATIONS)

        then:
        assertBuildResult(result, VIOLATIONS, ['org.projectlombok:lombok:1.18.22@jar'], [])
    }

}

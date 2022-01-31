package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import spock.lang.Unroll

class AnalyzeDependenciesPluginCompileOnlySpec extends AnalyzeDependenciesPluginBaseSpec {
    @Unroll
    def 'build with compileOnly dependency not needed in runtime and warnCompileOnly #warnCompileOnly'(boolean warnCompileOnly, compileOnlyArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withWarnCompileOnly(warnCompileOnly)
                .withMainClass(new GroovyClass("Foo").withClassAnnotation("lombok.Data", "").usesClass("java.lang.Integer"))
                .withDependency(new GradleDependency(configuration: 'compileOnly', id: 'org.projectlombok:lombok:1.18.22'))
                .withDependency(new GradleDependency(configuration: 'annotationProcessor', id: 'org.projectlombok:lombok:1.18.22'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, SUCCESS, [], [], compileOnlyArtifacts)

        where:
        warnCompileOnly | compileOnlyArtifacts
        true            | ['org.projectlombok:lombok:1.18.22@jar']
        false           | []
    }

    @Unroll
    def 'build with compileOnly dependency needed also in runtime and warnCompileOnly #warnCompileOnly'(boolean warnCompileOnly, compileOnlyArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withWarnCompileOnly(warnCompileOnly)
                .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull"))
                .withDependency(new GradleDependency(configuration: 'compileOnly', id: 'org.projectlombok:lombok:1.18.22'))
                .create(projectDir)

        when:
        def result = buildGradleProject(VIOLATIONS)

        then:
        assertBuildResult(result, VIOLATIONS, ['org.projectlombok:lombok:1.18.22@jar'], [], compileOnlyArtifacts)

        where:
        warnCompileOnly | compileOnlyArtifacts
        true            | ['org.projectlombok:lombok:1.18.22@jar']
        false           | []
    }
}

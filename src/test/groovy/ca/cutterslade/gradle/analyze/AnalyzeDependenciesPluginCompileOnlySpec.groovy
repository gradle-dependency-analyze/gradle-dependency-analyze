package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import spock.lang.Unroll

class AnalyzeDependenciesPluginCompileOnlySpec extends AnalyzeDependenciesPluginBaseSpec {
    @Unroll
    def 'build with compileOnly dependency not needed in runtime and warnCompileOnly #warnCompileOnly'() {
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
    def 'build with compileOnly dependency needed also in runtime and warnCompileOnly #warnCompileOnly'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withWarnCompileOnly(warnCompileOnly)
                .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull"))
                .withDependency(new GradleDependency(configuration: 'compileOnly', id: 'org.projectlombok:lombok:1.18.22'))
                .create(projectDir)

        when:
        def result = buildGradleProject(buildResult)

        then:
        assertBuildResult(result, buildResult, usedUndeclaredArtifacts, [], compileOnlyArtifacts)

        where:
        warnCompileOnly | buildResult | usedUndeclaredArtifacts                  | compileOnlyArtifacts
        true            | VIOLATIONS  | ['org.projectlombok:lombok:1.18.22@jar'] | ['org.projectlombok:lombok:1.18.22@jar']
        false           | SUCCESS     | []                                       | []
    }

    @Unroll
    def 'build with compileOnly dependency needed also in runtime and compileOnly and warnCompileOnly #warnCompileOnly'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withWarnCompileOnly(warnCompileOnly)
                .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull").withClassAnnotation("lombok.Data", ""))
                .withDependency(new GradleDependency(configuration: 'compileOnly', id: 'org.projectlombok:lombok:1.18.22'))
                .create(projectDir)

        when:
        def result = buildGradleProject(buildResult)

        then:
        assertBuildResult(result, buildResult, usedUndeclaredArtifacts, [], compileOnlyArtifacts)

        where:
        warnCompileOnly | buildResult | usedUndeclaredArtifacts                  | compileOnlyArtifacts
        true            | VIOLATIONS  | ['org.projectlombok:lombok:1.18.22@jar'] | ['org.projectlombok:lombok:1.18.22@jar']
        false           | SUCCESS     | []                                       | []
    }

    @Unroll
    def 'build with compileOnly dependency needed also in implementation and compileOnly and warnCompileOnly #warnCompileOnly'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withWarnCompileOnly(warnCompileOnly)
                .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull").withClassAnnotation("lombok.Data", ""))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.projectlombok:lombok:1.18.22'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, SUCCESS, [], [], compileOnlyArtifacts)

        where:
        warnCompileOnly | compileOnlyArtifacts
        true            | []
        false           | []
    }

    @Unroll
    def 'build with compileOnly dependency needed in runtime and compileOnly and warnCompileOnly #warnCompileOnly'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withWarnCompileOnly(warnCompileOnly)
                .withMainClass(new GroovyClass("Foo").usesClass("lombok.NonNull"))
                .withDependency(new GradleDependency(configuration: 'compileOnly', id: 'org.projectlombok:lombok:1.18.22'))
                .withDependency(new GradleDependency(configuration: 'permitUsedUndeclared', id: 'org.projectlombok:lombok:1.18.22'))
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
}

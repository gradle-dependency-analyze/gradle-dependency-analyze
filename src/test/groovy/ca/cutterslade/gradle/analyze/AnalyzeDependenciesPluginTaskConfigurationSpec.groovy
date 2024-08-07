package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class AnalyzeDependenciesPluginTaskConfigurationSpec extends AnalyzeDependenciesPluginBaseSpec {
    @Unroll
    def 'project with warnUsedUndeclared=#warnUsedUndeclared warnUnusedDeclared=#warnUnusedDeclared results in #expectedResult'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Dependent2'))
                .withSubProject(subProject('dependent1')
                        .withPlugin('java-library')
                        .withDependency(new GradleDependency(configuration: 'api', project: 'dependent2')))
                .withSubProject(subProject('dependent2')
                        .withMainClass(new GroovyClass('Dependent2')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent1'))
                .withWarnUsedUndeclared(warnUsedUndeclared)
                .withWarnUnusedDeclared(warnUnusedDeclared)
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        warnUsedUndeclared | warnUnusedDeclared | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        false              | false              | VIOLATIONS     | ['project :dependent2'] | ['project :dependent1']
        false              | true               | VIOLATIONS     | ['project :dependent2'] | []
        true               | false              | VIOLATIONS     | []                      | ['project :dependent1']
        true               | true               | SUCCESS        | ['project :dependent2'] | ['project :dependent1']
    }
}

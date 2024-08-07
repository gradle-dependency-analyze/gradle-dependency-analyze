package ca.cutterslade.gradle.analyze.projects

import ca.cutterslade.gradle.analyze.AnalyzeDependenciesPluginBaseSpec

class AnalyzeDependenciesPluginProjectsSpec extends AnalyzeDependenciesPluginBaseSpec {
    def 'issue_527'() {
        setup:
        copyProjectToTestFolder('projects/issue_527', projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, SUCCESS)
    }

    def 'issue_400'() {
        setup:
        copyProjectToTestFolder('projects/issue_400', projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, SUCCESS)
    }

    def 'issue_288'() {
        setup:
        copyProjectToTestFolder('projects/issue_288', projectDir)
        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, SUCCESS)
    }

    def 'issue_528'() {
        setup:
        copyProjectToTestFolder('projects/issue_528', projectDir)
        when:
        def result = buildGradleProject(VIOLATIONS)

        then:
        assertBuildResult(result, VIOLATIONS, [], [], [], ['jakarta.annotation:jakarta.annotation-api:2.1.1'])
    }
}

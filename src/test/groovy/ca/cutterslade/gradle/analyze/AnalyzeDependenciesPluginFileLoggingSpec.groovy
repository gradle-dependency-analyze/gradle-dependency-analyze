package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import spock.lang.Unroll

import static ca.cutterslade.gradle.analyze.AnalyzeDependenciesTask.DEPENDENCY_ANALYZE_DEPENDENCY_DIRECTORY_NAME

class AnalyzeDependenciesPluginFileLoggingSpec extends AnalyzeDependenciesPluginBaseSpec {
    def 'simple build without dependencies results in success'() {
        setup:
        rootProject()
                .logDependencyInformationToFiles()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('MainTest').usesClass('Main'))
                .create(projectDir)

        when:
        def result = gradleProject().build()

        then:
        assertBuildSuccess(result)
        assertLogFile('simple_analyzeDependencies.log')
    }

    @Unroll
    def 'build with dependency declared in config and used in build'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .logDependencyInformationToFiles()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
        assertLogFile('complex_analyzeDependencies.log')
    }

    private void assertLogFile(final String fileWithExpectedContent) {
        def actual = new File(projectDir, "build/$DEPENDENCY_ANALYZE_DEPENDENCY_DIRECTORY_NAME/analyzeDependencies.log").readLines()
                .collect { it.replaceAll('/spock_[0-9_a-zA-Z]*/', '/') }
        def expected = getClass().getResource('/' + fileWithExpectedContent).readLines()
                .collect { it.replaceAll('/spock_[0-9_a-zA-Z]*/', '/') }
        assert actual == expected
    }

}

package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.DisplayVisitor
import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import org.apache.commons.text.diff.StringsComparator

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
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    def 'simple build with unused dependencies results in violation'() {
        setup:
        rootProject()
                .logDependencyInformationToFiles()
                .withMavenRepositories()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('MainTest').usesClass('Main'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .create(projectDir)

        when:
        def result = buildGradleProject(VIOLATIONS)

        then:
        assertBuildResult(result, VIOLATIONS, [], ['org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar'])
        assertLogFile(projectDir, 'analyzeClassesDependencies.log', 'analyzeClassesDependencies.log')
    }

    def 'simple build with unused dependencies results in success when justWarn'() {
        setup:
        rootProject()
                .logDependencyInformationToFiles()
                .withMavenRepositories()
                .withWarnUnusedDeclared(true)
                .withWarnUsedUndeclared(true)
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('MainTest').usesClass('Main'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .create(projectDir)

        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
        assertLogFile(projectDir, 'analyzeClassesDependencies.log', 'analyzeClassesDependencies.log')
    }

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
        assertLogFile(projectDir, 'complex_analyzeDependencies.log', 'analyzeClassesDependencies.log')
    }

    private static void assertLogFile(final File projectDir,
                                      final String fileWithExpectedContent,
                                      final String fileName) {
        def actual = new File(projectDir, "build/$DEPENDENCY_ANALYZE_DEPENDENCY_DIRECTORY_NAME/$fileName").text
        def expected = getClass().getResource('/' + fileWithExpectedContent).text
        StringsComparator comparator = new StringsComparator(actual, expected)

        def script = comparator.getScript()

        if (script.modifications != 0) {
            def visitor = new DisplayVisitor()
            script.visit(visitor)
            System.err.println visitor.left
            System.err.println visitor.right
        }

        assert script.modifications == 0
    }
}

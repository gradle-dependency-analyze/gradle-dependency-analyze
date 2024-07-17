package ca.cutterslade.gradle.analyze


import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import com.github.difflib.DiffUtils

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
        def files = getFileContent('analyzeClassesDependencies.log', 'analyzeClassesDependencies.log')

        then:
        assertBuildResult(result, VIOLATIONS, [], ['org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'])
        if (files.v1 != files.v2) {
            println "Files differ:" + System.lineSeparator() + generateDiff(files.v1, files.v2)
        }

        files.v1 == files.v2

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
        def files = getFileContent('analyzeClassesDependencies.log', 'analyzeClassesDependencies.log')

        then:
        assertBuildSuccess(result)
        if (files.v1 != files.v2) {
            println "Files differ:" + System.lineSeparator() + generateDiff(files.v1, files.v2)
        }

        files.v1 == files.v2
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
        def files = getFileContent('complex_analyzeDependencies.log', 'analyzeClassesDependencies.log')

        then:
        assertBuildSuccess(result)
        if (files.v1 != files.v2) {
            println "Files differ:" + System.lineSeparator() + generateDiff(files.v1, files.v2)
        }

        files.v1 == files.v2
    }

    Tuple2<String, String> getFileContent(String fileWithExpectedContent, String fileName) {
        def actual = new File(projectDir, "build/reports/dependency-analyze/$fileName").text.replaceAll("\\r\\n", System.lineSeparator())
        def expected = getClass().getResource('/' + fileWithExpectedContent).text.replaceAll("\\r\\n", System.lineSeparator())
        new Tuple2<>(actual, expected)
    }

    String generateDiff(String content1, String content2) {
        def diff = DiffUtils.diff(content1.readLines(), content2.readLines())
        return diff.getDeltas().join(System.lineSeparator())
    }
}

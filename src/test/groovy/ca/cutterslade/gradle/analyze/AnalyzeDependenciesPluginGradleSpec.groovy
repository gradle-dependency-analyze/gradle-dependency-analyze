package ca.cutterslade.gradle.analyze

import static ca.cutterslade.gradle.analyze.util.GradleVersionUtil.isWarPluginBrokenWhenUsingProvidedRuntime

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import spock.lang.Unroll

class AnalyzeDependenciesPluginGradleSpec extends AnalyzeDependenciesPluginBaseSpec {
    @Unroll
    def 'simple build without dependencies results in #expectedResult for Gradle version #gradleVersion'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('MainTest').usesClass('Main'))
                .create(projectDir)

        when:
        def result = buildGradleProject(expectedResult, gradleVersion, false)

        then:
        assertBuildResult(result, expectedResult)

        where:
        pair << determineMinorVersions()
        gradleVersion = pair.v1
        expectedResult = pair.v2
    }

    @Unroll
    def 'used main dependency declared results in #expectedResult for Gradle version #gradleVersion'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Dependent'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent'))
                .create(projectDir)

        when:
        def result = buildGradleProject(expectedResult, gradleVersion, false)

        then:
        assertBuildResult(result, expectedResult)

        where:
        pair << determineMinorVersions()
        gradleVersion = pair.v1
        expectedResult = pair.v2
    }

    @Unroll
    def 'project only with test fixture classes results in #expectedResult for Gradle version #gradleVersion'() {
        setup:
        rootProject()
                .withPlugin('java-test-fixtures')
                .withTestFixturesClass(new GroovyClass('MainTestFixture'))
                .withGradleDependency('testFixturesImplementation')
                .create(projectDir)

        when:
        def result = buildGradleProject(expectedResult, gradleVersion, false)

        then:
        assertBuildResult(result, expectedResult)

        where:
        pair << determineMinorVersions('5.6')
        gradleVersion = pair.v1
        expectedResult = pair.v2
    }

    @Unroll
    def 'project with providedRuntime is #expectedResult for Gradle version #gradleVersion'() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withPlugin('war')
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('MainTest').usesClass('Main'))
                .withDependency(new GradleDependency(configuration: 'providedRuntime', id: 'org.springframework.boot:spring-boot-starter-tomcat:2.3.6.RELEASE'))
                .create(projectDir)

        when:
        def result = buildGradleProject(expectedResult, gradleVersion)

        then:
        assertBuildResult(result, expectedResult, [], unusedDeclaredArtifacts)
        result.output.contains('https://github.com/gradle/gradle/issues/17415') == isWarPluginBrokenWhenUsingProvidedRuntime(gradleVersion)

        where:
        pair << determineMinorVersions('6.9', '7.3')
        gradleVersion = pair.v1
        expectedResult = isWarPluginBrokenWhenUsingProvidedRuntime(pair.v1) ? VIOLATIONS : SUCCESS
        unusedDeclaredArtifacts = isWarPluginBrokenWhenUsingProvidedRuntime(pair.v1) ? ['org.springframework.boot:spring-boot-starter-tomcat:2.3.6.RELEASE'] : []
    }

    @Unroll
    def "aggregator dependency declared in config and used in build results in #expectedResult for Gradle version #gradleVersion"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: "implementation", project: "dependent"))
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult, gradleVersion, false)

        then:
        assertBuildResult(result, expectedResult)

        where:
        pair << determineMinorVersions()
        gradleVersion = pair.v1
        expectedResult = pair.v2
    }


    static List<Tuple2<GradleVersion, String>> determineMinorVersions(String minVersion = '5.0', String maxVersion = '9.0', String expectedResult = SUCCESS) {
        try {
            def serviceUrl = new URL("https://services.gradle.org/versions/all")
            def versions = new ObjectMapper().readValue(serviceUrl, JsonNode.class)
            def minGradleVersion = GradleVersion.version(minVersion)
            def maxGradleVersion = GradleVersion.version(maxVersion)

            StreamSupport.stream(versions.spliterator(), false)
                    .filter { node -> !node.get("broken") }
                    .map { node -> GradleVersion.version(node.get("version").asText()) }
                    .filter { version -> version >= minGradleVersion && version <= maxGradleVersion }
                    .filter { version -> version == version.getBaseVersion() }
                    .sorted { a, b -> a <=> b }
                    .collect(Collectors.toList())
                    .groupBy {
                        def idx = ordinalIndexOf(it.version, '.', 2)
                        idx == -1 ? it.version : it.version.substring(0, idx)
                    }.collect { k, v -> v.sort().last() }
                    .collect { it -> new Tuple2<GradleVersion, String>(it, expectedResult) }
        } catch (IOException e) {
            throw new UncheckedIOException(e)
        }
    }

    static int ordinalIndexOf(String str, String subStr, int n) {
        int pos = -1
        while (true) {
            def newPos = str.indexOf(subStr, pos + 1)
            if (newPos == -1) {
                pos = -1
                break
            } else {
                pos = newPos
                if (--n <= 0) {
                    break
                }
            }
        }
        pos
    }
}

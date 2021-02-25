package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.util.GradleVersion
import spock.lang.Unroll

import java.util.stream.Collectors
import java.util.stream.StreamSupport

class AnalyzeDependenciesPluginGradleSpec extends AnalyzeDependenciesPluginBaseSpec {
    @Unroll
    def 'simple build without dependencies results in #expectedResult for Gradle version #gradleVersion'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main'))
                .withTestClass(new GroovyClass('MainTest').usesClass('Main'))
                .create(projectDir.getRoot())

        when:
        def result = buildGradleProject(expectedResult, gradleVersion)

        then:
        assertBuildResult(result, expectedResult)

        where:
        pair << determineMinorVersions('5.0', '7.0', SUCCESS)
        gradleVersion = pair.first.version as String
        expectedResult = pair.second as String
    }

    @Unroll
    def 'used main dependency declared results in #expectedResult for Gradle version #gradleVersion'() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass('Main').usesClass('Dependent'))
                .withSubProject(subProject('dependent')
                        .withMainClass(new GroovyClass('Dependent')))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent'))
                .create(projectDir.getRoot())

        when:
        def result = buildGradleProject(expectedResult, gradleVersion)

        then:
        assertBuildResult(result, expectedResult)

        where:
        pair << determineMinorVersions('5.0', '7.0', SUCCESS)
        gradleVersion = pair.first.version as String
        expectedResult = pair.second as String
    }

    @Unroll
    def 'project only with test fixture classes results in #expectedResult for Gradle version #gradleVersion'() {
        setup:
        rootProject()
                .withPlugin('java-test-fixtures')
                .withTestFixturesClass(new GroovyClass('MainTestFixture'))
                .withGradleDependency('testFixturesImplementation')
                .create(projectDir.getRoot())

        when:
        def result = buildGradleProject(expectedResult, gradleVersion)

        then:
        assertBuildResult(result, expectedResult)

        where:
        pair << determineMinorVersions('5.6', '7.0', SUCCESS)
        gradleVersion = pair.first.version as String
        expectedResult = pair.second as String
    }

    def determineMinorVersions(String minVersion, String maxVersion, String expectedResult) {
        try {
            def serviceUrl = new URL("https://services.gradle.org/versions/all");
            def versions = new ObjectMapper().readValue(serviceUrl, JsonNode.class);
            def minGradleVersion = GradleVersion.version(minVersion)
            def maxGradleVersion = GradleVersion.version(maxVersion)

            StreamSupport.stream(versions.spliterator(), false)
                    .filter { node -> !node.get("broken") }
                    .map { node -> GradleVersion.version(node.get("version").asText()) }
                    .filter { version -> version >= minGradleVersion && version < maxGradleVersion }
                    .filter { version -> version == version.getBaseVersion() }
                    .sorted { a, b -> a <=> b }
                    .collect(Collectors.toList())
                    .groupBy {
                        def idx = ordinalIndexOf(it.version, '.', 2)
                        idx == -1 ? it.version : it.version.substring(0, idx)
                    }.collect { k, v -> v.sort().last() }
                    .collect { it -> new Tuple2(it, expectedResult) }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static int ordinalIndexOf(String str, String subStr, int n) {
        int pos = -1;
        while (true) {
            def newPos = str.indexOf(subStr, pos + 1);
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
        pos;
    }
}

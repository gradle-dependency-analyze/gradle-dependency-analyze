package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class AnalyzeDependenciesPluginAggregatorSpec extends AnalyzeDependenciesPluginBaseSpec {
    @Unroll
    def "aggregator dependency declared in config and used in build with #configuration results in #expectedResult"() {
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
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration    | expectedResult
        "implementation" | SUCCESS
        "runtimeOnly"    | BUILD_FAILURE
    }

    @Unroll
    def "aggregator dependency not declared in config and used in build should report unused aggregator with #configuration results in #expectedResult"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts                                                                                  | unusedDeclaredArtifacts
        "implementation" | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE", "org.springframework:spring-context:5.2.11.RELEASE"] | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"]
        "runtimeOnly"    | BUILD_FAILURE  | []                                                                                                       | []
    }

    @Unroll
    def "aggregator dependency declared in config and not used in build with dedicated dependency should report missing dependency with #configuration results in #expectedResult"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework:spring-context:5.2.11.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts                             | unusedDeclaredArtifacts
        "implementation" | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE"] | []
        "runtimeOnly"    | BUILD_FAILURE  | []                                                  | []
    }

    @Unroll
    def "aggregator dependency declared in config and used in build together with dedicated dependency should report explicit dependency as unused with #configuration results in #expectedResult"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework:spring-context:5.2.11.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts, [], superfluousDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts | superfluousDeclaredArtifacts
        "implementation" | VIOLATIONS     | []                      | []                      | ["org.springframework:spring-context:5.2.11.RELEASE"]
        "runtimeOnly"    | BUILD_FAILURE  | []                      | []                      | []
    }

    def "multiple aggregator dependencies declared and dependency available in both aggregators should choose the aggregator with less dependencies and report other as unused"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, VIOLATIONS, [], ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE"])
    }

    def "multiple aggregator dependencies declared in config and dependency available in both aggregators plus one additional should choose aggregators with less dependencies"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.jdbc.BadSqlGrammarException')
                )
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, VIOLATIONS, [], ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"])
    }

    def "multiple aggregator dependencies declared in config and dependency available in both aggregators plus one additional should keep only used distinct aggregators"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-validation:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-validation:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.jdbc.BadSqlGrammarException')
                        .usesClass('org.springframework.web.bind.annotation.RestController')
                        .usesClass('javax.validation.ConstraintValidator')
                )
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, VIOLATIONS, [], ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"])
    }

    def "multiple aggregator dependencies declared in config and another aggregator is smaller"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('com.fasterxml.jackson.databind.ObjectMapper')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.web.context.request.RequestContextHolder')
                )
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, VIOLATIONS, ["org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE"], ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE"])
    }

    def "aggregator from project"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', project: 'bom'))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'bom'))
                .withDependency(new GradleDependency(configuration: 'implementation', project: 'dependent'))
                .withSubProject(subProject('bom')
                        .withPlugin('java-library')
                        .withDependency(new GradleDependency(configuration: 'api', project: 'dependent'))
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent"))
                )
                .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, VIOLATIONS, [], [], [], ["project :dependent"])
    }

    def "aggregator from project with used jar dependency"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitTestAggregatorUse', project: 'spring'))
                .withSubProject(subProject('spring')
                        .withPlugin('java-library')
                        .withDependency(new GradleDependency(configuration: 'api', id: 'org.springframework:spring-beans:5.2.11.RELEASE'))
                        .withDependency(new GradleDependency(configuration: 'api', id: 'org.springframework:spring-context:5.2.11.RELEASE'))

                )
                .withSubProject(subProject('tests')
                        .withMavenRepositories()
                        .withDependency(new GradleDependency(configuration: 'testImplementation', project: 'spring'))
                        .withTestClass(new GroovyClass("Main")
                                .usesClass('org.springframework.context.annotation.ComponentScan'))
                )
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    def "aggregator with api and implementation from project with used jar dependency"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitTestAggregatorUse', project: 'spring'))
                .withSubProject(subProject('spring')
                        .withPlugin('java-library')
                        .withMavenRepositories()
                        .withDependency(new GradleDependency(configuration: 'api', id: 'org.apache.commons:commons-collections4:4.4'))
                        .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework:spring-context:5.2.11.RELEASE'))
                )
                .withSubProject(subProject('tests')
                        .withMavenRepositories()
                        .withDependency(new GradleDependency(configuration: 'testImplementation', project: 'spring'))
                        .withDependency(new GradleDependency(configuration: 'testImplementation', id: 'org.springframework:spring-aop:5.2.11.RELEASE'))
                        .withTestClass(new GroovyClass("Main")
                                .usesClass('org.springframework.aop.Advisor')
                                .usesClass('org.apache.commons.collections4.BidiMap'))
                )
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    def "aggregator with implementation dependency and additional overlapping api dependency"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withPlugin('java-library')
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'api', id: 'org.springframework:spring-web:5.2.11.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass("Main")
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.web.context.request.RequestContextHolder'))
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }

    def "aggregator with implementation dependency and additional overlapping api dependency when versions managed by platform"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withSubProject(platformProject('platform')
                        .withDependency(new GradleDependency(configuration: 'api', reference: 'enforcedPlatform("org.springframework.boot:spring-boot-dependencies:2.3.6.RELEASE")'))
                )
                .withPlugin('java-library')
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web'))
                .withDependency(new GradleDependency(configuration: 'api', id: 'org.springframework:spring-web'))
                .withDependency(new GradleDependency(configuration: 'implementation', id: 'org.springframework.boot:spring-boot-starter-web'))
                .withMainClass(new GroovyClass("Main")
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.web.context.request.RequestContextHolder'))
                .applyPlatformConfiguration()
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(SUCCESS)

        then:
        assertBuildSuccess(result)
    }
}

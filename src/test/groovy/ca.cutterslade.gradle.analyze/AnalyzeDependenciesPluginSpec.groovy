package ca.cutterslade.gradle.analyze

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification
import spock.lang.Unroll

class AnalyzeDependenciesPluginSpec extends Specification {

    private static final String SUCCESS = "success"
    private static final String BUILD_FAILURE = "build failure"
    private static final String TEST_BUILD_FAILURE = "test build failure"
    private static final String VIOLATIONS = "violations"

    @Rule
    private TemporaryFolder projectDir

    def "simple build without dependencies results in success"() {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("MainTest").usesClass("Main"))
                .create(projectDir.getRoot())

        when:
        def result = gradleProject().build()
        then:
        assertBuildSuccess(result)
    }

    @Unroll
    def "used main dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main").usesClass("Dependent"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration    | expectedResult
        "compile"        | SUCCESS
        "implementation" | SUCCESS
        "compileOnly"    | SUCCESS
        "runtimeOnly"    | BUILD_FAILURE
    }

    @Unroll
    def "used transient main dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main").usesClass("Transient"))
                .withSubProject(subProject("dependent")
                        .withDependency(new GradleDependency(configuration: "compile", project: "transient")))
                .withSubProject(subProject("transient")
                        .withMainClass(new GroovyClass("Transient")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult      | usedUndeclaredArtifacts               | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS          | ["project:transient:unspecified@jar"] | ["project:dependent:unspecified@jar"]
        "implementation" | VIOLATIONS          | ["project:transient:unspecified@jar"] | ["project:dependent:unspecified@jar"]
        "compileOnly"    | VIOLATIONS          | ["project:transient:unspecified@jar"] | ["project:dependent:unspecified@jar"]
        "runtimeOnly"    | BUILD_FAILURE       | []                                    | []
    }

    @Unroll
    def "unused main dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withSubProject(subProject("independent")
                        .withMainClass(new GroovyClass("Independent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "independent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult      | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS          | []                      | ["project:independent:unspecified@jar"]
        "implementation" | VIOLATIONS          | []                      | ["project:independent:unspecified@jar"]
        "compileOnly"    | VIOLATIONS          | []                      | ["project:independent:unspecified@jar"]
        "runtimeOnly"    | SUCCESS             | []                      | []
    }

    @Unroll
    def "used test dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("Test").usesClass("Dependent"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration        | expectedResult
        "testCompile"        | SUCCESS
        "testImplementation" | SUCCESS
        "testCompileOnly"    | SUCCESS
        "testRuntimeOnly"    | TEST_BUILD_FAILURE
    }

    @Unroll
    def "unused test dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("Test"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration        | expectedResult      | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        "testCompile"        | VIOLATIONS          | []                      | ["project:dependent:unspecified@jar"]
        "testImplementation" | VIOLATIONS          | []                      | ["project:dependent:unspecified@jar"]
        "testCompileOnly"    | VIOLATIONS          | []                      | ["project:dependent:unspecified@jar"]
        "testRuntimeOnly"    | SUCCESS             | []                      | []
    }

    @Unroll
    def "used transient test dependency declared with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMainClass(new GroovyClass("Main"))
                .withTestClass(new GroovyClass("Test").usesClass("Transient"))
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent"))
                        .withDependency(new GradleDependency(configuration: "compile", project: "transient")))
                .withSubProject(subProject("transient")
                        .withMainClass(new GroovyClass("Transient")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration        | expectedResult      | usedUndeclaredArtifacts               | unusedDeclaredArtifacts
        "testCompile"        | VIOLATIONS          | ["project:transient:unspecified@jar"] | ["project:dependent:unspecified@jar"]
        "testImplementation" | VIOLATIONS          | ["project:transient:unspecified@jar"] | ["project:dependent:unspecified@jar"]
        "testCompileOnly"    | VIOLATIONS          | ["project:transient:unspecified@jar"] | ["project:dependent:unspecified@jar"]
        "testRuntimeOnly"    | TEST_BUILD_FAILURE  | []                                    | []
    }

    @Unroll
    def "aggregator dependency declared in config and used in build with #configuration results in #expectedResult"(String configuration, String expectedResult) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult)

        where:
        configuration    | expectedResult
        "compile"        | SUCCESS
        "implementation" | SUCCESS
        "compileOnly"    | SUCCESS
        "runtimeOnly"    | BUILD_FAILURE
    }

    @Unroll
    def "aggregator dependency not declared in config and used in build should report unused aggregator with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts                                                                                          | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE@jar", "org.springframework:spring-context:5.2.11.RELEASE@jar"] | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "implementation" | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE@jar", "org.springframework:spring-context:5.2.11.RELEASE@jar"] | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "compileOnly"    | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE@jar", "org.springframework:spring-context:5.2.11.RELEASE@jar"] | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "runtimeOnly"    | BUILD_FAILURE  | []                                                                                                               | []
    }

    @Unroll
    def "aggregator dependency declared in config and not used in build with dedicated dependency should report missing dependency with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework:spring-context:5.2.11.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts                                 | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE@jar"] | []
        "implementation" | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE@jar"] | []
        "compileOnly"    | VIOLATIONS     | ["org.springframework:spring-beans:5.2.11.RELEASE@jar"] | []
        "runtimeOnly"    | BUILD_FAILURE  | []                                                      | []
    }

    @Unroll
    def "aggregator dependency declared in config and used in build together with dedicated dependency should report explicit dependency as unused with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework:spring-context:5.2.11.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('Dependent')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .withSubProject(subProject("dependent")
                        .withMainClass(new GroovyClass("Dependent")))
                .withDependency(new GradleDependency(configuration: configuration, project: "dependent"))
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS     | []                      | ["org.springframework:spring-context:5.2.11.RELEASE@jar"]
        "implementation" | VIOLATIONS     | []                      | ["org.springframework:spring-context:5.2.11.RELEASE@jar"]
        "compileOnly"    | VIOLATIONS     | []                      | ["org.springframework:spring-context:5.2.11.RELEASE@jar"]
        "runtimeOnly"    | BUILD_FAILURE  | []                      | []
    }

    @Unroll
    def "multiple aggregator dependencies declared and dependency available in both aggregators should choose the aggregator with less dependencies and report other as unused with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                )
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar"]
        "implementation" | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar"]
        "compileOnly"    | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar"]
        "runtimeOnly"    | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar"]
    }

    @Unroll
    def "multiple aggregator dependencies declared in config and dependency available in both aggregators plus one additional should choose aggregators with less dependencies with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.jdbc.BadSqlGrammarException')
                )
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "implementation" | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "compileOnly"    | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "runtimeOnly"    | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
    }

    @Unroll
    def "multiple aggregator dependencies declared in config and dependency available in both aggregators plus one additional should keep only used distinct aggregators with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter-jdbc:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('org.springframework.context.annotation.ComponentScan')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.jdbc.BadSqlGrammarException')
                        .usesClass('org.springframework.web.bind.annotation.RestController')
                )
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "implementation" | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "compileOnly"    | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "runtimeOnly"    | VIOLATIONS     | []                      | ["org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
    }

    @Unroll
    def "multiple aggregator dependencies declared in config and another aggregator is smaller with #configuration results in #expectedResult"(String configuration, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        setup:
        rootProject()
                .withMavenRepositories()
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE'))
                .withAggregator(new GradleDependency(configuration: 'permitAggregatorUse', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter:2.3.6.RELEASE'))
                .withDependency(new GradleDependency(configuration: 'compile', id: 'org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE'))
                .withMainClass(new GroovyClass('Main')
                        .usesClass('com.fasterxml.jackson.databind.ObjectMapper')
                        .usesClass('org.springframework.beans.factory.annotation.Autowired')
                        .usesClass('org.springframework.web.context.request.RequestContextHolder')
                )
                .create(projectDir.getRoot())

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        configuration    | expectedResult | usedUndeclaredArtifacts                                                 | unusedDeclaredArtifacts
        "compile"        | VIOLATIONS     | ["org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE@jar"] | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "implementation" | VIOLATIONS     | ["org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE@jar"] | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "compileOnly"    | VIOLATIONS     | ["org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE@jar"] | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
        "runtimeOnly"    | VIOLATIONS     | ["org.springframework.boot:spring-boot-starter-json:2.3.6.RELEASE@jar"] | ["org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE@jar", "org.springframework.boot:spring-boot-starter:2.3.6.RELEASE@jar"]
    }

    private BuildResult buildGradleProject(String expectedResult) {
        if (expectedResult == SUCCESS) {
            return gradleProject().build()
        }
        return gradleProject().buildAndFail()
    }

    private static void assertBuildResult(BuildResult result, String expectedResult) {
        assertBuildResult(result, expectedResult, [] as String[], [] as String[])
    }

    private static void assertBuildResult(BuildResult result, String expectedResult, String[] usedUndeclaredArtifacts, String[] unusedDeclaredArtifacts) {
        if (expectedResult == SUCCESS) {
            if (result.task(":build") == null) {
                throw new SpockAssertionError("Build task not run: \n${result.getOutput()}")
            }
            assert result.task(":build").getOutcome() == TaskOutcome.SUCCESS
        } else if (expectedResult == BUILD_FAILURE) {
            if (result.task(":compileGroovy") == null) {
                throw new SpockAssertionError("compileGroovy task not run: \n${result.getOutput()}")
            }
            assert result.task(":compileGroovy").getOutcome() == TaskOutcome.FAILED
        } else if (expectedResult == TEST_BUILD_FAILURE) {
            if (result.task(":compileTestGroovy") == null) {
                throw new SpockAssertionError("compileTestGroovy task not run: \n${result.getOutput()}")
            }
            assert result.task(":compileTestGroovy").getOutcome() == TaskOutcome.FAILED
        } else if (expectedResult == VIOLATIONS) {
            def violations = new StringBuilder("> Dependency analysis found issues.\n")
            if (usedUndeclaredArtifacts?.length > 0) {
                violations.append("  usedUndeclaredArtifacts: \n")
                usedUndeclaredArtifacts.each { violations.append("   - ${it}\n") }
            }
            if (unusedDeclaredArtifacts?.length > 0) {
                violations.append("  unusedDeclaredArtifacts: \n")
                unusedDeclaredArtifacts.each { violations.append("   - ${it}\n") }
            }
            violations.append("\n")
            assert result.output.contains(violations)
        } else {
            assert result.output.contains(expectedResult)
        }
    }

    private static GradleProject rootProject() {
        new GradleProject("project", true)
                .withPlugin("ca.cutterslade.analyze")
                .withDependency(new GradleDependency(configuration: "compile", reference: "localGroovy()"))
    }

    private static GradleProject subProject(String name) {
        new GradleProject(name)
                .withDependency(new GradleDependency(configuration: "compile", reference: "localGroovy()"))
    }

    private GradleRunner gradleProject() {
        GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withPluginClasspath()
                .withArguments("build")
    }

    private static void assertBuildSuccess(BuildResult result) {
        if (result.task(":build") == null) {
            throw new SpockAssertionError("Build task not run: \n${result.getOutput()}")
        }
        assert result.task(":build").getOutcome() == TaskOutcome.SUCCESS
    }
}

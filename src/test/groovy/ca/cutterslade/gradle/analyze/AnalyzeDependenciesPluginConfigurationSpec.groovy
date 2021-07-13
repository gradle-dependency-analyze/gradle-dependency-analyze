package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.helper.GradleDependency
import ca.cutterslade.gradle.analyze.helper.GroovyClass
import org.gradle.testkit.runner.BuildResult

class AnalyzeDependenciesPluginConfigurationSpec extends AnalyzeDependenciesPluginBaseSpec {
    def "forceConfigurationResolution task should not cause plugin failure"() {
        setup:
        rootProject()
                .withMavenRepositories()
                .withMainClass(new GroovyClass('Main'))
                .withPlugin('java-library')
                .withDependency(new GradleDependency(configuration: 'api', id: 'com.google.guava:guava:30.1-jre'))
                .withAdditionalTask("forceConfigurationResolution", """
                    // Minimal example from our internal plugins that force resolution early
                    import org.gradle.api.artifacts.Configuration
                    
                    tasks.register("forceConfigurationResolution") {
                      doLast() {
                        tasks.forEach { Task task ->
                          task.metaPropertyValues.forEach { PropertyValue prop ->
                            def value
                            try {
                              value = prop.value
                            } catch (e) {
                              // Log the failure, then skip over it
                              project.logger.debug("Failed to resolve property \${prop.name} on \${task.name}", e)
                              return
                            }
                    
                            // If this blows up, it should throw, not get swallowed up
                            if (value instanceof Configuration) {
                              // The return value is not important, just calling it will make some lazily initialized stuff happen
                              value.resolvedConfiguration
                            }
                          }
                        }
                      }
                    }
                    """.stripIndent())
                .create(projectDir)

        when:
        BuildResult result = buildGradleProject(expectedResult)

        then:
        assertBuildResult(result, expectedResult, usedUndeclaredArtifacts, unusedDeclaredArtifacts)

        where:
        expectedResult | usedUndeclaredArtifacts | unusedDeclaredArtifacts
        VIOLATIONS     | []                      | ["com.google.guava:guava:30.1-jre@jar"]
    }
}

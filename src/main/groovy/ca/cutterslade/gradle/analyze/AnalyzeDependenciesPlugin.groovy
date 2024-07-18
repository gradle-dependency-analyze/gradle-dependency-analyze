package ca.cutterslade.gradle.analyze

import static ca.cutterslade.gradle.analyze.util.ProjectDependencyResolverUtils.configureApiHelperConfiguration

import ca.cutterslade.gradle.analyze.util.GradleVersionUtil
import groovy.transform.CompileDynamic
import org.apache.commons.collections4.multimap.HashSetValuedHashMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.SourceSet
import org.gradle.util.GradleVersion

@CompileDynamic
class AnalyzeDependenciesPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        if (project.rootProject == project) {
            project.rootProject.extensions.add(ProjectDependencyResolver.CACHE_NAME, new HashSetValuedHashMap<>())
        }

        project.plugins.withId('java') {
            def commonTask = project.tasks.register('analyzeDependencies') {
                group = 'Verification'
                description = 'Analyze project for dependency issues.'
            }

            project.tasks.named('check').configure {
                it.dependsOn commonTask
            }

            project.sourceSets.all { SourceSet sourceSet ->
                project.configurations.create(sourceSet.getTaskName('permit', 'unusedDeclared')) {
                    it.canBeConsumed = false
                    it.canBeResolved = true
                }
                project.configurations.create(sourceSet.getTaskName('permit', 'usedUndeclared')) {
                    it.canBeConsumed = false
                    it.canBeResolved = true
                }
                project.configurations.create(sourceSet.getTaskName('permit', 'aggregatorUse')) {
                    it.canBeConsumed = false
                    it.canBeResolved = true
                    it.attributes {
                        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage, Usage.JAVA_API))
                    }
                }
                project.configurations.create(sourceSet.getTaskName('apiHelper', '')) {
                    it.canBeConsumed = false
                    it.canBeResolved = true
                }
                project.configurations.create(sourceSet.getTaskName('compileOnlyHelper', '')) {
                    it.canBeConsumed = false
                    it.canBeResolved = true
                }

                def analyzeTask = project.tasks.register(sourceSet.getTaskName('analyze', 'classesDependencies'), AnalyzeDependenciesTask) {
                    group = 'Verification'
                    description = "Analyze project for dependency issues related to ${sourceSet.name} source set."

                    doFirst {
                        final def configuration = project.configurations.findByName('providedRuntime')
                        if (configuration != null && !configuration.resolvedConfiguration.firstLevelModuleDependencies.empty) {
                            GradleVersionUtil.warnAboutWarPluginBrokenWhenUsingProvidedRuntime(GradleVersion.current(), project.logger)
                        }
                    }
                }

                commonTask.configure {
                    dependsOn analyzeTask
                }

                project.afterEvaluate {
                    analyzeTask.configure {
                        require = [
                                project.configurations.getByName(sourceSet.compileClasspathConfigurationName)
                        ]

                        compileOnly = configureApiHelperConfiguration(
                                project.configurations.getByName(sourceSet.getTaskName('compileOnlyHelper', '')),
                                project,
                                sourceSet.compileOnlyConfigurationName
                        )

                        apiHelperConfiguration = configureApiHelperConfiguration(
                                project.configurations.getByName(sourceSet.getTaskName('apiHelper', '')),
                                project,
                                sourceSet.apiConfigurationName
                        )
                        allowedAggregatorsToUse = [
                                project.configurations.getByName(sourceSet.getTaskName('permit', 'aggregatorUse'))
                        ]
                        allowedToUse = [
                                project.configurations.getByName(sourceSet.getTaskName('permit', 'usedUndeclared'))
                        ]
                        allowedToDeclare = [
                                project.configurations.getByName(sourceSet.getTaskName('permit', 'unusedDeclared'))
                        ]

                        if (sourceSet.name == 'test') {
                            allowedToUse.add(project.configurations.compileClasspath)
                            if (project.configurations.any { it.name == 'testFixturesCompileClasspath' })
                                allowedToUse.add(project.configurations.testFixturesCompileClasspath)
                        }
                        if (sourceSet.name == 'testFixtures') {
                            allowedToUse.add(project.configurations.testCompileClasspath)
                        }
                        classesDirs = sourceSet.output.classesDirs
                    }
                }
            }
        }
    }

}

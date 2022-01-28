package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.util.GradleVersionUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.SourceSet
import org.gradle.util.GradleVersion

import java.util.concurrent.ConcurrentHashMap

import static ca.cutterslade.gradle.analyze.util.ProjectDependencyResolverUtils.configureApiHelperConfiguration

class AnalyzeDependenciesPlugin implements Plugin<Project> {
    @Override
    void apply(final Project project) {
        if (project.rootProject == project) {
            project.rootProject.extensions.add(ProjectDependencyResolver.CACHE_NAME, new ConcurrentHashMap<>())
        }

        project.plugins.withId('java') {
            def commonTask = project.task('analyzeDependencies',
                    group: 'Verification',
                    description: 'Analyze project for dependency issues.'
            )

            project.tasks.check.dependsOn commonTask

            project.sourceSets.all { SourceSet sourceSet ->
                project.configurations.create(sourceSet.getTaskName('permit', 'unusedDeclared')) {
                    canBeConsumed = false
                    canBeResolved = true
                }
                project.configurations.create(sourceSet.getTaskName('permit', 'usedUndeclared')) {
                    canBeConsumed = false
                    canBeResolved = true
                }
                project.configurations.create(sourceSet.getTaskName('permit', 'aggregatorUse')) {
                    canBeConsumed = false
                    canBeResolved = true
                    attributes {
                        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage, Usage.JAVA_API))
                    }
                }
                project.configurations.create(sourceSet.getTaskName('apiHelper', '')) {
                    canBeConsumed = false
                    canBeResolved = true
                }
                project.configurations.create(sourceSet.getTaskName('compileOnlyHelper', '')) {
                    canBeConsumed = false
                    canBeResolved = true
                }

                def analyzeTask = project.task(sourceSet.getTaskName('analyze', 'classesDependencies'),
                        dependsOn: sourceSet.classesTaskName, // needed for pre-4.0, later versions infer this from classesDirs
                        type: AnalyzeDependenciesTask,
                        group: 'Verification',
                        description: "Analyze project for dependency issues related to ${sourceSet.name} source set.")

                commonTask.dependsOn analyzeTask

                project.afterEvaluate {
                    analyzeTask.configure {
                        final def configuration = project.configurations.findByName("providedRuntime")
                        if (configuration != null && !configuration.resolvedConfiguration.firstLevelModuleDependencies.empty) {
                            GradleVersionUtil.warnAboutWarPluginBrokenWhenUsingProvidedRuntime(GradleVersion.current(), project.logger)
                        }

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

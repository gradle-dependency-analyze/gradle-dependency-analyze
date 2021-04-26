package ca.cutterslade.gradle.analyze

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.SourceSet

import java.util.concurrent.ConcurrentHashMap

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

                def analyzeTask = project.task(sourceSet.getTaskName('analyze', 'classesDependencies'),
                        dependsOn: sourceSet.classesTaskName, // needed for pre-4.0, later versions infer this from classesDirs
                        type: AnalyzeDependenciesTask,
                        group: 'Verification',
                        description: "Analyze project for dependency issues related to ${sourceSet.name} source set.")

                commonTask.dependsOn analyzeTask

                project.afterEvaluate {
                    analyzeTask.configure {
                        require = [
                                project.configurations.getByName(sourceSet.compileClasspathConfigurationName)
                        ]
                        apiHelperConfiguration = project.configurations.getByName(sourceSet.getTaskName('apiHelper', ''))
                        apiConfigurationName = sourceSet.apiConfigurationName
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
                        def output = sourceSet.output
                        // classesDirs was defined in gradle 4.0
                        classesDirs = output.hasProperty('classesDirs') ? output.classesDirs : project.files(output.classesDir)
                    }
                }
            }
        }
    }
}

package ca.cutterslade.gradle.analyze

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

import java.util.concurrent.ConcurrentHashMap

class AnalyzeDependenciesPlugin implements Plugin<Project> {
  @Override
  void apply(final Project project) {
    if (project.rootProject == project) {
      project.rootProject.extensions.add(ProjectDependencyResolver.CACHE_NAME, new ConcurrentHashMap<>())
    }
    project.plugins.withId('java') {
      project.configurations.create('permitAggregatorUse')

      project.configurations.create('permitUnusedDeclared')
      project.configurations.create('permitTestUnusedDeclared')

      project.configurations.create('permitUsedUndeclared')
      project.configurations.create('permitTestUsedUndeclared')

      def mainTask = project.task('analyzeClassesDependencies',
          dependsOn: ['classes', project.configurations.compile],
          type: AnalyzeDependenciesTask,
          group: 'Verification',
          description: 'Analyze project for dependency issues related to main source set.'
      ) {
        allowedAggregatorsToUse = [
                project.rootProject.configurations.permitAggregatorUse
        ]
        require = [
            project.configurations.compile,
            project.configurations.findByName('compileOnly'),
            project.configurations.findByName('provided'),
            project.configurations.findByName('compileClasspath')
        ]
        allowedToUse = [
            project.configurations.permitUsedUndeclared
        ]
        allowedToDeclare = [
            project.configurations.permitUnusedDeclared
        ]
        def output = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output
        classesDirs = output.hasProperty('classesDirs') ? output.classesDirs : project.files(output.classesDir)
      }

      def testTask = project.task('analyzeTestClassesDependencies',
          dependsOn: ['testClasses', project.configurations.testCompile],
          type: AnalyzeDependenciesTask,
          group: 'Verification',
          description: 'Analyze project for dependency issues related to test source set.'
      ) {
        allowedAggregatorsToUse = [
                project.rootProject.configurations.permitAggregatorUse
        ]
        require = [
            project.configurations.testCompile,
            project.configurations.findByName('testCompileOnly'),
            project.configurations.findByName('testCompileClasspath')
        ]
        allowedToUse = [
            project.configurations.compile,
            project.configurations.permitTestUsedUndeclared,
            project.configurations.findByName('provided'),
            project.configurations.findByName('compileClasspath'),
            project.configurations.findByName('runtimeClasspath')
        ]
        allowedToDeclare = [
            project.configurations.permitTestUnusedDeclared
        ]
        def output = project.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).output
        classesDirs = output.hasProperty('classesDirs') ? output.classesDirs : project.files(output.classesDir)
      }

      project.check.dependsOn project.task('analyzeDependencies',
          dependsOn: [mainTask, testTask],
          group: 'Verification',
          description: 'Analyze project for dependency issues.'
      )
    }
  }
}

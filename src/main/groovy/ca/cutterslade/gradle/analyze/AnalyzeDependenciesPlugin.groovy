package ca.cutterslade.gradle.analyze

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class AnalyzeDependenciesPlugin implements Plugin<Project> {
  @Override
  void apply(final Project project) {
    project.task('analyzeDependencies')
    if (project.tasks['classes']) {
      project.task(dependsOn: 'classes', type: AnalyzeDependenciesTask, 'analyzeClassesDependencies') {
        require = [project.configurations.compile]
        allowedToDeclare = [project.configurations['provided']]
        classesDir = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output.classesDir
      }
      project.analyzeDependencies.dependsOn('analyzeClassesDependencies')
    }
    if (project.tasks['testClasses']) {
      project.task(dependsOn: 'testClasses', type: AnalyzeDependenciesTask, 'analyzeTestClassesDependencies') {
        require = [project.configurations.testCompile]
        allowedToUse = [project.configurations.compile]
        allowedToDeclare = [project.configurations['provided']]
        classesDir = project.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).output.classesDir
      }
      project.analyzeDependencies.dependsOn('analyzeTestClassesDependencies')
    }
    project.check.dependsOn('analyzeDependencies')
  }
}

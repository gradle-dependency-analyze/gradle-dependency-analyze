package ca.cutterslade.gradle.analyze

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class AnalyzeDependenciesPlugin implements Plugin<Project> {
  @Override
  void apply(final Project project) {
    project.configurations.create('permitUnusedDeclared')
    project.configurations.create('permitTestUnusedDeclared')
    project.task('analyzeDependencies')
    if (project.tasks['classes']) {
      project.task(dependsOn: 'classes', type: AnalyzeDependenciesTask, 'analyzeClassesDependencies') {
        require = [
            project.configurations.compile,
            project.configurations.findByName('compileOnly'),
            project.configurations.findByName('provided')
        ]
        allowedToDeclare = [
            project.configurations.permitUnusedDeclared
        ]
        classesDir = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output.classesDir
      }
      project.analyzeDependencies.dependsOn('analyzeClassesDependencies')
    }
    if (project.tasks['testClasses']) {
      project.task(dependsOn: 'testClasses', type: AnalyzeDependenciesTask, 'analyzeTestClassesDependencies') {
        require = [
            project.configurations.testCompile,
            project.configurations.findByName('testCompileOnly')
        ]
        allowedToUse = [
            project.configurations.compile,
            project.configurations.findByName('provided')
        ]
        allowedToDeclare = [
            project.configurations.permitTestUnusedDeclared
        ]
        classesDir = project.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).output.classesDir
      }
      project.analyzeDependencies.dependsOn('analyzeTestClassesDependencies')
    }
    project.check.dependsOn('analyzeDependencies')
  }
}

package ca.cutterslade.gradle.analyze

import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.lang.reflect.Method

class AnalyzeDependenciesTask extends DefaultTask {
  @Input
  boolean justWarn = false
  List<Configuration> require = []
  List<Configuration> allowedToUse = []
  List<Configuration> allowedToDeclare = []
  @InputFiles
  FileCollection classesDirs = project.files()
  @OutputFile
  File outputFile = project.file("$project.buildDir/dependency-analyze/result")

  AnalyzeDependenciesTask() {
    def methods = outputs.class.getMethods().grep {Method m -> m.name == 'cacheIf'}
    if (methods) {
      outputs.cacheIf({true})
    }
  }

  void setClassesDir(File classesDir) {
    this.classesDirs = project.files(classesDir)
  }

  @TaskAction
  def action() {
    project.logger.info "Analyzing dependencies of $classesDirs for [require: $require, allowedToUse: $allowedToUse, " +
        "allowedToDeclare: $allowedToDeclare]"
    ProjectDependencyAnalysis analysis =
        new ProjectDependencyResolver(project, require, allowedToUse, allowedToDeclare, classesDirs)
            .analyzeDependencies()
    StringBuffer buffer = new StringBuffer()
    ['usedUndeclaredArtifacts', 'unusedDeclaredArtifacts'].each {section ->
      def violations = analysis."$section"
      if (violations) {
        buffer.append("$section: \n")
        violations*.moduleVersion*.id.each {
          buffer.append(" - $it.group:$it.name:$it.version\n")
        }
      }
    }
    outputFile.parentFile.mkdirs()
    outputFile.text = buffer.toString()
    if (buffer) {
      def message = "Dependency analysis found issues.\n$buffer"
      if (justWarn) {
        project.logger.warn message
      }
      else {
        throw new DependencyAnalysisException(message)
      }
    }
  }

  @InputFiles
  FileCollection getAllArtifacts() {
    project.files({
      def files = ProjectDependencyResolver.removeNulls(
          ProjectDependencyResolver.removeNulls(require)
              *.resolvedConfiguration
              *.firstLevelModuleDependencies
              *.allModuleArtifacts
              *.file.flatten() as Set<File>
      )
      project.logger.info "All Artifact Files: $files"
      files
    })

  }

  @InputFiles
  FileCollection getRequiredFiles() {
    getFirstLevelFiles(require, 'required')
  }

  @InputFiles
  FileCollection getAllowedToUseFiles() {
    getFirstLevelFiles(allowedToUse, 'allowed to user')
  }

  @InputFiles
  FileCollection getAllowedToDeclareFiles() {
    getFirstLevelFiles(allowedToDeclare, 'allowed to declare')
  }

  private FileCollection getFirstLevelFiles(List<Configuration> configurations, String name) {
    project.files({
      def files = ProjectDependencyResolver.removeNulls(
          ProjectDependencyResolver.getFirstLevelDependencies(
              ProjectDependencyResolver.removeNulls(configurations)
          )*.allModuleArtifacts*.file.flatten()
      )
      project.logger.info "First level $name files: $files"
      files
    })
  }
}

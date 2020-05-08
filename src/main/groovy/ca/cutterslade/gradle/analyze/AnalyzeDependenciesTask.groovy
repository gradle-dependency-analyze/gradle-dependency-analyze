package ca.cutterslade.gradle.analyze

import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.lang.reflect.Method

class AnalyzeDependenciesTask extends DefaultTask {
  @Input
  boolean justWarn = false
  @InputFiles
  List<Configuration> require = []
  @InputFiles
  List<Configuration> allowedToUse = []
  @InputFiles
  List<Configuration> allowedToDeclare = []
  @InputFiles
  FileCollection classesDirs = project.files()
  @OutputFile
  File outputFile = project.file("$project.buildDir/reports/dependency-analyze/$name")

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
    logger.info "Analyzing dependencies of $classesDirs for [require: $require, allowedToUse: $allowedToUse, " +
        "allowedToDeclare: $allowedToDeclare]"
    ProjectDependencyAnalysis analysis =
        new ProjectDependencyResolver(project, require, allowedToUse, allowedToDeclare, classesDirs)
            .analyzeDependencies()
    StringBuffer buffer = new StringBuffer()
    ['usedUndeclaredArtifacts', 'unusedDeclaredArtifacts'].each {section ->
      def violations = analysis."$section"
      if (violations) {
        buffer.append("$section: \n")
        violations.sort { it.moduleVersion.id.toString() }.each { DefaultResolvedArtifact it ->
          def clas = it.classifier ? ":$it.classifier" : ""
          buffer.append(" - $it.moduleVersion.id$clas@$it.extension\n")
        }
      }
    }
    outputFile.parentFile.mkdirs()
    outputFile.text = buffer.toString()
    if (buffer) {
      def message = "Dependency analysis found issues.\n$buffer"
      if (justWarn) {
        logger.warn message
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
      logger.info "All Artifact Files: $files"
      files
    })
  }

  @InputFiles
  FileCollection getRequiredFiles() {
    project.files({
      def files = getFirstLevelFiles(require, 'required') -
          getFirstLevelFiles(allowedToUse, 'allowed to use')
      logger.info  "Actual required files: $files"
      files
    })
  }

  @InputFiles
  FileCollection getAllowedToUseFiles() {
    getFirstLevelFileCollection(allowedToUse, 'allowed to use')
  }

  @InputFiles
  FileCollection getAllowedToDeclareFiles() {
    getFirstLevelFileCollection(allowedToDeclare, 'allowed to declare')
  }

  private FileCollection getFirstLevelFileCollection(List<Configuration> configurations, String name) {
    project.files {getFirstLevelFiles(configurations, name)}
  }

  Set<File> getFirstLevelFiles(List<Configuration> configurations, String name) {
    Set<File> files = ProjectDependencyResolver.removeNulls(
        ProjectDependencyResolver.getFirstLevelDependencies(
            ProjectDependencyResolver.removeNulls(configurations)
        )*.moduleArtifacts*.file.flatten()
    )
    logger.info "First level $name files: $files"
    files
  }
}

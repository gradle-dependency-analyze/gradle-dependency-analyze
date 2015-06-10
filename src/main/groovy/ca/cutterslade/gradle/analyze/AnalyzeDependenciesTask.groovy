package ca.cutterslade.gradle.analyze

import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction

class AnalyzeDependenciesTask extends DefaultTask {
  boolean justWarn
  List<Configuration> require;
  List<Configuration> allowedToUse;
  List<Configuration> allowedToDeclare;
  File classesDir;

  @TaskAction
  def action() {
    project.logger.info "Analyzing dependencies of $classesDir for [require: $require, allowedToUse: $allowedToUse, " +
        "allowedToDeclare: $allowedToDeclare]"
    ProjectDependencyAnalysis analysis =
        new ProjectDependencyResolver(project.logger, require, allowedToUse, allowedToDeclare, classesDir)
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
    if (buffer) {
      def message = "Dependency analysis found issues: $buffer"
      if (justWarn) {
        project.logger.warn message
      }
      else {
        throw new DependencyAnalysisException(message)
      }
    }
  }
}

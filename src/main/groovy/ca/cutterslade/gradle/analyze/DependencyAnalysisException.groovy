package ca.cutterslade.gradle.analyze

import org.gradle.api.GradleException

class DependencyAnalysisException extends GradleException {
  DependencyAnalysisException(final String message) {
    super(message)
  }
}

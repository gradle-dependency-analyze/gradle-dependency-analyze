package ca.cutterslade.gradle.analyze;

import org.gradle.api.GradleException;

public class DependencyAnalysisException extends GradleException {
  public DependencyAnalysisException(final String message) {
    super(message);
  }
}

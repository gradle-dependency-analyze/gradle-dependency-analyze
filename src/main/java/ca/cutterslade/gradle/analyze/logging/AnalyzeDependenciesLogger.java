package ca.cutterslade.gradle.analyze.logging;

import ca.cutterslade.gradle.analyze.ProjectDependencyAnalysisResult;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.collections4.MultiValuedMap;
import org.gradle.api.logging.Logger;

public abstract class AnalyzeDependenciesLogger {
  public static ProjectDependencyAnalysisResult create(
      final Logger gradleLogger,
      final boolean logDependencyInformationToFiles,
      final Path logFilePath,
      final Function<AnalyzeDependenciesLogger, ProjectDependencyAnalysisResult> withLogger) {
    if (logDependencyInformationToFiles) {
      try (final AnalyzeDependenciesFileLogger logger =
          new AnalyzeDependenciesFileLogger(logFilePath)) {
        return withLogger.apply(logger);
      }
    } else {
      final AnalyzeDependenciesStandardLogger logger =
          new AnalyzeDependenciesStandardLogger(gradleLogger);
      return withLogger.apply(logger);
    }
  }

  public abstract void info(String title);

  public abstract void info(String title, Collection<?> files);

  public void info(String title, MultiValuedMap<?, ?> map) {
    info(title, map.asMap());
  }

  public abstract void info(String title, Map<?, ? extends Collection<?>> fileMap);
}

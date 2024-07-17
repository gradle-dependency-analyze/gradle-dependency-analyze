package ca.cutterslade.gradle.analyze.logging;

import ca.cutterslade.gradle.analyze.ProjectDependencyAnalysisResult;
import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import org.apache.commons.collections4.MultiValuedMap;
import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public abstract class AnalyzeDependenciesLogger {
    public static ProjectDependencyAnalysisResult create(final Logger gradleLogger,
                                                         final boolean logDependencyInformationToFiles,
                                                         final Path logFilePath,
                                                         @ClosureParams(value = SimpleType.class, options = "ca.cutterslade.gradle.analyze.logging.AnalyzeDependenciesLogger") final Closure<ProjectDependencyAnalysisResult> withLogger) {
        if (logDependencyInformationToFiles) {
            try (final AnalyzeDependenciesFileLogger logger = new AnalyzeDependenciesFileLogger(logFilePath)) {
                return withLogger.call(logger);
            }
        } else {
            final AnalyzeDependenciesStandardLogger logger = new AnalyzeDependenciesStandardLogger(gradleLogger);
            return withLogger.call(logger);
        }
    }

    public abstract void info(String title);

    public abstract void info(String title,
                              Collection<?> files);

    public void info(String title,
                     MultiValuedMap<?, ?> map) {
        info(title, map.asMap());
    }

    public abstract void info(String title,
                              Map<?, ? extends Collection<?>> fileMap);
}

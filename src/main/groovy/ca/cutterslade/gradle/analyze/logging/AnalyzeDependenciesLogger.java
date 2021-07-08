package ca.cutterslade.gradle.analyze.logging;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.gradle.api.logging.Logger;

import ca.cutterslade.gradle.analyze.ProjectDependencyAnalysisResult;
import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;

public abstract class AnalyzeDependenciesLogger {
    public abstract void info(String title);

    public abstract void info(String title,
                              Collection<?> files);

    public abstract void info(String title,
                              Map<File, Set<String>> fileMap);

    public static ProjectDependencyAnalysisResult create(final Logger gradleLogger,
                                                         final Path buildDirPath,
                                                         final boolean writeToFile,
                                                         @ClosureParams(value = SimpleType.class, options = "ca.cutterslade.gradle.analyze.logging.AnalyzeDependenciesLogger") final Closure<ProjectDependencyAnalysisResult> withLogger) {
        if (writeToFile) {
            try (final AnalyzeDependenciesFileLogger logger = new AnalyzeDependenciesFileLogger(buildDirPath)) {
                return withLogger.call(logger);
            }
        } else {
            final AnalyzeDependenciesStandardLogger logger = new AnalyzeDependenciesStandardLogger(gradleLogger);
            return withLogger.call(logger);
        }
    }
}

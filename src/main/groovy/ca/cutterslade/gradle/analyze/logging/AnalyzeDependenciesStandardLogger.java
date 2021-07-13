package ca.cutterslade.gradle.analyze.logging;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.gradle.api.logging.Logger;

public class AnalyzeDependenciesStandardLogger extends AnalyzeDependenciesLogger {
    private final Logger logger;

    public AnalyzeDependenciesStandardLogger(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(final String title) {
        logger.info(title);
    }

    @Override
    public void info(final String title, final Collection<?> files) {
        logger.info(title + " = " + files);
    }

    @Override
    public void info(final String title, final Map<File, Set<String>> fileMap) {
        logger.info(title + " = " + fileMap);
    }
}

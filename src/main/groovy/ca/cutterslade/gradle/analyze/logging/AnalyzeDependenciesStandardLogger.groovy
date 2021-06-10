package ca.cutterslade.gradle.analyze.logging

import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger

@CompileStatic
class AnalyzeDependenciesStandardLogger extends AnalyzeDependenciesLogger {
    private final Logger logger

    AnalyzeDependenciesStandardLogger(final Logger logger) {
        this.logger = logger
    }

    @Override
    void info(final String title) {
        logger.info title
    }

    @Override
    void info(final String title, final Collection<?> files) {
        logger.info "${title} = ${files}"
    }

    @Override
    void info(final String title, final Map<File, Set<String>> fileMap) {
        logger.info "${title} = ${fileMap}"
    }
}

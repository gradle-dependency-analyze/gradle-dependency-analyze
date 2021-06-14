package ca.cutterslade.gradle.analyze.logging

import ca.cutterslade.gradle.analyze.AnalyzeDependenciesTask
import groovy.transform.CompileStatic

import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
class AnalyzeDependenciesFileLogger extends AnalyzeDependenciesLogger implements AutoCloseable {
    private PrintWriter writer

    AnalyzeDependenciesFileLogger(final Path buildDirPath) {
        final def outputDirectoryPath = buildDirPath.resolve(AnalyzeDependenciesTask.DEPENDENCY_ANALYZE_DEPENDENCY_DIRECTORY_NAME)
        Files.createDirectories(outputDirectoryPath)
        final def analyzeOutputPath = outputDirectoryPath.resolve('analyzeDependencies.log')
        writer = new PrintWriter(Files.newOutputStream(analyzeOutputPath))
    }

    @Override
    void info(final String title) {
        writer.println(title)
        writer.println()
    }

    @Override
    void info(final String title, final Collection<?> files) {
        writer.println(title)
        files.collect { "- ${it instanceof File ? it.name : it}" }.sort(false).forEach({ writer.println(it) })
        writer.println()
    }

    @Override
    void info(final String title, final Map<File, Set<String>> fileMap) {
        writer.println(title)
        fileMap.toSorted({ it.key.toString() }).forEach({ key, value ->
            writer.println("- ${key.name}")
            value.collect { "  - ${it}" }.sort(false).forEach({ writer.println(it) })
        })
        writer.println()
    }

    @Override
    void close() throws Exception {
        writer.close()
    }
}

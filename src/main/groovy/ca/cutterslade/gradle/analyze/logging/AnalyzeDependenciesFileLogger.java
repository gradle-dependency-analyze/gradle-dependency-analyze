package ca.cutterslade.gradle.analyze.logging;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class AnalyzeDependenciesFileLogger extends AnalyzeDependenciesLogger implements AutoCloseable {
    private final PrintWriter writer;

    public AnalyzeDependenciesFileLogger(final Path logFilePath) {
        try {
            Files.createDirectories(logFilePath.getParent());
            writer = new PrintWriter(Files.newOutputStream(logFilePath));
        } catch (final Exception e) {
            throw new RuntimeException("unable to create file for logging", e);
        }
    }

    private static String getString(Object f) {
        return f instanceof File ? ((File) f).getName() : f.toString();
    }

    @Override
    public void info(final String title) {
        writer.println(title.trim());
        writer.println();
    }

    @Override
    public void info(final String title, final Collection<?> files) {
        writer.println(title.trim());
        files.stream()
                .filter(Objects::nonNull)
                .map(AnalyzeDependenciesFileLogger::getString)
                .map(s -> "- " + s)
                .sorted()
                .forEach(writer::println);
        writer.println();
    }

    @Override
    public void info(final String title, final Map<?, ? extends Collection<?>> fileMap) {
        writer.println(title.trim());
        fileMap.entrySet().stream()
                .sorted(Comparator.comparing(e -> getString(e.getKey())))
                .forEach(e -> {
                    writer.println("- " + getString(e.getKey()));
                    e.getValue().stream()
                            .sorted()
                            .map(s -> "  - " + getString(s))
                            .forEach(writer::println);
                });
        writer.println();
    }

    @Override
    public void close() {
        writer.close();
    }
}

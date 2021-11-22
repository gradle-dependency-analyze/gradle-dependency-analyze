package ca.cutterslade.gradle.analyze.logging;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
                .map(f -> f instanceof File ? ((File) f).getName() : f.toString())
                .map(s -> "- " + s)
                .sorted()
                .forEach(writer::println);
        writer.println();
    }

    @Override
    public void info(final String title, final Map<File, Set<String>> fileMap) {
        writer.println(title.trim());
        fileMap.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getName()))
                .forEach(e -> {
                    writer.println("- " + e.getKey().getName());
                    e.getValue().stream()
                            .sorted()
                            .map(s -> "  - " + s)
                            .forEach(writer::println);
                });
        writer.println();
    }

    @Override
    public void close() {
        writer.close();
    }
}

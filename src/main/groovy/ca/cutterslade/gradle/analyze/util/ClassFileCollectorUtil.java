package ca.cutterslade.gradle.analyze.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ClassFileCollectorUtil {
    private static final String classSuffix = ".class";

    private ClassFileCollectorUtil() {
    }

    public static Set<String> collectFromFile(final File file) throws IOException {
        final Set<String> classFiles = new HashSet<>();
        if (file.getPath().endsWith(".jar") || file.getPath().endsWith(".nar")) {
            collectFormJar(file, classFiles);
        } else if (!file.getPath().endsWith(".pom")) {
            if (file.isDirectory()) {
                collectFromDirectory(file, classFiles);
            } else if (file.exists()) {
                throw new IllegalArgumentException("Unsupported file for collecting classes: " + file.getPath());
            }
        }
        return classFiles;
    }


    private static void collectFormJar(final File jarFile, final Set<String> classFiles) throws IOException {
        try (final FileInputStream fis = new FileInputStream(jarFile);
             final JarInputStream jis = new JarInputStream(fis)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                addToClassFilesIfMatches(entry.getName(), classFiles);
            }
        }
    }

    private static void collectFromDirectory(final File directory, final Set<String> classFiles) {
        List<Path> classes;
        final Path directoryPath = directory.toPath();
        try (Stream<Path> walk = Files.walk(directoryPath)) {
            classes = walk.filter(path -> path.getFileName().toString().endsWith(classSuffix))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("%s from directory = %s", e.getMessage(), directoryPath), e);
        }

        for (final Path path : classes) {
            addToClassFilesIfMatches(directoryPath.relativize(path).toString().replace(File.separatorChar, '/'), classFiles);
        }
    }

    private static void addToClassFilesIfMatches(final String fullQualifiedName, final Set<String> classFiles) {
        if (fullQualifiedName.endsWith(classSuffix) && fullQualifiedName.indexOf('-') == -1) {
            classFiles.add(fullQualifiedName.substring(0, fullQualifiedName.length() - classSuffix.length()).replace('/', '.'));
        }
    }
}

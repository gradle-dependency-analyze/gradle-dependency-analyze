package ca.cutterslade.gradle.analyze.util;

import ca.cutterslade.gradle.analyze.util.JavaUtil.LinkedHashSetValuedLinkedHashMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.logging.Logger;

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
        } else {
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

    /**
     * Map each of the files declared on all configurations of the project to a collection of the class names they
     * contain.
     *
     * @param logger              logger
     * @param cache               cache for file to containing classes that were found and have been analyzed
     * @param dependencyArtifacts component identifiers for dependencies with their file locations
     * @return a Map of files to their classes
     * @throws IOException file could not be analyzed for class files
     */
    public static MultiValuedMap<ComponentIdentifier, String> buildArtifactClassMap(final Logger logger,
                                                                                    final MultiValuedMap<File, String> cache,
                                                                                    final MultiValuedMap<ComponentIdentifier, File> dependencyArtifacts) throws IOException {
        final MultiValuedMap<ComponentIdentifier, String> artifactClassMap = new LinkedHashSetValuedLinkedHashMap<>();

        int hits = 0;
        int misses = 0;

        final MapIterator<ComponentIdentifier, File> iterator = dependencyArtifacts.mapIterator();
        while (iterator.hasNext()) {
            final ComponentIdentifier identifier = iterator.next();
            final File file = iterator.getValue();
            if (cache.containsKey(file)) {
                logger.debug("Artifact class cache hit for {}", file);
                hits++;
            } else {
                logger.debug("Artifact class cache miss for {}", file);
                misses++;
                final Set<String> classes = collectFromFile(file);
                cache.putAll(file, classes);
            }
            artifactClassMap.putAll(identifier, cache.get(file));
        }
        logger.info("Built artifact class map with {} hits and {} misses; cache size is {}", hits, misses, cache.size());
        return artifactClassMap;
    }
}

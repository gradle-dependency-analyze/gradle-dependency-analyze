package ca.cutterslade.gradle.analyze.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.codehaus.plexus.util.DirectoryScanner;

public final class ClassFileCollectorUtil {
    private static final String classSuffix = ".class";
    private static final String[] CLASS_INCLUDES = {"**/*" + classSuffix};

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
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(directory);
        scanner.setIncludes(CLASS_INCLUDES);
        scanner.scan();

        for (String path : scanner.getIncludedFiles()) {
            path = path.replace(File.separatorChar, '/');
            addToClassFilesIfMatches(path, classFiles);
        }
    }

    private static void addToClassFilesIfMatches(final String fullQualifiedName, final Set<String> classFiles) {
        if (fullQualifiedName.endsWith(classSuffix) && fullQualifiedName.indexOf('-') == -1) {
            classFiles.add(fullQualifiedName.substring(0, fullQualifiedName.length() - classSuffix.length()).replace('/', '.'));
        }
    }
}
package ca.cutterslade.gradle.analyze.util;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;

public final class ProjectDependencyResolverUtils {
    private ProjectDependencyResolverUtils() {
    }

    /**
     * Determine which of the project dependencies are used.
     *
     * @param artifactClassMap  a map of Files to the classes they contain
     * @param dependencyClasses all classes used directly by the project
     * @return a map of artifact files to used classes in the project
     */
    public static Map<File, Set<String>> buildUsedArtifacts(final Map<File, Set<String>> artifactClassMap,
                                                            final Collection<String> dependencyClasses) {
        final Map<File, Set<String>> map = new HashMap<>();
        dependencyClasses.forEach(className ->
                artifactClassMap.entrySet().stream()
                        .filter(e -> e.getValue().contains(className))
                        .findFirst()
                        .ifPresent(e -> map.computeIfAbsent(e.getKey(), f -> new HashSet<>()).add(className))
        );
        return map;
    }

    public static Set<ResolvedArtifact> resolveArtifacts(final Collection<Configuration> configurations) {
        return configurations.stream()
                .map(Configuration::getResolvedConfiguration)
                .map(ResolvedConfiguration::getFirstLevelModuleDependencies)
                .flatMap(Collection::stream)
                .map(ResolvedDependency::getAllModuleArtifacts)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public static Set<ResolvedDependency> getFirstLevelDependencies(final Collection<Configuration> configurations) {
        return configurations.stream()
                .map(Configuration::getResolvedConfiguration)
                .map(ResolvedConfiguration::getFirstLevelModuleDependencies)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public static Set<File> findModuleArtifactFiles(final Collection<ResolvedDependency> dependencies) {
        return dependencies.stream()
                .map(ResolvedDependency::getModuleArtifacts)
                .flatMap(Collection::stream)
                .map(ResolvedArtifact::getFile)
                .collect(Collectors.toSet());
    }

    public static Set<File> findAllModuleArtifactFiles(final Collection<ResolvedDependency> dependencies) {
        return dependencies.stream()
                .map(ResolvedDependency::getAllModuleArtifacts)
                .flatMap(Collection::stream)
                .map(ResolvedArtifact::getFile)
                .collect(Collectors.toSet());
    }
}

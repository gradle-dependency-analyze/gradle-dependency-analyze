package ca.cutterslade.gradle.analyze.util;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;

import ca.cutterslade.gradle.analyze.logging.AnalyzeDependenciesLogger;

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
        final Map<File, Set<String>> map = new LinkedHashMap<>();
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

    public static List<ResolvedDependency> getFirstLevelDependencies(final Collection<Configuration> configurations) {
        return configurations.stream()
                .map(Configuration::getResolvedConfiguration)
                .map(ResolvedConfiguration::getFirstLevelModuleDependencies)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<File> findModuleArtifactFiles(final Collection<ResolvedDependency> dependencies) {
        return dependencies.stream()
                .map(ResolvedDependency::getModuleArtifacts)
                .flatMap(Collection::stream)
                .map(ResolvedArtifact::getFile)
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<File> findAllModuleArtifactFiles(final Collection<ResolvedDependency> dependencies) {
        return dependencies.stream()
                .map(ResolvedDependency::getAllModuleArtifacts)
                .flatMap(Collection::stream)
                .map(ResolvedArtifact::getFile)
                .distinct()
                .collect(Collectors.toList());
    }

    public static Map<ResolvedArtifact, Collection<ResolvedArtifact>> used(final List<ComponentIdentifier> allDependencyArtifacts,
                                                                           final Set<File> usedArtifacts,
                                                                           final Map<ResolvedArtifact, Set<ResolvedArtifact>> aggregatorsWithDependencies,
                                                                           final AnalyzeDependenciesLogger logger) {
        final Map<ResolvedArtifact, Collection<ResolvedArtifact>> usedAggregators = aggregatorsWithDependencies.entrySet().stream()
                .filter(e -> allDependencyArtifacts.contains(e.getKey().getId().getComponentIdentifier()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            final Set<File> filesForAggregator = e.getValue().stream()
                                    .map(ResolvedArtifact::getFile)
                                    .distinct()
                                    .filter(usedArtifacts::contains)
                                    .collect(Collectors.toSet());

                            return e.getValue().stream()
                                    .filter(f -> filesForAggregator.contains(f.getFile()))
                                    .collect(Collectors.toSet());
                        }
                ));

        return removeDuplicates(usedAggregators, aggregatorsWithDependencies, logger);
    }


    private static Map<ResolvedArtifact, Collection<ResolvedArtifact>> removeDuplicates(final Map<ResolvedArtifact, Collection<ResolvedArtifact>> usedAggregators,
                                                                                        final Map<ResolvedArtifact, Set<ResolvedArtifact>> aggregatorsWithDependencies,
                                                                                        final AnalyzeDependenciesLogger logger) {
        final Map<ResolvedArtifact, Collection<ResolvedArtifact>> aggregatorsSortedByDependencies = usedAggregators.entrySet().stream()
                .sorted(Map.Entry.<ResolvedArtifact, Collection<ResolvedArtifact>>comparingByValue(Comparator.comparingInt(Collection::size))
                        .thenComparing(Map.Entry.comparingByKey(Comparator.<ResolvedArtifact>comparingInt(k -> aggregatorsWithDependencies.get(k).size()).reversed())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a1, a2) -> a2, LinkedHashMap::new));

        final Set<ResolvedArtifact> aggregatorArtifactAlreadySeen = new HashSet<>();

        aggregatorsSortedByDependencies.entrySet().removeIf(e -> {
            aggregatorArtifactAlreadySeen.add(e.getKey());
            return aggregatorsSortedByDependencies.entrySet().stream()
                    .anyMatch(e2 -> !aggregatorArtifactAlreadySeen.contains(e2.getKey()) && e2.getValue().containsAll(e.getValue()));
        });

        logger.info("used aggregators", aggregatorsSortedByDependencies.keySet());
        return aggregatorsSortedByDependencies;
    }

    public static Map<ResolvedArtifact, Set<ResolvedArtifact>> getAggregatorsMapping(final Collection<Configuration> allowedAggregatorsToUse) {
        if (allowedAggregatorsToUse.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final Map<String, ResolvedArtifact> resolvedArtifacts = resolveArtifacts(allowedAggregatorsToUse).stream()
                    .collect(Collectors.toMap(d -> d.getModuleVersion().toString(), Function.identity()));
            final List<ResolvedDependency> dependencies = getFirstLevelDependencies(allowedAggregatorsToUse);
            return dependencies.stream()
                    .filter(d -> resolvedArtifacts.containsKey(d.getName()))
                    .collect(Collectors.toMap(d -> resolvedArtifacts.get(d.getName()), ResolvedDependency::getAllModuleArtifacts));
        }
    }

    public static List<Configuration> configureApiHelperConfiguration(final Configuration apiHelperConfiguration,
                                                                      final Project project,
                                                                      final String apiConfigurationName) {
        final Configuration apiConfiguration = project.getConfigurations().findByName(apiConfigurationName);
        if (apiConfiguration != null) {
            apiHelperConfiguration.extendsFrom(apiConfiguration);
            return Collections.singletonList(apiHelperConfiguration);
        } else {
            return Collections.emptyList();
        }
    }

    public static <T> Collection<T> removeNulls(final Collection<T> collection) {
        if (collection == null) {
            return Collections.emptyList();
        } else {
            collection.removeIf(Objects::isNull);
            return collection;
        }
    }
}

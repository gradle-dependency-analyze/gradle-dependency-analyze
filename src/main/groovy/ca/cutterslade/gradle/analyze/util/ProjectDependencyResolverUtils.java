package ca.cutterslade.gradle.analyze.util;

import ca.cutterslade.gradle.analyze.logging.AnalyzeDependenciesLogger;
import ca.cutterslade.gradle.analyze.util.JavaUtil.LinkedHashSetValuedLinkedHashMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ComponentIdentifier;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static ca.cutterslade.gradle.analyze.util.JavaUtil.toMultiValuedMap;

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
    public static MultiValuedMap<ComponentIdentifier, String> buildUsedArtifacts(final MultiValuedMap<ComponentIdentifier, String> artifactClassMap,
                                                                                 final Collection<String> dependencyClasses) {
        final MultiValuedMap<ComponentIdentifier, String> map = new LinkedHashSetValuedLinkedHashMap<>();
        dependencyClasses.forEach(className ->
                artifactClassMap.asMap().entrySet().stream()
                        .filter(e -> e.getValue().contains(className))
                        .findFirst()
                        .ifPresent(e -> map.put(e.getKey(), className))
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

    public static MultiValuedMap<ComponentIdentifier, File> findModuleArtifactFiles(final Collection<ResolvedDependency> dependencies) {
        return dependencies.stream()
                .map(ResolvedDependency::getModuleArtifacts)
                .flatMap(Collection::stream)
                .collect(toMultiValuedMap(a -> a.getId().getComponentIdentifier(), ResolvedArtifact::getFile));
    }

    public static MultiValuedMap<ComponentIdentifier, File> findAllModuleArtifactFiles(final Collection<ResolvedDependency> dependencies) {
        return dependencies.stream()
                .map(ResolvedDependency::getAllModuleArtifacts)
                .flatMap(Collection::stream)
                .collect(toMultiValuedMap(a -> a.getId().getComponentIdentifier(), ResolvedArtifact::getFile));
    }

    public static Map<ComponentIdentifier, Collection<ComponentIdentifier>> used(final Set<ComponentIdentifier> allDependencyArtifacts,
                                                                                 final Set<ComponentIdentifier> usedArtifacts,
                                                                                 final Map<ComponentIdentifier, Set<ComponentIdentifier>> aggregatorsWithDependencies,
                                                                                 final AnalyzeDependenciesLogger logger) {
        final Map<ComponentIdentifier, Collection<ComponentIdentifier>> usedAggregators = aggregatorsWithDependencies.entrySet().stream()
                .filter(e -> allDependencyArtifacts.contains(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            final Set<ComponentIdentifier> ComponentsForAggregator = e.getValue().stream()
                                    .filter(usedArtifacts::contains)
                                    .collect(Collectors.toSet());

                            return e.getValue().stream()
                                    .filter(ComponentsForAggregator::contains)
                                    .collect(Collectors.toSet());
                        }
                ));

        return removeDuplicates(usedAggregators, aggregatorsWithDependencies, logger);
    }


    private static Map<ComponentIdentifier, Collection<ComponentIdentifier>> removeDuplicates(final Map<ComponentIdentifier, Collection<ComponentIdentifier>> usedAggregators,
                                                                                              final Map<ComponentIdentifier, Set<ComponentIdentifier>> aggregatorsWithDependencies,
                                                                                              final AnalyzeDependenciesLogger logger) {
        final Map<ComponentIdentifier, Collection<ComponentIdentifier>> aggregatorsSortedByDependencies = usedAggregators.entrySet().stream()
                .sorted(Map.Entry.<ComponentIdentifier, Collection<ComponentIdentifier>>comparingByValue(Comparator.comparingInt(Collection::size))
                        .thenComparing(Map.Entry.comparingByKey(Comparator.<ComponentIdentifier>comparingInt(k -> aggregatorsWithDependencies.get(k).size()).reversed())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a1, a2) -> a2, LinkedHashMap::new));

        final Set<ComponentIdentifier> aggregatorArtifactAlreadySeen = new HashSet<>();

        aggregatorsSortedByDependencies.entrySet().removeIf(e -> {
            aggregatorArtifactAlreadySeen.add(e.getKey());
            return aggregatorsSortedByDependencies.entrySet().stream()
                    .anyMatch(e2 -> !aggregatorArtifactAlreadySeen.contains(e2.getKey()) && e2.getValue().containsAll(e.getValue()));
        });

        logger.info("used aggregators", aggregatorsSortedByDependencies.keySet());
        return aggregatorsSortedByDependencies;
    }

    public static Map<ComponentIdentifier, Set<ComponentIdentifier>> getAggregatorsMapping(final Collection<Configuration> allowedAggregatorsToUse) {
        if (allowedAggregatorsToUse.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final Map<ModuleVersionIdentifier, ComponentIdentifier> resolvedArtifacts = resolveArtifacts(allowedAggregatorsToUse).stream()
                    .collect(Collectors.toMap(d-> d.getModuleVersion().getId(), d -> d.getId().getComponentIdentifier()));

            final List<ResolvedDependency> dependencies = getFirstLevelDependencies(allowedAggregatorsToUse);
            return dependencies.stream()
                    .filter(d -> resolvedArtifacts.containsKey(d.getModule().getId()))
                    .collect(Collectors.toMap(d -> resolvedArtifacts.get(d.getModule().getId()), d -> d.getAllModuleArtifacts().stream()
                            .map(a -> a.getId().getComponentIdentifier())
                            .collect(Collectors.toSet())));
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

package ca.cutterslade.gradle.analyze.util;

import static ca.cutterslade.gradle.analyze.util.JavaUtil.toMultiValuedMap;

import ca.cutterslade.gradle.analyze.logging.AnalyzeDependenciesLogger;
import ca.cutterslade.gradle.analyze.util.JavaUtil.LinkedHashSetValuedLinkedHashMap;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MultiValuedMap;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;

public final class ProjectDependencyResolverUtils {
  private ProjectDependencyResolverUtils() {}

  /**
   * Determine which of the project dependencies are used.
   *
   * @param artifactClassMap a map of Files to the classes they contain
   * @param dependencyClasses all classes used directly by the project
   * @return a map of artifact files to used classes in the project
   */
  public static MultiValuedMap<ComponentIdentifier, String> buildUsedArtifacts(
      final MultiValuedMap<ComponentIdentifier, String> artifactClassMap,
      final Collection<String> dependencyClasses) {
    final MultiValuedMap<ComponentIdentifier, String> map =
        new LinkedHashSetValuedLinkedHashMap<>();
    dependencyClasses.forEach(
        className ->
            artifactClassMap.asMap().entrySet().stream()
                .filter(e -> e.getValue().contains(className))
                .findFirst()
                .ifPresent(e -> map.put(e.getKey(), className)));
    return map;
  }

  /**
   * Resolves artifacts from a collection of Configuration providers.
   *
   * @param configurationProviders a collection of Configuration providers
   * @return a set of resolved artifacts
   */
  public static Set<ResolvedArtifact> resolveArtifacts(
      final Collection<Provider<Configuration>> configurationProviders) {
    return configurationProviders.stream()
        .map(Provider::get) // Resolution happens here, when actually needed
        .map(Configuration::getResolvedConfiguration)
        .map(ResolvedConfiguration::getFirstLevelModuleDependencies)
        .flatMap(Collection::stream)
        .map(ResolvedDependency::getAllModuleArtifacts)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Gets first level dependencies from a collection of Configuration providers.
   *
   * @param configurationProviders a collection of Configuration providers
   * @return a list of resolved dependencies
   */
  public static List<ResolvedDependency> getFirstLevelDependencies(
      final Collection<Provider<Configuration>> configurationProviders) {
    return configurationProviders.stream()
        .map(Provider::get) // Resolution happens here, when actually needed
        .map(Configuration::getResolvedConfiguration)
        .map(ResolvedConfiguration::getFirstLevelModuleDependencies)
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());
  }

  public static MultiValuedMap<ComponentIdentifier, File> findModuleArtifactFiles(
      final Collection<ResolvedDependency> dependencies) {
    return dependencies.stream()
        .map(ResolvedDependency::getModuleArtifacts)
        .flatMap(Collection::stream)
        .collect(
            toMultiValuedMap(a -> a.getId().getComponentIdentifier(), ResolvedArtifact::getFile));
  }

  public static MultiValuedMap<ComponentIdentifier, File> findAllModuleArtifactFiles(
      final Collection<ResolvedDependency> dependencies) {
    return dependencies.stream()
        .map(ResolvedDependency::getAllModuleArtifacts)
        .flatMap(Collection::stream)
        .collect(
            toMultiValuedMap(a -> a.getId().getComponentIdentifier(), ResolvedArtifact::getFile));
  }

  public static Map<ComponentIdentifier, Collection<ComponentIdentifier>> used(
      final Set<ComponentIdentifier> allDependencyArtifacts,
      final Set<ComponentIdentifier> usedArtifacts,
      final Map<ComponentIdentifier, Set<ComponentIdentifier>> parentDependenciesMap,
      final AnalyzeDependenciesLogger logger) {
    final Map<ComponentIdentifier, Collection<ComponentIdentifier>> usedParents =
        parentDependenciesMap.entrySet().stream()
            .filter(e -> allDependencyArtifacts.contains(e.getKey()))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      final Set<ComponentIdentifier> componentsForParent =
                          e.getValue().stream()
                              .filter(usedArtifacts::contains)
                              .collect(Collectors.toSet());

                      return e.getValue().stream()
                          .filter(componentsForParent::contains)
                          .collect(Collectors.toSet());
                    }));

    return removeDuplicates(usedParents, parentDependenciesMap, logger);
  }

  private static Map<ComponentIdentifier, Collection<ComponentIdentifier>> removeDuplicates(
      final Map<ComponentIdentifier, Collection<ComponentIdentifier>> usedParents,
      final Map<ComponentIdentifier, Set<ComponentIdentifier>> parentDependenciesMap,
      final AnalyzeDependenciesLogger logger) {
    final Map<ComponentIdentifier, Collection<ComponentIdentifier>> parentsSortedByDependencies =
        usedParents.entrySet().stream()
            .sorted(
                Map.Entry.<ComponentIdentifier, Collection<ComponentIdentifier>>comparingByValue(
                        Comparator.comparingInt(Collection::size))
                    .thenComparing(
                        Map.Entry.comparingByKey(
                            Comparator.<ComponentIdentifier>comparingInt(
                                    k -> parentDependenciesMap.get(k).size())
                                .reversed())))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (a1, a2) -> a2, LinkedHashMap::new));

    final Set<ComponentIdentifier> parentArtifactAlreadySeen = new HashSet<>();

    parentsSortedByDependencies
        .entrySet()
        .removeIf(
            e -> {
              parentArtifactAlreadySeen.add(e.getKey());
              return parentsSortedByDependencies.entrySet().stream()
                  .anyMatch(
                      e2 ->
                          !parentArtifactAlreadySeen.contains(e2.getKey())
                              && e2.getValue().containsAll(e.getValue()));
            });

    logger.info("used aggregators", parentsSortedByDependencies.keySet());
    return parentsSortedByDependencies;
  }

  /**
   * Maps POM dependencies to their dependencies.
   *
   * @param configurationProviders a collection of Configuration providers
   * @param logger logger
   * @return a map of POM component identifiers to their dependencies
   */
  public static Map<ComponentIdentifier, Set<ComponentIdentifier>> getPomsWithDependenciesMapping(
      final List<Provider<Configuration>> configurationProviders, final Logger logger) {
    if (configurationProviders.isEmpty()) {
      return Collections.emptyMap();
    }

    // First detect all POM dependencies
    final Set<ModuleComponentIdentifier> pomDependencies =
        new PomDependencyDetector(logger).processDependencies(configurationProviders);

    // For each POM dependency, get all of its dependencies
    final Map<ComponentIdentifier, Set<ComponentIdentifier>> pomsWithDeps = new HashMap<>();

    // Get all first-level dependencies to examine
    final List<ResolvedDependency> allDependencies =
        getFirstLevelDependencies(configurationProviders);

    // For each POM dependency, find its corresponding ResolvedDependency and add its dependencies
    for (final ModuleComponentIdentifier pomId : pomDependencies) {
      for (final ResolvedDependency dependency : allDependencies) {
        // Use the module coordinates to match dependencies
        final ModuleVersionIdentifier moduleId = dependency.getModule().getId();

        if (pomId.getGroup().equals(moduleId.getGroup())
            && pomId.getModule().equals(moduleId.getName())
            && pomId.getVersion().equals(moduleId.getVersion())) {

          // Found matching dependency, add all its dependencies
          final Set<ComponentIdentifier> deps =
              dependency.getChildren().stream()
                  .flatMap(child -> child.getAllModuleArtifacts().stream())
                  .map(artifact -> artifact.getId().getComponentIdentifier())
                  .collect(Collectors.toSet());

          pomsWithDeps.put(pomId, deps);
          break;
        }
      }
    }

    return pomsWithDeps;
  }

  /**
   * Gets aggregators mapping from a collection of Configuration providers.
   *
   * @param configurationProviders a collection of Configuration providers
   * @return a map of component identifiers to their dependencies
   */
  public static Map<ComponentIdentifier, Set<ComponentIdentifier>> getAggregatorsMapping(
      final List<Provider<Configuration>> configurationProviders) {
    if (configurationProviders.isEmpty()) {
      return Collections.emptyMap();
    } else {
      // Resolve providers only when needed
      final Map<ModuleVersionIdentifier, ComponentIdentifier> resolvedArtifacts =
          resolveArtifacts(configurationProviders).stream()
              .collect(
                  Collectors.toMap(
                      d -> d.getModuleVersion().getId(), d -> d.getId().getComponentIdentifier()));

      final List<ResolvedDependency> dependencies =
          getFirstLevelDependencies(configurationProviders);
      return dependencies.stream()
          .filter(d -> resolvedArtifacts.containsKey(d.getModule().getId()))
          .collect(
              Collectors.toMap(
                  d -> resolvedArtifacts.get(d.getModule().getId()),
                  d ->
                      d.getAllModuleArtifacts().stream()
                          .map(a -> a.getId().getComponentIdentifier())
                          .collect(Collectors.toSet())));
    }
  }
}

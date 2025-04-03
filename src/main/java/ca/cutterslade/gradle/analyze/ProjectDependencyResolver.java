package ca.cutterslade.gradle.analyze;

import static ca.cutterslade.gradle.analyze.util.ClassFileCollectorUtil.buildArtifactClassMap;
import static ca.cutterslade.gradle.analyze.util.JavaUtil.*;
import static ca.cutterslade.gradle.analyze.util.ProjectDependencyResolverUtils.*;

import ca.cutterslade.gradle.analyze.logging.AnalyzeDependenciesLogger;
import ca.cutterslade.gradle.analyze.util.JavaUtil;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.maven.shared.dependency.analyzer.DependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.asm.ASMDependencyAnalyzer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;

class ProjectDependencyResolver {
  static final String CACHE_NAME =
      "ca.cutterslade.gradle.analyze.ProjectDependencyResolver.artifactClassCache";
  private static final JavaUtil.Function<ResolvedArtifact, ComponentIdentifier, RuntimeException>
      resolvedArtifactToComponentIdentifier = artifact -> artifact.getId().getComponentIdentifier();
  private final DependencyAnalyzer dependencyAnalyzer = new ASMDependencyAnalyzer();
  private final HashSetValuedHashMap<File, String> artifactClassCache;
  private final Logger logger;
  private final List<Provider<Configuration>> require;
  private final List<Provider<Configuration>> compileOnly;
  private final List<Provider<Configuration>> api;
  private final List<Provider<Configuration>> allowedToUse;
  private final List<Provider<Configuration>> allowedToDeclare;
  private final Collection<File> classesDirs;
  private final Map<ComponentIdentifier, Set<ComponentIdentifier>> aggregatorsWithDependencies;
  private final Map<ComponentIdentifier, Set<ComponentIdentifier>> pomsWithDependencies;
  private final Path logFilePath;
  private final boolean logDependencyInformationToFiles;

  ProjectDependencyResolver(
      final Logger logger,
      final HashSetValuedHashMap<File, String> artifactClassCache,
      final List<Provider<Configuration>> require,
      final List<Provider<Configuration>> compileOnly,
      final List<Provider<Configuration>> apiHelperConfiguration,
      final List<Provider<Configuration>> allowedToUse,
      final List<Provider<Configuration>> allowedToDeclare,
      final Collection<File> classesDirs,
      final List<Provider<Configuration>> allowedAggregatorsToUse,
      final Path logFilePath,
      final boolean logDependencyInformationToFiles) {
    this.logDependencyInformationToFiles = logDependencyInformationToFiles;
    this.logFilePath = logFilePath;
    this.logger = logger;
    this.require = require;
    this.compileOnly = compileOnly;
    this.api = apiHelperConfiguration;
    this.allowedToUse = allowedToUse;
    this.allowedToDeclare = allowedToDeclare;
    this.classesDirs = classesDirs;
    this.pomsWithDependencies = getPomsWithDependenciesMapping(require, logger);
    this.aggregatorsWithDependencies = getAggregatorsMapping(allowedAggregatorsToUse);
    this.artifactClassCache = artifactClassCache;
  }

  ProjectDependencyAnalysisResult analyzeDependencies() {
    return AnalyzeDependenciesLogger.create(
        logger,
        logDependencyInformationToFiles,
        logFilePath,
        logger -> {
          // Use utility methods that directly accept providers
          final List<ResolvedDependency> allowedToUseDeps = getFirstLevelDependencies(allowedToUse);
          final List<ResolvedDependency> allowedToDeclareDeps =
              getFirstLevelDependencies(allowedToDeclare);
          final List<ResolvedDependency> requiredDeps = getFirstLevelDependencies(require);
          requiredDeps.removeIf(
              req ->
                  allowedToUseDeps.stream()
                      .anyMatch(
                          allowed -> req.getModule().getId().equals(allowed.getModule().getId())));

          final MultiValuedMap<ComponentIdentifier, File> dependencyArtifacts =
              findModuleArtifactFiles(requiredDeps);
          logger.info("dependencyArtifacts", dependencyArtifacts);

          final MultiValuedMap<ComponentIdentifier, File> allDependencyArtifactFiles =
              findAllModuleArtifactFiles(requiredDeps);
          logger.info("allDependencyArtifactFiles", allDependencyArtifactFiles);

          final MultiValuedMap<ComponentIdentifier, String> fileClassMap =
              buildArtifactClassMap(this.logger, artifactClassCache, allDependencyArtifactFiles);
          logger.info("fileClassMap", fileClassMap);

          final Set<String> dependencyClasses = analyzeClassDependencies();
          logger.info("dependencyClasses", dependencyClasses);

          final MultiValuedMap<ComponentIdentifier, String> usedClassesInArtifacts =
              buildUsedArtifacts(fileClassMap, dependencyClasses);
          logger.info("usedClassesInArtifacts", usedClassesInArtifacts);

          final Set<ComponentIdentifier> usedArtifacts =
              new HashSet<>(usedClassesInArtifacts.keySet());
          logger.info("usedArtifacts", usedArtifacts);

          final Set<ComponentIdentifier> usedDeclaredArtifactFiles =
              new HashSet<>(dependencyArtifacts.keySet());
          usedDeclaredArtifactFiles.retainAll(usedArtifacts);
          logger.info("usedDeclaredArtifacts", usedDeclaredArtifactFiles);

          final Set<ComponentIdentifier> usedUndeclaredArtifactFiles = new HashSet<>(usedArtifacts);
          usedUndeclaredArtifactFiles.removeAll(dependencyArtifacts.keySet());
          logger.info("usedUndeclaredArtifacts", usedUndeclaredArtifactFiles);

          final Set<ComponentIdentifier> unusedDeclaredArtifactFiles =
              new HashSet<>(dependencyArtifacts.keySet());
          unusedDeclaredArtifactFiles.removeAll(usedArtifacts);
          logger.info("unusedDeclaredArtifacts", unusedDeclaredArtifactFiles);

          final Set<ResolvedArtifact> allowedToUseArtifacts =
              collectMany(allowedToUseDeps, ResolvedDependency::getModuleArtifacts);
          logger.info("allowedToUseArtifacts", allowedToUseArtifacts);

          final Set<ComponentIdentifier> allowedToUseComponentIdentifiers =
              collect(allowedToUseArtifacts, resolvedArtifactToComponentIdentifier);
          logger.info("allowedToUseComponentIdentifiers", allowedToUseComponentIdentifiers);

          final Set<ResolvedArtifact> allowedToDeclareArtifacts =
              collectMany(allowedToDeclareDeps, ResolvedDependency::getModuleArtifacts);
          logger.info("allowedToDeclareArtifacts", allowedToDeclareArtifacts);

          final Set<ComponentIdentifier> allArtifacts =
              collect(resolveArtifacts(require), resolvedArtifactToComponentIdentifier);
          logger.info("allArtifacts", allArtifacts);

          final Set<ComponentIdentifier> usedDeclared =
              findAll(allArtifacts, usedDeclaredArtifactFiles::contains);
          logger.info("usedDeclared", usedDeclared);

          final Set<ComponentIdentifier> usedUndeclared =
              findAll(allArtifacts, usedUndeclaredArtifactFiles::contains);
          logger.info("usedUndeclared", usedUndeclared);

          if (!allowedToUseComponentIdentifiers.isEmpty()) {
            usedUndeclared.removeIf(allowedToUseComponentIdentifiers::contains);
            logger.info("usedUndeclared without allowedToUseArtifacts", usedUndeclared);
          }

          final Set<ComponentIdentifier> unusedDeclared =
              findAll(allArtifacts, unusedDeclaredArtifactFiles::contains);
          logger.info("unusedDeclared", unusedDeclared);

          if (!allowedToDeclareArtifacts.isEmpty()) {
            final Set<ComponentIdentifier> allowedToDeclareComponentIdentifiers =
                collect(allowedToDeclareArtifacts, resolvedArtifactToComponentIdentifier);
            unusedDeclared.removeIf(allowedToDeclareComponentIdentifiers::contains);
            logger.info("unusedDeclared without allowedToDeclareArtifacts", unusedDeclared);
          }

          final Set<ComponentIdentifier> superfluous = new LinkedHashSet<>();

          final Map<ComponentIdentifier, Set<ComponentIdentifier>> dependencyMap = new HashMap<>();
          dependencyMap.putAll(aggregatorsWithDependencies);
          dependencyMap.putAll(pomsWithDependencies);

          if (!dependencyMap.isEmpty()) {
            final Set<ComponentIdentifier> usedIdentifiers =
                collect(
                    collectMany(requiredDeps, ResolvedDependency::getAllModuleArtifacts),
                    resolvedArtifactToComponentIdentifier);

            usedIdentifiers.addAll(pomsWithDependencies.keySet());
            usedDeclared.addAll(pomsWithDependencies.keySet());
            usedArtifacts.addAll(pomsWithDependencies.keySet());

            final Map<Boolean, Map<ComponentIdentifier, Collection<ComponentIdentifier>>>
                aggregatorUsage =
                    used(usedIdentifiers, usedArtifacts, dependencyMap, logger).entrySet().stream()
                        .collect(
                            Collectors.groupingBy(
                                o -> o.getValue().isEmpty(),
                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            if (aggregatorUsage.containsKey(true)) {
              final Set<ComponentIdentifier> aggregators = aggregatorUsage.get(true).keySet();
              superfluous.addAll(SetUtils.intersection(aggregators, usedIdentifiers));
              superfluous.removeIf(pomsWithDependencies.keySet()::contains);
            }
            if (aggregatorUsage.containsKey(false)) {
              final Set<ComponentIdentifier> aggregators = aggregatorUsage.get(false).keySet();
              usedDeclared.addAll(SetUtils.intersection(aggregators, unusedDeclared));

              final Set<ComponentIdentifier> aggregatorDependencies =
                  collectMany(aggregatorUsage.get(false).values(), c -> c);
              superfluous.addAll(SetUtils.intersection(usedDeclared, aggregatorDependencies));

              superfluous.removeIf(aggregators::contains);
              unusedDeclared.removeIf(aggregators::contains);
              collectMany(aggregators, dependencyMap::get).forEach(usedUndeclared::remove);

              final Set<ComponentIdentifier> apiDependencies =
                  collect(
                      collectMany(
                          getFirstLevelDependencies(api),
                          ResolvedDependency::getAllModuleArtifacts),
                      resolvedArtifactToComponentIdentifier);
              unusedDeclared.removeIf(apiDependencies::contains);
              superfluous.removeIf(apiDependencies::contains);

              final Set<ComponentIdentifier> undeclaredAggregators =
                  findAll(
                      aggregators,
                      componentIdentifier -> !usedDeclared.contains(componentIdentifier));
              usedUndeclared.addAll(undeclaredAggregators);
              usedUndeclared.removeIf(
                  id ->
                      allowedToUseComponentIdentifiers.contains(id)
                          && dependencyMap.containsKey(id));
            }
          }

          final Map<ComponentIdentifier, List<ResolvedArtifact>> compileOnlyDependencyArtifacts =
              resolveArtifacts(compileOnly).stream()
                  .collect(Collectors.groupingBy(resolvedArtifactToComponentIdentifier));
          logger.info("compileOnlyDependencyArtifacts", compileOnlyDependencyArtifacts);

          final Set<ComponentIdentifier> compileOnlyDependencyModuleIdentifiers =
              compileOnlyDependencyArtifacts.keySet();
          usedUndeclared.addAll(
              findAll(usedDeclared, compileOnlyDependencyModuleIdentifiers::contains));
          compileOnlyDependencyModuleIdentifiers.forEach(unusedDeclared::remove);

          return new ProjectDependencyAnalysisResult(
              usedDeclared,
              usedUndeclared,
              unusedDeclared,
              compileOnlyDependencyModuleIdentifiers,
              superfluous);
        });
  }

  /**
   * Find and analyze all class files to determine which external classes are used.
   *
   * @return a Set of class names
   */
  private Set<String> analyzeClassDependencies() {
    return collectMany(classesDirs, file -> dependencyAnalyzer.analyze(file.toURI().toURL()));
  }
}

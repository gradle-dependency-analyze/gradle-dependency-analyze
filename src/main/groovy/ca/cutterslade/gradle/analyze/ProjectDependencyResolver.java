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

class ProjectDependencyResolver {
  static final String CACHE_NAME =
      "ca.cutterslade.gradle.analyze.ProjectDependencyResolver.artifactClassCache";
  private static final JavaUtil.Function<ResolvedArtifact, ComponentIdentifier, RuntimeException>
      resolvedArtifactToComponentIdentifier = artifact -> artifact.getId().getComponentIdentifier();
  private final DependencyAnalyzer dependencyAnalyzer = new ASMDependencyAnalyzer();
  private final HashSetValuedHashMap<File, String> artifactClassCache;
  private final Logger logger;
  private final List<Configuration> require;
  private final List<Configuration> compileOnly;
  private final List<Configuration> api;
  private final List<Configuration> allowedToUse;
  private final List<Configuration> allowedToDeclare;
  private final Collection<File> classesDirs;
  private final Map<ComponentIdentifier, Set<ComponentIdentifier>> aggregatorsWithDependencies;
  private final Path logFilePath;
  private final boolean logDependencyInformationToFiles;

  ProjectDependencyResolver(
      final Logger logger,
      final HashSetValuedHashMap<File, String> artifactClassCache,
      final List<Configuration> require,
      final List<Configuration> compileOnly,
      final List<Configuration> apiHelperConfiguration,
      final List<Configuration> allowedToUse,
      final List<Configuration> allowedToDeclare,
      final Collection<File> classesDirs,
      final List<Configuration> allowedAggregatorsToUse,
      final Path logFilePath,
      final boolean logDependencyInformationToFiles) {
    this.logDependencyInformationToFiles = logDependencyInformationToFiles;
    this.logFilePath = logFilePath;
    this.logger = logger;
    this.require = removeNulls(require);
    this.compileOnly = compileOnly;
    this.api = apiHelperConfiguration;
    this.allowedToUse = removeNulls(allowedToUse);
    this.allowedToDeclare = removeNulls(allowedToDeclare);
    this.classesDirs = classesDirs;
    this.aggregatorsWithDependencies = getAggregatorsMapping(removeNulls(allowedAggregatorsToUse));
    this.artifactClassCache = artifactClassCache;
  }

  ProjectDependencyAnalysisResult analyzeDependencies() {
    return AnalyzeDependenciesLogger.create(
        logger,
        logDependencyInformationToFiles,
        logFilePath,
        logger -> {
          List<ResolvedDependency> allowedToUseDeps = getFirstLevelDependencies(allowedToUse);
          List<ResolvedDependency> allowedToDeclareDeps =
              getFirstLevelDependencies(allowedToDeclare);
          List<ResolvedDependency> requiredDeps = getFirstLevelDependencies(require);
          requiredDeps.removeIf(
              req ->
                  allowedToUseDeps.stream()
                      .anyMatch(
                          allowed -> req.getModule().getId().equals(allowed.getModule().getId())));

          MultiValuedMap<ComponentIdentifier, File> dependencyArtifacts =
              findModuleArtifactFiles(requiredDeps);
          logger.info("dependencyArtifacts", dependencyArtifacts);

          MultiValuedMap<ComponentIdentifier, File> allDependencyArtifactFiles =
              findAllModuleArtifactFiles(requiredDeps);
          logger.info("allDependencyArtifactFiles", allDependencyArtifactFiles);

          MultiValuedMap<ComponentIdentifier, String> fileClassMap =
              buildArtifactClassMap(this.logger, artifactClassCache, allDependencyArtifactFiles);
          logger.info("fileClassMap", fileClassMap);

          Set<String> dependencyClasses = analyzeClassDependencies();
          logger.info("dependencyClasses", dependencyClasses);

          MultiValuedMap<ComponentIdentifier, String> usedClassesInArtifacts =
              buildUsedArtifacts(fileClassMap, dependencyClasses);
          logger.info("usedClassesInArtifacts", usedClassesInArtifacts);

          Set<ComponentIdentifier> usedArtifacts = usedClassesInArtifacts.keySet();
          logger.info("usedArtifacts", usedArtifacts);

          HashSet<ComponentIdentifier> usedDeclaredArtifactFiles =
              new HashSet<>(dependencyArtifacts.keySet());
          usedDeclaredArtifactFiles.retainAll(usedArtifacts);
          logger.info("usedDeclaredArtifacts", usedDeclaredArtifactFiles);

          HashSet<ComponentIdentifier> usedUndeclaredArtifactFiles = new HashSet<>(usedArtifacts);
          usedUndeclaredArtifactFiles.removeAll(dependencyArtifacts.keySet());
          logger.info("usedUndeclaredArtifacts", usedUndeclaredArtifactFiles);

          HashSet<ComponentIdentifier> unusedDeclaredArtifactFiles =
              new HashSet<>(dependencyArtifacts.keySet());
          unusedDeclaredArtifactFiles.removeAll(usedArtifacts);
          logger.info("unusedDeclaredArtifacts", unusedDeclaredArtifactFiles);

          Set<ResolvedArtifact> allowedToUseArtifacts =
              collectMany(allowedToUseDeps, ResolvedDependency::getModuleArtifacts);
          logger.info("allowedToUseArtifacts", allowedToUseArtifacts);

          Set<ComponentIdentifier> allowedToUseComponentIdentifiers =
              collect(allowedToUseArtifacts, resolvedArtifactToComponentIdentifier);
          logger.info("allowedToUseComponentIdentifiers", allowedToUseComponentIdentifiers);

          Set<ResolvedArtifact> allowedToDeclareArtifacts =
              collectMany(allowedToDeclareDeps, ResolvedDependency::getModuleArtifacts);
          logger.info("allowedToDeclareArtifacts", allowedToDeclareArtifacts);

          Set<ComponentIdentifier> allArtifacts =
              collect(resolveArtifacts(require), resolvedArtifactToComponentIdentifier);
          logger.info("allArtifacts", allArtifacts);

          Set<ComponentIdentifier> usedDeclared =
              findAll(allArtifacts, usedDeclaredArtifactFiles::contains);
          logger.info("usedDeclared", usedDeclared);

          Set<ComponentIdentifier> usedUndeclared =
              findAll(allArtifacts, usedUndeclaredArtifactFiles::contains);
          logger.info("usedUndeclared", usedUndeclared);

          if (!allowedToUseComponentIdentifiers.isEmpty()) {
            usedUndeclared.removeIf(allowedToUseComponentIdentifiers::contains);
            logger.info("usedUndeclared without allowedToUseArtifacts", usedUndeclared);
          }

          Set<ComponentIdentifier> unusedDeclared =
              findAll(allArtifacts, unusedDeclaredArtifactFiles::contains);
          logger.info("unusedDeclared", unusedDeclared);

          if (!allowedToDeclareArtifacts.isEmpty()) {
            Set<ComponentIdentifier> allowedToDeclareComponentIdentifiers =
                collect(allowedToDeclareArtifacts, resolvedArtifactToComponentIdentifier);
            unusedDeclared.removeIf(allowedToDeclareComponentIdentifiers::contains);
            logger.info("unusedDeclared without allowedToDeclareArtifacts", unusedDeclared);
          }

          Set<ComponentIdentifier> superfluous = new LinkedHashSet<>();
          if (!aggregatorsWithDependencies.isEmpty()) {
            Set<ComponentIdentifier> usedIdentifiers =
                collect(
                    collectMany(requiredDeps, ResolvedDependency::getAllModuleArtifacts),
                    resolvedArtifactToComponentIdentifier);

            Map<Boolean, Map<ComponentIdentifier, Collection<ComponentIdentifier>>>
                aggregatorUsage =
                    used(usedIdentifiers, usedArtifacts, aggregatorsWithDependencies, logger)
                        .entrySet()
                        .stream()
                        .collect(
                            Collectors.groupingBy(
                                o -> o.getValue().isEmpty(),
                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            if (aggregatorUsage.containsKey(true)) {
              Set<ComponentIdentifier> aggregators = aggregatorUsage.get(true).keySet();
              superfluous.addAll(SetUtils.intersection(aggregators, usedIdentifiers));
            }
            if (aggregatorUsage.containsKey(false)) {
              Set<ComponentIdentifier> aggregators = aggregatorUsage.get(false).keySet();
              usedDeclared.addAll(SetUtils.intersection(aggregators, unusedDeclared));

              Set<ComponentIdentifier> aggregatorDependencies =
                  collectMany(aggregatorUsage.get(false).values(), c -> c);
              superfluous.addAll(SetUtils.intersection(usedDeclared, aggregatorDependencies));

              superfluous.removeIf(aggregators::contains);
              unusedDeclared.removeIf(aggregators::contains);
              collectMany(aggregators, aggregatorsWithDependencies::get)
                  .forEach(usedUndeclared::remove);

              Set<ComponentIdentifier> apiDependencies =
                  collect(
                      collectMany(
                          getFirstLevelDependencies(api),
                          ResolvedDependency::getAllModuleArtifacts),
                      resolvedArtifactToComponentIdentifier);
              unusedDeclared.removeIf(apiDependencies::contains);
              superfluous.removeIf(apiDependencies::contains);

              Set<ComponentIdentifier> undeclaredAggregators =
                  findAll(
                      aggregators,
                      componentIdentifier -> !usedDeclared.contains(componentIdentifier));
              usedUndeclared.addAll(undeclaredAggregators);
              usedUndeclared.removeIf(
                  id ->
                      allowedToUseComponentIdentifiers.contains(id)
                          && aggregatorsWithDependencies.containsKey(id));
            }
          }

          Map<ComponentIdentifier, List<ResolvedArtifact>> compileOnlyDependencyArtifacts =
              resolveArtifacts(compileOnly).stream()
                  .collect(Collectors.groupingBy(resolvedArtifactToComponentIdentifier));
          logger.info("compileOnlyDependencyArtifacts", compileOnlyDependencyArtifacts);

          Set<ComponentIdentifier> compileOnlyDependencyModuleIdentifiers =
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

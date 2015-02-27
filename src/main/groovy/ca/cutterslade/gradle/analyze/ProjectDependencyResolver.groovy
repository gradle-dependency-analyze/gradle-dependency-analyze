package ca.cutterslade.gradle.analyze

import org.apache.maven.shared.dependency.analyzer.ClassAnalyzer
import org.apache.maven.shared.dependency.analyzer.DefaultClassAnalyzer
import org.apache.maven.shared.dependency.analyzer.DependencyAnalyzer
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis
import org.apache.maven.shared.dependency.analyzer.asm.ASMDependencyAnalyzer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger

import java.util.concurrent.ConcurrentHashMap

class ProjectDependencyResolver {
  private static final ConcurrentHashMap<File, Set<String>> ARTIFACT_CLASS_CACHE = new ConcurrentHashMap<>();

  private final ClassAnalyzer classAnalyzer = new DefaultClassAnalyzer();
  private final DependencyAnalyzer dependencyAnalyzer = new ASMDependencyAnalyzer();

  private final Logger logger;
  private final List<Configuration> require;
  private final List<Configuration> allowedToUse;
  private final List<Configuration> allowedToDeclare;
  private final File classesDir;

  ProjectDependencyResolver(final Logger logger, final List<Configuration> require,
      final List<Configuration> allowedToUse, final List<Configuration> allowedToDeclare,
      final File classesDir) {
    this.logger = logger
    this.require = require
    this.allowedToUse = allowedToUse
    this.allowedToDeclare = allowedToDeclare
    this.classesDir = classesDir
  }
/**
 *
 * @param project
 * @return
 */
  ProjectDependencyAnalysis analyzeDependencies() {
    Set<ResolvedDependency> allowedToUseDeps = allowedToUseDependencies
    Set<ResolvedDependency> allowedToDeclareDeps = allowedToDeclareDependencies
    Set<ResolvedDependency> requiredDeps = requiredDependencies - allowedToUseDeps
    Set<File> dependencyArtifacts = findModuleArtifactFiles(requiredDeps)
    logger.info "dependencyArtifacts = $dependencyArtifacts"

    Set<File> allDependencyArtifacts = findAllModuleArtifactFiles(requiredDeps)
    logger.info "allDependencyArtifacts = $allDependencyArtifacts"

    Map<File, Set<String>> fileClassMap = buildArtifactClassMap(allDependencyArtifacts)
    logger.info "fileClassMap = $fileClassMap"

    Set<String> dependencyClasses = analyzeClassDependencies()
    logger.info "dependencyClasses = $dependencyClasses"

    Set<File> usedArtifacts = buildUsedArtifacts(fileClassMap, dependencyClasses)
    logger.info "usedArtifacts = $usedArtifacts"

    Set<File> usedDeclaredArtifacts = new LinkedHashSet<File>(dependencyArtifacts)
    usedDeclaredArtifacts.retainAll(usedArtifacts)
    logger.info "usedDeclaredArtifacts = $usedDeclaredArtifacts"

    Set<File> usedUndeclaredArtifacts = new LinkedHashSet<File>(usedArtifacts)
    usedUndeclaredArtifacts.removeAll(dependencyArtifacts)
    logger.info "usedUndeclaredArtifacts = $usedUndeclaredArtifacts"

    Set<File> unusedDeclaredArtifacts = new LinkedHashSet<File>(dependencyArtifacts)
    unusedDeclaredArtifacts.removeAll(usedArtifacts)
    logger.info "unusedDeclaredArtifacts = $unusedDeclaredArtifacts"

    Set<ResolvedArtifact> allowedToUseArtifacts = allowedToUseDeps*.moduleArtifacts?.flatten()
    logger.info "allowedToUseArtifacts = $allowedToUseArtifacts"
    Set<ResolvedArtifact> allowedToDeclareArtifacts = allowedToDeclareDeps*.moduleArtifacts?.flatten()
    logger.info "allowedToDeclareArtifacts = $allowedToDeclareArtifacts"

    Set<ResolvedArtifact> allArtifacts = require*.resolvedConfiguration*.firstLevelModuleDependencies*.allModuleArtifacts.flatten()
    logger.info "allArtifacts = $allArtifacts"

    def usedDeclared = allArtifacts.findAll {ResolvedArtifact artifact -> artifact.file in usedDeclaredArtifacts}
    def usedUndeclared = allArtifacts.findAll {ResolvedArtifact artifact -> artifact.file in usedUndeclaredArtifacts}
    if (allowedToUseArtifacts) usedUndeclared-=allowedToUseArtifacts
    def unusedDeclared = allArtifacts.findAll {ResolvedArtifact artifact -> artifact.file in unusedDeclaredArtifacts}
    if (allowedToDeclareArtifacts) unusedDeclared-=allowedToDeclareArtifacts

    return new ProjectDependencyAnalysis(
        usedDeclared.unique {it.file} as Set,
        usedUndeclared.unique {it.file} as Set,
        unusedDeclared.unique {it.file} as Set)
  }

  private Set<ResolvedDependency> getRequiredDependencies() {
    getFirstLevelDependencies(require)
  }

  private Set<ResolvedDependency> getAllowedToUseDependencies() {
    getFirstLevelDependencies(allowedToUse)
  }

  private Set<ResolvedDependency> getAllowedToDeclareDependencies() {
    getFirstLevelDependencies(allowedToDeclare)
  }

  private Set<ResolvedDependency> getFirstLevelDependencies(final List<Configuration> configurations) {
    configurations.collect {it.resolvedConfiguration.firstLevelModuleDependencies}.flatten()
  }


  /**
   * Map each of the files declared on all configurations of the project to a collection of the class names they
   * contain.
   * @param project the project we're working on
   * @return a Map of files to their classes
   * @throws IOException
   */
  private Map<File, Set<String>> buildArtifactClassMap(Set<File> dependencyArtifacts) throws IOException {
    final Map<File, Set<String>> artifactClassMap = [:]

    dependencyArtifacts.each {File file ->
      if (file.name.endsWith('jar')) {
        artifactClassMap.put(file, ARTIFACT_CLASS_CACHE.computeIfAbsent(file, { it ->
          classAnalyzer.analyze(it.toURI().toURL()).asImmutable();
        }))
      }
      else {
        logger.info "Skipping analysis of file for classes: $file"
      }
    }
    return artifactClassMap
  }

  private Set<File> findModuleArtifactFiles(Set<ResolvedDependency> dependencies) {
    dependencies*.moduleArtifacts*.collect {it.file}.unique().flatten()
  }

  private Set<File> findAllModuleArtifactFiles(Set<ResolvedDependency> dependencies) {
    dependencies*.allModuleArtifacts*.collect {it.file}.unique().flatten()
  }

  /**
   * Find and analyze all class files to determine which external classes are used.
   * @param project
   * @return a Set of class names
   */
  private Set<String> analyzeClassDependencies() {
    dependencyAnalyzer.analyze(classesDir.toURI().toURL())
  }

  /**
   * Determine which of the project dependencies are used.
   *
   * @param artifactClassMap a map of Files to the classes they contain
   * @param dependencyClasses all classes used directly by the project
   * @return a set of project dependencies confirmed to be used by the project
   */
  private Set<File> buildUsedArtifacts(Map<File, Set<String>> artifactClassMap, Set<String> dependencyClasses) {
    Set<File> usedArtifacts = new HashSet()

    dependencyClasses.each {String className ->
      File artifact = artifactClassMap.find {it.value.contains(className)}?.key
      if (artifact) {
        usedArtifacts << artifact
      }
    }
    return usedArtifacts
  }
}
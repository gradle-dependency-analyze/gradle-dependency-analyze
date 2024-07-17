package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.logging.AnalyzeDependenciesLogger
import groovy.transform.CompileStatic
import org.apache.commons.collections4.multimap.HashSetValuedHashMap
import org.apache.maven.shared.dependency.analyzer.DependencyAnalyzer
import org.apache.maven.shared.dependency.analyzer.asm.ASMDependencyAnalyzer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.logging.Logger

import java.nio.file.Path

import static ca.cutterslade.gradle.analyze.util.ClassFileCollectorUtil.buildArtifactClassMap
import static ca.cutterslade.gradle.analyze.util.ProjectDependencyResolverUtils.*

@CompileStatic
class ProjectDependencyResolver {
    static final String CACHE_NAME = 'ca.cutterslade.gradle.analyze.ProjectDependencyResolver.artifactClassCache'

    private final DependencyAnalyzer dependencyAnalyzer = new ASMDependencyAnalyzer()
    private final HashSetValuedHashMap<File, String> artifactClassCache
    private final Logger logger
    private final List<Configuration> require
    private final List<Configuration> compileOnly
    private final List<Configuration> api
    private final List<Configuration> allowedToUse
    private final List<Configuration> allowedToDeclare
    private final Iterable<File> classesDirs
    private final Map<ComponentIdentifier, Set<ComponentIdentifier>> aggregatorsWithDependencies
    private final List<Configuration> allowedAggregatorsToUse
    private final Path logFilePath
    private final boolean logDependencyInformationToFiles

    ProjectDependencyResolver(final Project project,
                              final List<Configuration> require,
                              final List<Configuration> compileOnly,
                              final List<Configuration> apiHelperConfiguration,
                              final List<Configuration> allowedToUse,
                              final List<Configuration> allowedToDeclare,
                              final Iterable<File> classesDirs,
                              final List<Configuration> allowedAggregatorsToUse,
                              final Path logFilePath,
                              final boolean logDependencyInformationToFiles) {
        this.logDependencyInformationToFiles = logDependencyInformationToFiles
        this.logFilePath = logFilePath
        this.logger = project.logger
        this.require = removeNulls(require) as List
        this.compileOnly = compileOnly
        this.api = apiHelperConfiguration
        this.allowedAggregatorsToUse = removeNulls(allowedAggregatorsToUse) as List
        this.allowedToUse = removeNulls(allowedToUse) as List
        this.allowedToDeclare = removeNulls(allowedToDeclare) as List
        this.classesDirs = classesDirs
        this.aggregatorsWithDependencies = getAggregatorsMapping(this.allowedAggregatorsToUse)
        this.artifactClassCache =
                project.rootProject.extensions.getByName(CACHE_NAME) as HashSetValuedHashMap<File, String>
    }

    ProjectDependencyAnalysisResult analyzeDependencies() {
        AnalyzeDependenciesLogger.create(logger, logDependencyInformationToFiles, logFilePath) { logger ->
            def allowedToUseDeps = allowedToUseDependencies
            def allowedToDeclareDeps = allowedToDeclareDependencies
            def requiredDeps = requiredDependencies
            requiredDeps.removeAll { req ->
                allowedToUseDeps.any { allowed ->
                    req.module.id == allowed.module.id
                }
            }

            def dependencyArtifacts = findModuleArtifactFiles(requiredDeps)
            logger.info 'dependencyArtifacts', dependencyArtifacts

            def allDependencyArtifactFiles = findAllModuleArtifactFiles(requiredDeps)
            logger.info 'allDependencyArtifactFiles', allDependencyArtifactFiles

            def fileClassMap = buildArtifactClassMap(this.logger, artifactClassCache, allDependencyArtifactFiles)
            logger.info 'fileClassMap', fileClassMap

            def dependencyClasses = analyzeClassDependencies()
            logger.info 'dependencyClasses', dependencyClasses

            def usedClassesInArtifacts = buildUsedArtifacts(fileClassMap, dependencyClasses)
            logger.info 'usedClassesInArtifacts', usedClassesInArtifacts

            def usedArtifacts = usedClassesInArtifacts.keySet()
            logger.info 'usedArtifacts', usedArtifacts

            def usedDeclaredArtifactFiles = new HashSet<>(dependencyArtifacts.keySet())
            usedDeclaredArtifactFiles.retainAll(usedArtifacts)
            logger.info 'usedDeclaredArtifacts', usedDeclaredArtifactFiles

            def usedUndeclaredArtifactFiles = new HashSet<>(usedArtifacts)
            usedUndeclaredArtifactFiles.removeAll(dependencyArtifacts.keySet())
            logger.info 'usedUndeclaredArtifacts', usedUndeclaredArtifactFiles

            def unusedDeclaredArtifactFiles = new HashSet<>(dependencyArtifacts.keySet())
            unusedDeclaredArtifactFiles.removeAll(usedArtifacts)
            logger.info 'unusedDeclaredArtifacts', unusedDeclaredArtifactFiles

            def allowedToUseArtifacts = allowedToUseDeps.collectMany { it.moduleArtifacts }.toSet()
            logger.info 'allowedToUseArtifacts', allowedToUseArtifacts

            def allowedToUseComponentIdentifiers = allowedToUseArtifacts.collect { it.id.componentIdentifier }.toSet()
            logger.info 'allowedToUseComponentIdentifiers', allowedToUseComponentIdentifiers

            def allowedToDeclareArtifacts = allowedToDeclareDeps.collectMany { it.moduleArtifacts }.toSet()
            logger.info 'allowedToDeclareArtifacts', allowedToDeclareArtifacts

            def allArtifacts = resolveArtifacts(require).collect { e -> e.id.componentIdentifier }.toSet()
            logger.info 'allArtifacts', allArtifacts

            def usedDeclared = allArtifacts.findAll { a -> a in usedDeclaredArtifactFiles }
            logger.info 'usedDeclared', usedDeclared

            def usedUndeclared = allArtifacts.findAll { a -> a in usedUndeclaredArtifactFiles }
            logger.info 'usedUndeclared', usedUndeclared

            if (allowedToUseComponentIdentifiers) {
                usedUndeclared.removeAll { allowedToUseComponentIdentifiers.contains(it) }
                logger.info 'usedUndeclared without allowedToUseArtifacts', usedUndeclared
            }

            def unusedDeclared = allArtifacts.findAll { a -> a in unusedDeclaredArtifactFiles }
            logger.info 'unusedDeclared', unusedDeclared
            if (allowedToDeclareArtifacts) {
                def allowedToDeclareComponentIdentifiers = allowedToDeclareArtifacts.collect { it.id.componentIdentifier }
                unusedDeclared.removeAll { allowedToDeclareComponentIdentifiers.contains(it) }
                logger.info 'unusedDeclared without allowedToDeclareArtifacts', unusedDeclared
            }


            if (!aggregatorsWithDependencies.isEmpty()) {
                def usedIdentifiers = requiredDeps.collectMany { it.allModuleArtifacts }.collect { it.id.componentIdentifier }.toSet()
                def aggregatorUsage = used(usedIdentifiers, usedArtifacts, aggregatorsWithDependencies, logger).groupBy { it.value.isEmpty() }
                if (aggregatorUsage.containsKey(true)) {
                    def aggregators = aggregatorUsage.get(true).keySet()
                    def unusedAggregators = aggregators.intersect(usedIdentifiers)
                    unusedAggregators.each { a -> unusedDeclared.add(a) }
                }
                if (aggregatorUsage.containsKey(false)) {
                    def aggregators = aggregatorUsage.get(false).keySet()
                    def usedAggregators = aggregators.intersect(unusedDeclared)
                    usedAggregators.each { a -> usedDeclared.add(a) }

                    def aggregatorDependencies = aggregatorUsage.get(false).values().collectMany { it }.toSet()
                    def usedAggregatorDependencies = usedDeclared.intersect(aggregatorDependencies)
                    usedAggregatorDependencies.each { a -> unusedDeclared.add(a) }

                    unusedDeclared.removeAll { aggregators.contains(it) }
                    aggregators.collectMany { aggregatorsWithDependencies.get(it) }.toSet()
                            .forEach { usedUndeclared.remove(it) }

                    def apiDependencies = getFirstLevelDependencies(api).collectMany { it.allModuleArtifacts }
                            .collect { it.id.componentIdentifier }.toSet()
                    unusedDeclared.removeAll { apiDependencies.contains(it) }


                    def undeclaredAggregators = aggregators.findAll { !usedDeclared.contains(it) }
                    undeclaredAggregators.each { a -> usedUndeclared.add(a) }
                    usedUndeclared.removeAll { allowedToUseComponentIdentifiers.contains(it) && aggregatorsWithDependencies.keySet().contains(it) }
                }
            }

            def compileOnlyDependencyArtifacts = resolveArtifacts(compileOnly).groupBy { a -> a.id.componentIdentifier }
            logger.info 'compileOnlyDependencyArtifacts', compileOnlyDependencyArtifacts

            def compileOnlyDependencyModuleIdentifiers = compileOnlyDependencyArtifacts.keySet()
            usedDeclared.findAll {
                compileOnlyDependencyModuleIdentifiers.contains(it)
            }.each { a -> usedUndeclared.add(a) }
            compileOnlyDependencyModuleIdentifiers.each { a -> unusedDeclared.remove(a) }

            return new ProjectDependencyAnalysisResult(
                    usedDeclared,
                    usedUndeclared,
                    unusedDeclared,
                    compileOnlyDependencyModuleIdentifiers)
        }
    }

    private List<ResolvedDependency> getRequiredDependencies() {
        getFirstLevelDependencies(require)
    }

    private List<ResolvedDependency> getAllowedToUseDependencies() {
        getFirstLevelDependencies(allowedToUse)
    }

    private List<ResolvedDependency> getAllowedToDeclareDependencies() {
        getFirstLevelDependencies(allowedToDeclare)
    }

    /**
     * Find and analyze all class files to determine which external classes are used.
     * @param project
     * @return a Set of class names
     */
    private Set<String> analyzeClassDependencies() {
        classesDirs.collectMany { dependencyAnalyzer.analyze(it.toURI().toURL()) }.toSet()
    }
}

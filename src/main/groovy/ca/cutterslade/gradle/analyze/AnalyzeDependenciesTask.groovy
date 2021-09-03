package ca.cutterslade.gradle.analyze

import ca.cutterslade.gradle.analyze.util.ProjectDependencyResolverUtils
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

import java.lang.reflect.Method
import java.nio.file.Files

class AnalyzeDependenciesTask extends DefaultTask {
    public static final String DEPENDENCY_ANALYZE_DEPENDENCY_DIRECTORY_NAME = "reports/dependency-analyze"
    @Deprecated
    @Input
    boolean justWarn = false
    @Input
    boolean warnUsedUndeclared = false
    @Input
    boolean warnUnusedDeclared = false
    @Input
    boolean logDependencyInformationToFiles = false
    @InputFiles
    List<Configuration> require = []
    @Internal
    List<Configuration> apiHelperConfiguration = []
    @InputFiles
    List<Configuration> allowedToUse = []
    @InputFiles
    List<Configuration> allowedToDeclare = []
    @InputFiles
    List<Configuration> allowedAggregatorsToUse = []
    @InputFiles
    FileCollection classesDirs = project.files()
    @OutputDirectory
    File outputDirectory = project.file("$project.buildDir/$DEPENDENCY_ANALYZE_DEPENDENCY_DIRECTORY_NAME/")

    AnalyzeDependenciesTask() {
        def methods = outputs.class.getMethods().grep { Method m -> m.name == 'cacheIf' }
        if (methods) {
            outputs.cacheIf({ true })
        }
    }

    void setClassesDir(File classesDir) {
        this.classesDirs = project.files(classesDir)
    }

    private static String getArtifactSummary(String sectionName, Set<ResolvedArtifact> resolvedArtifacts) {
        StringBuffer buffer = new StringBuffer()
        if (resolvedArtifacts) {
            buffer.append("$sectionName: \n")
            resolvedArtifacts.sort(false) { it.moduleVersion.id.toString() }.each { ResolvedArtifact it ->
                def classifier = it.classifier ? ":$it.classifier" : ""
                buffer.append(" - $it.moduleVersion.id$classifier@$it.extension\n")
            }
        }
        return buffer.toString()
    }

    private static GString foundIssues(String issues) {
        return "Dependency analysis found issues.\n$issues"
    }

    @TaskAction
    def action() {
        if (justWarn) {
            logger.warn("justWarn is deprecated in favor of warnUsedUndeclared and warnUnusedDeclared. Forcefully setting " +
                    "warnUsedUndeclared=true and warnUnusedDeclared=true options")
            warnUnusedDeclared = true
            warnUsedUndeclared = true
        }

        logger.info "Analyzing dependencies of $classesDirs for [require: $require, allowedToUse: $allowedToUse, " +
                "allowedToDeclare: $allowedToDeclare]"
        ProjectDependencyAnalysisResult analysis =
                new ProjectDependencyResolver(project, require, apiHelperConfiguration, allowedToUse,
                        allowedToDeclare, classesDirs, allowedAggregatorsToUse, logDependencyInformationToFiles).analyzeDependencies()
        String usedUndeclaredViolations = getArtifactSummary('usedUndeclaredArtifacts', analysis.getUsedUndeclaredArtifacts())
        String unusedDeclaredViolations = getArtifactSummary('unusedDeclaredArtifacts', analysis.getUnusedDeclaredArtifacts())
        String combinedViolations = usedUndeclaredViolations.concat(unusedDeclaredViolations)

        if (!combinedViolations.isEmpty()) {
            if (logDependencyInformationToFiles) {
                final def outputFile = new File(outputDirectory, "${name}.log")
                outputFile.parentFile.mkdirs()
                outputFile.text = combinedViolations
            }

            if (!warnUsedUndeclared && !warnUnusedDeclared) {
                throw new DependencyAnalysisException(foundIssues(combinedViolations))
            }

            boolean processedUsedUndeclared = false
            boolean processedUnusedDeclared = false

            if (warnUsedUndeclared && !usedUndeclaredViolations.isEmpty()) {
                logger.warn foundIssues(usedUndeclaredViolations)
                processedUsedUndeclared = true
            }

            if (warnUnusedDeclared && !unusedDeclaredViolations.isEmpty()) {
                logger.warn foundIssues(unusedDeclaredViolations)
                processedUnusedDeclared = true
            }

            if (!processedUsedUndeclared && !usedUndeclaredViolations.isEmpty()) {
                throw new DependencyAnalysisException(foundIssues(usedUndeclaredViolations))
            }

            if (!processedUnusedDeclared && !unusedDeclaredViolations.isEmpty()) {
                throw new DependencyAnalysisException(foundIssues(unusedDeclaredViolations))
            }
        }
    }

    @InputFiles
    FileCollection getAllArtifacts() {
        project.files({
            def files = ProjectDependencyResolverUtils.removeNulls(
                    ProjectDependencyResolverUtils.removeNulls(require)
                            *.resolvedConfiguration
                            *.firstLevelModuleDependencies
                            *.allModuleArtifacts
                            *.file.flatten() as Set<File>
            )
            if (logDependencyInformationToFiles) {
                Files.createDirectories(outputDirectory.toPath())
                new PrintWriter(Files.newOutputStream(outputDirectory.toPath().resolve("allArtifactFiles.log"))).withCloseable { final printWriter ->
                    printWriter.println("All artifact files:")
                    files.forEach { final file -> printWriter.println(file) }
                }
            } else {
                logger.info "All Artifact Files: $files"
            }
            files
        })
    }

    @InputFiles
    FileCollection getRequiredFiles() {
        project.files({
            def files = getFirstLevelFiles(require, 'required') -
                    getFirstLevelFiles(allowedToUse, 'allowed to use')
            if (logDependencyInformationToFiles) {
                Files.createDirectories(outputDirectory.toPath())
                new PrintWriter(Files.newOutputStream(outputDirectory.toPath().resolve("allRequiredFiles.log"))).withCloseable { final printWriter ->
                    printWriter.println("Actual required files:")
                    files.forEach { final file -> printWriter.println(file) }
                }
            } else {
                logger.info "Actual required files: $files"
            }
            files
        })
    }

    @InputFiles
    FileCollection getAllowedToUseFiles() {
        getFirstLevelFileCollection(allowedToUse, 'allowed to use')
    }

    @InputFiles
    FileCollection getAllowedToDeclareFiles() {
        getFirstLevelFileCollection(allowedToDeclare, 'allowed to declare')
    }

    private FileCollection getFirstLevelFileCollection(List<Configuration> configurations, String name) {
        project.files { getFirstLevelFiles(configurations, name) }
    }

    Set<File> getFirstLevelFiles(List<Configuration> configurations, String name) {
        Set<File> files = ProjectDependencyResolverUtils.removeNulls(
                ProjectDependencyResolverUtils.getFirstLevelDependencies(
                        ProjectDependencyResolverUtils.removeNulls(configurations)
                )*.moduleArtifacts*.file.flatten() as Set<File>
        )
        if (logDependencyInformationToFiles) {
            Files.createDirectories(outputDirectory.toPath())
            new PrintWriter(Files.newOutputStream(outputDirectory.toPath().resolve("first level ${name}.log"))).withCloseable { final printWriter ->
                printWriter.println("First level $name files:")
                files.forEach { final file -> printWriter.println(file) }
            }
        } else {
            logger.info "First level $name files: $files"
        }
        files
    }
}

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
    @Input
    boolean justWarn = false
    @Input
    boolean logDependencyInformationToFiles = false
    @InputFiles
    List<Configuration> require = []
    @Internal
    Configuration apiHelperConfiguration
    @Internal
    String apiConfigurationName = ''
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

    @TaskAction
    def action() {
        logger.info "Analyzing dependencies of $classesDirs for [require: $require, allowedToUse: $allowedToUse, " +
                "allowedToDeclare: $allowedToDeclare]"
        ProjectDependencyAnalysisResult analysis =
                new ProjectDependencyResolver(project, require, apiHelperConfiguration, apiConfigurationName, allowedToUse,
                        allowedToDeclare, classesDirs, allowedAggregatorsToUse, logDependencyInformationToFiles).analyzeDependencies()
        StringBuffer buffer = new StringBuffer()
        [new Tuple2<>('usedUndeclaredArtifacts', analysis.getUsedUndeclaredArtifacts()),
         new Tuple2<>('unusedDeclaredArtifacts', analysis.getUnusedDeclaredArtifacts())].each { violations ->
            if (violations.second) {
                buffer.append("$violations.first: \n")
                violations.second.sort(false) { it.moduleVersion.id.toString() }.each { ResolvedArtifact it ->
                    def classifier = it.classifier ? ":$it.classifier" : ""
                    buffer.append(" - $it.moduleVersion.id$classifier@$it.extension\n")
                }
            }
        }

        if (buffer) {
            if (logDependencyInformationToFiles) {
                final def outputFile = new File(outputDirectory, name)
                outputFile.parentFile.mkdirs()
                outputFile.text = buffer.toString()
            }
            def message = "Dependency analysis found issues.\n$buffer"
            if (justWarn) {
                logger.warn message
            } else {
                throw new DependencyAnalysisException(message)
            }
        }
    }

    @InputFiles
    FileCollection getAllArtifacts() {
        project.files({
            def files = ProjectDependencyResolver.removeNulls(
                    ProjectDependencyResolver.removeNulls(require)
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
        Set<File> files = ProjectDependencyResolver.removeNulls(
                ProjectDependencyResolverUtils.getFirstLevelDependencies(
                        ProjectDependencyResolver.removeNulls(configurations)
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

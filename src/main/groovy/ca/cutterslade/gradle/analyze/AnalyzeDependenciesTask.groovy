package ca.cutterslade.gradle.analyze

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

import java.lang.reflect.Method
import java.nio.file.Path

import static ca.cutterslade.gradle.analyze.util.ProjectDependencyAnalysisResultHandler.warnAndLogOrFail

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

    AnalyzeDependenciesTask() {
        def methods = outputs.class.getMethods().grep { Method m -> m.name == 'cacheIf' }
        if (methods) {
            outputs.cacheIf({ true })
        }
    }

    void setClassesDir(File classesDir) {
        this.classesDirs = project.files(classesDir)
    }

    @Optional
    @OutputFile
    Path getLogFilePath() {
        logDependencyInformationToFiles
                ? project.buildDir.toPath().resolve("$DEPENDENCY_ANALYZE_DEPENDENCY_DIRECTORY_NAME").resolve("${name}.log")
                : null
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
        final def analysis = new ProjectDependencyResolver(
                project,
                require,
                apiHelperConfiguration,
                allowedToUse,
                allowedToDeclare,
                classesDirs,
                allowedAggregatorsToUse,
                logFilePath
        ).analyzeDependencies()

        warnAndLogOrFail(
                analysis,
                warnUsedUndeclared,
                warnUnusedDeclared,
                logFilePath,
                logger
        )
    }
}

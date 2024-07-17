package ca.cutterslade.gradle.analyze;

import org.gradle.api.DefaultTask;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ca.cutterslade.gradle.analyze.util.ProjectDependencyAnalysisResultHandler.warnAndLogOrFail;

@CacheableTask
public class AnalyzeDependenciesTask extends DefaultTask {
    private final Path logFilePath = getProject().getLayout().getBuildDirectory()
            .dir("reports").get().getAsFile().toPath()
            .resolve("dependency-analyze")
            .resolve(getName() + ".log");
    @Deprecated
    private Boolean justWarn = false;
    private Boolean warnUsedUndeclared = false;
    private Boolean warnUnusedDeclared = false;
    private Boolean warnCompileOnly = false;
    private Boolean logDependencyInformationToFiles = false;
    private List<Configuration> require = new ArrayList<>();
    private List<Configuration> compileOnly = new ArrayList<>();
    private List<Configuration> apiHelperConfiguration = new ArrayList<>();
    private List<Configuration> allowedToUse = new ArrayList<>();
    private List<Configuration> allowedToDeclare = new ArrayList<>();
    private List<Configuration> allowedAggregatorsToUse = new ArrayList<>();
    private FileCollection classesDirs = getProject().files();

    @TaskAction
    public void action() throws IOException {
        if (justWarn) {
            getLogger().warn("justWarn is deprecated in favor of warnUsedUndeclared and warnUnusedDeclared. Forcefully setting " + "warnUsedUndeclared=true and warnUnusedDeclared=true options");
            warnUnusedDeclared = true;
            warnUsedUndeclared = true;
        }

        if (logDependencyInformationToFiles) {
            getLogger().info("Writing dependency information to {}", logFilePath);
        }

        getLogger().info("Analyzing dependencies of {} for [require: {}, allowedToUse: {}, allowedToDeclare: {}]",
                getClassesDirs(), getRequire(), getAllowedToUse(), getAllowedToDeclare());
        ProjectDependencyAnalysisResult analysis = getAnalysisResult();

        warnAndLogOrFail(analysis, warnUsedUndeclared, warnUnusedDeclared, warnCompileOnly, logFilePath, getLogger());
    }

    private ProjectDependencyAnalysisResult getAnalysisResult() {
        final ProjectDependencyResolver resolver;
        try {
            resolver = new ProjectDependencyResolver(
                    getProject(), require, compileOnly, apiHelperConfiguration, allowedToUse, allowedToDeclare, classesDirs,
                    allowedAggregatorsToUse, logFilePath, logDependencyInformationToFiles);
        } catch (UnknownDomainObjectException e) {
            throw new IllegalStateException("Dependency analysis plugin must also be applied to the root project", e);
        }
        return resolver.analyzeDependencies();
    }

    @Input
    @Deprecated
    public Boolean getJustWarn() {
        return justWarn;
    }

    @Deprecated
    public void setJustWarn(final boolean justWarn) {
        this.justWarn = justWarn;
    }

    @Input
    public Boolean getWarnUsedUndeclared() {
        return warnUsedUndeclared;
    }

    public void setWarnUsedUndeclared(final boolean warnUsedUndeclared) {
        this.warnUsedUndeclared = warnUsedUndeclared;
    }

    @Input
    public Boolean getWarnUnusedDeclared() {
        return warnUnusedDeclared;
    }

    public void setWarnUnusedDeclared(final boolean warnUnusedDeclared) {
        this.warnUnusedDeclared = warnUnusedDeclared;
    }

    @Input
    public Boolean getWarnCompileOnly() {
        return warnCompileOnly;
    }

    public void setWarnCompileOnly(final boolean warnCompileOnly) {
        this.warnCompileOnly = warnCompileOnly;
    }

    @Input
    public Boolean getLogDependencyInformationToFiles() {
        return logDependencyInformationToFiles;
    }

    public void setLogDependencyInformationToFiles(final boolean logDependencyInformationToFiles) {
        this.logDependencyInformationToFiles = logDependencyInformationToFiles;
    }

    @InputFiles
    @CompileClasspath
    public List<Configuration> getCompileOnly() {
        return compileOnly;
    }

    public void setCompileOnly(final List<Configuration> compileOnly) {
        this.compileOnly = compileOnly;
    }

    @InputFiles
    @CompileClasspath
    public List<Configuration> getRequire() {
        return require;
    }

    public void setRequire(final List<Configuration> require) {
        this.require = require;
    }

    @InputFiles
    @CompileClasspath
    public List<Configuration> getApiHelperConfiguration() {
        return apiHelperConfiguration;
    }

    public void setApiHelperConfiguration(final List<Configuration> apiHelperConfiguration) {
        this.apiHelperConfiguration = apiHelperConfiguration;
    }

    @InputFiles
    @CompileClasspath
    public List<Configuration> getAllowedToUse() {
        return allowedToUse;
    }

    public void setAllowedToUse(final List<Configuration> allowedToUse) {
        this.allowedToUse = allowedToUse;
    }

    @InputFiles
    @CompileClasspath
    public List<Configuration> getAllowedToDeclare() {
        return allowedToDeclare;
    }

    public void setAllowedToDeclare(final List<Configuration> allowedToDeclare) {
        this.allowedToDeclare = allowedToDeclare;
    }

    @InputFiles
    @CompileClasspath
    public List<Configuration> getAllowedAggregatorsToUse() {
        return allowedAggregatorsToUse;
    }

    public void setAllowedAggregatorsToUse(final List<Configuration> allowedAggregatorsToUse) {
        this.allowedAggregatorsToUse = allowedAggregatorsToUse;
    }

    @InputFiles
    @Classpath
    public FileCollection getClassesDirs() {
        return classesDirs;
    }

    public void setClassesDirs(final FileCollection classesDirs) {
        this.classesDirs = classesDirs;
    }

    @OutputFile
    public Path getLogFilePath() {
        return logFilePath;
    }
}

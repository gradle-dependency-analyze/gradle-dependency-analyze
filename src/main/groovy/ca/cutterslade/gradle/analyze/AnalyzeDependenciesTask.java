package ca.cutterslade.gradle.analyze;

import static ca.cutterslade.gradle.analyze.util.ProjectDependencyAnalysisResultHandler.warnAndLogOrFail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.*;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.*;

@CacheableTask
public class AnalyzeDependenciesTask extends DefaultTask {
  private Boolean warnUsedUndeclared = false;
  private Boolean warnUnusedDeclared = false;
  private Boolean warnSuperfluous = false;
  private Boolean warnCompileOnly = false;
  private Boolean logDependencyInformationToFiles = false;
  private List<Configuration> require = new ArrayList<>();
  private List<Configuration> compileOnly = new ArrayList<>();
  private List<Configuration> apiHelperConfiguration = new ArrayList<>();
  private List<Configuration> allowedToUse = new ArrayList<>();
  private List<Configuration> allowedToDeclare = new ArrayList<>();
  private List<Configuration> allowedAggregatorsToUse = new ArrayList<>();
  private final Logger logger;
  private final HashSetValuedHashMap<File, String> artifactClassCache;
  private final ConfigurableFileCollection classesDirs;
  private final RegularFileProperty logFile;
  private final DirectoryProperty buildDirectory;

  @Inject
  @SuppressWarnings("unchecked")
  public AnalyzeDependenciesTask(ProjectLayout projectLayout, ObjectFactory objectFactory) {
    this.classesDirs = objectFactory.fileCollection();
    this.buildDirectory = projectLayout.getBuildDirectory();
    this.logFile = objectFactory.fileProperty();

    this.logger = getLogger();
    try {
      this.artifactClassCache =
          (HashSetValuedHashMap<File, String>)
              getProject()
                  .getRootProject()
                  .getExtensions()
                  .getByName(ProjectDependencyResolver.CACHE_NAME);
    } catch (UnknownDomainObjectException e) {
      throw new IllegalStateException(
          "Dependency analysis plugin must also be applied to the root project", e);
    }
    logFile.convention(
        getBuildDirectory()
            .dir("reports")
            .map(directory -> directory.dir("dependency-analyze").file(getName() + ".log")));
    dependsOn("jar");
  }

  @TaskAction
  public void action() throws IOException {
    final Path logFilePath = getLogFile().get().getAsFile().toPath();
    if (logDependencyInformationToFiles) {
      logger.info("Writing dependency information to {}", logFilePath);
    }

    logger.info(
        "Analyzing dependencies of {} for [require: {}, allowedToUse: {}, allowedToDeclare: {}]",
        getClassesDirs(),
        getRequire(),
        getAllowedToUse(),
        getAllowedToDeclare());
    ProjectDependencyAnalysisResult analysis = getAnalysisResult(logFilePath);

    warnAndLogOrFail(
        analysis,
        warnUsedUndeclared,
        warnUnusedDeclared,
        warnCompileOnly,
        warnSuperfluous,
        logFilePath,
        logDependencyInformationToFiles,
        logger);
  }

  private ProjectDependencyAnalysisResult getAnalysisResult(final Path logFilePath) {
    return new ProjectDependencyResolver(
            logger,
            artifactClassCache,
            require,
            compileOnly,
            apiHelperConfiguration,
            allowedToUse,
            allowedToDeclare,
            classesDirs.getFiles(),
            allowedAggregatorsToUse,
            logFilePath,
            logDependencyInformationToFiles)
        .analyzeDependencies();
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
  public Boolean getWarnSuperfluous() {
    return warnSuperfluous;
  }

  public void setWarnSuperfluous(final boolean warnSuperfluous) {
    this.warnSuperfluous = warnSuperfluous;
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
    this.classesDirs.setFrom(classesDirs);
  }

  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  public DirectoryProperty getBuildDirectory() {
    return buildDirectory;
  }

  @OutputFile
  public RegularFileProperty getLogFile() {
    return logFile;
  }
}

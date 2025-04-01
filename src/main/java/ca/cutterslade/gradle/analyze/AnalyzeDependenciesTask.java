package ca.cutterslade.gradle.analyze;

import static ca.cutterslade.gradle.analyze.util.ProjectDependencyAnalysisResultHandler.warnAndLogOrFail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.*;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

@CacheableTask
public class AnalyzeDependenciesTask extends DefaultTask {
  private Boolean warnUsedUndeclared = false;
  private Boolean warnUnusedDeclared = false;
  private Boolean warnSuperfluous = false;
  private Boolean warnCompileOnly = false;
  private Boolean logDependencyInformationToFiles = false;
  private List<Provider<Configuration>> require = new ArrayList<>();
  private List<Provider<Configuration>> compileOnly = new ArrayList<>();
  private List<Provider<Configuration>> apiHelperConfiguration = new ArrayList<>();
  private List<Provider<Configuration>> allowedToUse = new ArrayList<>();
  private List<Provider<Configuration>> allowedToDeclare = new ArrayList<>();
  private List<Provider<Configuration>> allowedAggregatorsToUse = new ArrayList<>();
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

    // Add a dependency on jar to ensure classes are built
    dependsOn("jar");
  }

  @TaskAction
  public void action() throws IOException {
    final Path logFilePath = getLogFile().get().getAsFile().toPath();
    if (logDependencyInformationToFiles) {
      logger.info("Writing dependency information to {}", logFilePath);
    }

    if (logger.isInfoEnabled()) {
      // Resolve Configuration providers when needed at execution time
      List<Configuration> resolvedRequire =
          require.stream().map(Provider::get).collect(Collectors.toList());
      List<Configuration> resolvedAllowedToUse =
          allowedToUse.stream().map(Provider::get).collect(Collectors.toList());
      List<Configuration> resolvedAllowedToDeclare =
          allowedToDeclare.stream().map(Provider::get).collect(Collectors.toList());

      logger.info(
          "Analyzing dependencies of {} for [require: {}, allowedToUse: {}, allowedToDeclare: {}]",
          getClassesDirs(),
          resolvedRequire,
          resolvedAllowedToUse,
          resolvedAllowedToDeclare);
    }

    ProjectDependencyAnalysisResult analysis =
        new ProjectDependencyResolver(
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
  public List<Provider<Configuration>> getCompileOnly() {
    return compileOnly;
  }

  public void setCompileOnly(final List<Provider<Configuration>> compileOnly) {
    this.compileOnly = compileOnly;
  }

  // For backward compatibility
  public void setCompileOnly(final Configuration compileOnly) {
    this.compileOnly = new ArrayList<>();
    this.compileOnly.add(getProject().provider(() -> compileOnly));
  }

  @InputFiles
  @CompileClasspath
  public List<Provider<Configuration>> getRequire() {
    return require;
  }

  public void setRequire(final List<Provider<Configuration>> require) {
    this.require = require;
  }

  @InputFiles
  @CompileClasspath
  public List<Provider<Configuration>> getApiHelperConfiguration() {
    return apiHelperConfiguration;
  }

  public void setApiHelperConfiguration(
      final List<Provider<Configuration>> apiHelperConfiguration) {
    this.apiHelperConfiguration = apiHelperConfiguration;
  }

  @InputFiles
  @CompileClasspath
  public List<Provider<Configuration>> getAllowedToUse() {
    return allowedToUse;
  }

  public void setAllowedToUse(final List<Provider<Configuration>> allowedToUse) {
    this.allowedToUse = allowedToUse;
  }

  @InputFiles
  @CompileClasspath
  public List<Provider<Configuration>> getAllowedToDeclare() {
    return allowedToDeclare;
  }

  public void setAllowedToDeclare(final List<Provider<Configuration>> allowedToDeclare) {
    this.allowedToDeclare = allowedToDeclare;
  }

  @InputFiles
  @CompileClasspath
  public List<Provider<Configuration>> getAllowedAggregatorsToUse() {
    return allowedAggregatorsToUse;
  }

  public void setAllowedAggregatorsToUse(
      final List<Provider<Configuration>> allowedAggregatorsToUse) {
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

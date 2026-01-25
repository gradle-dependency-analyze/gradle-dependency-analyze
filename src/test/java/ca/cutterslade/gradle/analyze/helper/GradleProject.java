package ca.cutterslade.gradle.analyze.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GradleProject {

  // Helper method to join task names with comma separator
  private String joinTaskNames(final Set<String> taskNames) {
    return taskNames.stream()
        .map(task -> "'" + task + "'")
        .collect(java.util.stream.Collectors.joining(", "));
  }

  private final String name;
  private final boolean rootProject;

  private boolean warnCompileOnly = false;
  private boolean warnUsedUndeclared = false;
  private boolean ignoreUsedUndeclared = true;
  private boolean warnUnusedDeclared = false;
  private boolean logDependencyInformationToFiles = false;
  private final Set<GradleProject> subProjects = new LinkedHashSet<>();
  private final Set<GroovyClass> mainClasses = new LinkedHashSet<>();
  private final Set<GroovyClass> testClasses = new LinkedHashSet<>();
  private final Set<GroovyClass> testFixturesClasses = new LinkedHashSet<>();
  private final Set<String> plugins = new LinkedHashSet<>();
  private final Set<String> allProjectPlugins = new LinkedHashSet<>();
  private final Set<GradleDependency> dependencies = new LinkedHashSet<>();
  private String repositories;
  private String platformConfiguration = "";
  private final Map<String, String> additionalTasks = new LinkedHashMap<>();

  public GradleProject(final String name) {
    this(name, false);
  }

  public GradleProject(final String name, final boolean rootProject) {
    this.name = name;
    this.rootProject = rootProject;
  }

  public GradleProject withSubProject(final GradleProject project) {
    subProjects.add(project);
    return this;
  }

  public GradleProject withMainClass(final GroovyClass clazz) {
    mainClasses.add(clazz);
    return this;
  }

  public GradleProject withTestClass(final GroovyClass clazz) {
    testClasses.add(clazz);
    return this;
  }

  public GradleProject withTestFixturesClass(final GroovyClass clazz) {
    testFixturesClasses.add(clazz);
    return this;
  }

  public GradleProject withPlugin(final String plugin) {
    plugins.add(plugin);
    return this;
  }

  public GradleProject withAllProjectsPlugin(final String plugin) {
    allProjectPlugins.add(plugin);
    return this;
  }

  public GradleProject withWarnCompileOnly(final boolean value) {
    warnCompileOnly = value;
    return this;
  }

  public GradleProject withWarnUsedUndeclared(final boolean value) {
    warnUsedUndeclared = value;
    return this;
  }

  public GradleProject withIgnoreUsedUndeclared(final boolean value) {
    ignoreUsedUndeclared = value;
    return this;
  }

  public GradleProject withWarnUnusedDeclared(final boolean value) {
    warnUnusedDeclared = value;
    return this;
  }

  public GradleProject logDependencyInformationToFiles() {
    logDependencyInformationToFiles = true;
    return this;
  }

  public GradleProject withDependency(final GradleDependency dep) {
    dependencies.add(dep);
    return this;
  }

  public GradleProject withGradleDependency(final String configuration) {
    dependencies.add(
        new GradleDependency().setConfiguration(configuration).setReference("localGroovy()"));
    return this;
  }

  public GradleProject withAggregator(final GradleDependency aggregator) {
    dependencies.add(aggregator);
    return this;
  }

  public GradleProject withAdditionalTask(final String taskName, final String buildGradleSnippet) {
    additionalTasks.put(taskName, buildGradleSnippet);
    return this;
  }

  public GradleProject withMavenRepositories() {
    repositories = "repositories {\n" + "    mavenLocal()\n" + "    mavenCentral()\n" + "}\n";
    return this;
  }

  public GradleProject applyPlatformConfiguration() {
    platformConfiguration =
        "configurations {\n"
            + "    myPlatform {\n"
            + "        canBeResolved = false\n"
            + "        canBeConsumed = false\n"
            + "    }\n"
            + "}\n"
            + "configurations.all {\n"
            + "    if (canBeResolved) {\n"
            + "        extendsFrom(configurations.myPlatform)\n"
            + "    }\n"
            + "}\n"
            + "dependencies {\n"
            + "    myPlatform platform(project(':platform'))\n"
            + "}\n";
    return this;
  }

  public void create(final Path root) throws IOException {
    additionalTasks.putIfAbsent("build", "");

    Files.createDirectories(root);
    for (final GradleProject project : subProjects) {
      project.create(root.resolve(project.name));
    }

    createBuildGradle(root);
    createSettingsGradle(root);

    if (!mainClasses.isEmpty()) {
      createClasses(root, "src/main/groovy", mainClasses);
    }
    if (!testClasses.isEmpty()) {
      createClasses(root, "src/test/groovy", testClasses);
    }
    if (!testFixturesClasses.isEmpty()) {
      createClasses(root, "src/testFixtures/groovy", testFixturesClasses);
    }
  }

  private static void createClasses(
      final Path root, final String dir, final Set<GroovyClass> classes) throws IOException {
    final Path sourceDir = root.resolve(dir);
    Files.createDirectories(sourceDir);
    for (final GroovyClass clazz : classes) {
      clazz.create(sourceDir);
    }
  }

  private void createSettingsGradle(final Path root) throws IOException {
    final StringBuilder settingsGradle = new StringBuilder();
    if (name != null) {
      settingsGradle.append("rootProject.name = '").append(name).append("'\n");
    }

    for (final GradleProject subProject : subProjects) {
      settingsGradle.append("include(':").append(subProject.name).append("')\n");
    }

    if (settingsGradle.length() > 0) {
      Files.write(
          root.resolve("settings.gradle"),
          settingsGradle.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private void createBuildGradle(final Path root) throws IOException {
    final StringBuilder buildGradle = new StringBuilder();
    if (!plugins.isEmpty()) {
      buildGradle.append("plugins {\n");
      plugins.forEach(plugin -> buildGradle.append("  id '").append(plugin).append("'\n"));
      buildGradle.append("}\n");
    }
    if (plugins.contains("java-platform")) {
      buildGradle.append("javaPlatform {\n").append("    allowDependencies()\n").append("}\n");
    }
    buildGradle.append(repositories != null ? repositories : "");
    if (!dependencies.isEmpty()) {
      buildGradle.append("dependencies {\n");
      for (final GradleDependency dep : dependencies) {
        buildGradle.append("  ").append(dep.get()).append("\n");
      }
      buildGradle.append("}\n");
    }

    buildGradle
        .append("\ndefaultTasks ")
        .append(joinTaskNames(additionalTasks.keySet()))
        .append("\n");

    buildGradle.append(platformConfiguration);

    if (rootProject && !allProjectPlugins.isEmpty()) {
      buildGradle.append("allprojects {\n");
      for (final String plugin : allProjectPlugins) {
        buildGradle.append("  apply plugin: '").append(plugin).append("'\n");
      }
      buildGradle.append("}\n");
    }

    if (plugins.contains("ca.cutterslade.analyze")
        && (warnUsedUndeclared
            || !ignoreUsedUndeclared  // Only configure if different from default (true)
            || warnUnusedDeclared
            || logDependencyInformationToFiles
            || warnCompileOnly)) {
      buildGradle.append("tasks.named('analyzeClassesDependencies').configure {\n");
      if (warnCompileOnly) {
        buildGradle.append("  warnCompileOnly = ").append(true).append("\n");
      }
      if (warnUsedUndeclared) {
        buildGradle.append("  warnUsedUndeclared = ").append(true).append("\n");
      }
      if (!ignoreUsedUndeclared) {  // Only set if false (different from default)
        buildGradle.append("  ignoreUsedUndeclared = ").append(false).append("\n");
      }
      if (warnUnusedDeclared) {
        buildGradle.append("  warnUnusedDeclared = ").append(true).append("\n");
      }
      if (logDependencyInformationToFiles) {
        buildGradle.append("  logDependencyInformationToFiles = ").append(true).append("\n");
      }
      buildGradle.append("}\n");
      
      // Also configure test task with same settings
      buildGradle.append("tasks.named('analyzeTestClassesDependencies').configure {\n");
      if (warnCompileOnly) {
        buildGradle.append("  warnCompileOnly = ").append(true).append("\n");
      }
      if (warnUsedUndeclared) {
        buildGradle.append("  warnUsedUndeclared = ").append(true).append("\n");
      }
      if (!ignoreUsedUndeclared) {  // Only set if false (different from default)
        buildGradle.append("  ignoreUsedUndeclared = ").append(false).append("\n");
      }
      if (warnUnusedDeclared) {
        buildGradle.append("  warnUnusedDeclared = ").append(true).append("\n");
      }
      if (logDependencyInformationToFiles) {
        buildGradle.append("  logDependencyInformationToFiles = ").append(true).append("\n");
      }
      buildGradle.append("}\n");
    }

    additionalTasks.values().forEach(task -> buildGradle.append(task).append("\n"));

    Files.write(
        root.resolve("build.gradle"), buildGradle.toString().getBytes(StandardCharsets.UTF_8));
  }
}

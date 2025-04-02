package ca.cutterslade.gradle.analyze.helper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GradleProject {

  // Helper method to join task names with comma separator
  private String joinTaskNames(Set<String> taskNames) {
    return taskNames.stream()
        .map(task -> "'" + task + "'")
        .collect(java.util.stream.Collectors.joining(", "));
  }

  private final String name;
  private final boolean rootProject;

  private boolean warnCompileOnly = false;
  private boolean warnUsedUndeclared = false;
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

  public GradleProject(String name) {
    this(name, false);
  }

  public GradleProject(String name, boolean rootProject) {
    this.name = name;
    this.rootProject = rootProject;
  }

  public GradleProject withSubProject(GradleProject project) {
    subProjects.add(project);
    return this;
  }

  public GradleProject withMainClass(GroovyClass clazz) {
    mainClasses.add(clazz);
    return this;
  }

  public GradleProject withTestClass(GroovyClass clazz) {
    testClasses.add(clazz);
    return this;
  }

  public GradleProject withTestFixturesClass(GroovyClass clazz) {
    testFixturesClasses.add(clazz);
    return this;
  }

  public GradleProject withPlugin(String plugin) {
    plugins.add(plugin);
    return this;
  }

  public GradleProject withAllProjectsPlugin(String plugin) {
    allProjectPlugins.add(plugin);
    return this;
  }

  public GradleProject withWarnCompileOnly(boolean value) {
    warnCompileOnly = value;
    return this;
  }

  public GradleProject withWarnUsedUndeclared(boolean value) {
    warnUsedUndeclared = value;
    return this;
  }

  public GradleProject withWarnUnusedDeclared(boolean value) {
    warnUnusedDeclared = value;
    return this;
  }

  public GradleProject logDependencyInformationToFiles() {
    logDependencyInformationToFiles = true;
    return this;
  }

  public GradleProject withDependency(GradleDependency dep) {
    dependencies.add(dep);
    return this;
  }

  public GradleProject withGradleDependency(String configuration) {
    dependencies.add(
        new GradleDependency().setConfiguration(configuration).setReference("localGroovy()"));
    return this;
  }

  public GradleProject withAggregator(GradleDependency aggregator) {
    dependencies.add(aggregator);
    return this;
  }

  public GradleProject withAdditionalTask(String taskName, String buildGradleSnippet) {
    additionalTasks.put(taskName, buildGradleSnippet);
    return this;
  }

  public GradleProject withMavenRepositories() {
    repositories = "repositories {\n" + "    mavenLocal()\n" + "    mavenCentral()\n" + "}\n";
    return this;
  }

  public GradleProject applyPlatformConfiguration() {
    platformConfiguration =
        ""
            + "configurations {\n"
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

  public void create(File root) {
    additionalTasks.putIfAbsent("build", "");

    root.mkdirs();
    subProjects.forEach(project -> project.create(new File(root, project.name)));

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

  private static void createClasses(File root, String dir, Set<GroovyClass> classes) {
    File sourceDir = new File(root, dir);
    if (!sourceDir.mkdirs() && !sourceDir.exists()) {
      throw new IllegalStateException("Could not create source dir " + sourceDir);
    }

    for (GroovyClass clazz : classes) {
      clazz.create(sourceDir);
    }
  }

  private void createSettingsGradle(File root) {
    StringBuilder settingsGradle = new StringBuilder();
    if (name != null) {
      settingsGradle.append("rootProject.name = '").append(name).append("'\n");
    }

    for (GradleProject subProject : subProjects) {
      settingsGradle.append("include(':").append(subProject.name).append("')\n");
    }

    if (settingsGradle.length() > 0) {
      try (java.io.FileWriter writer = new java.io.FileWriter(new File(root, "settings.gradle"))) {
        writer.write(settingsGradle.toString());
      } catch (IOException e) {
        throw new RuntimeException("Could not write settings.gradle file", e);
      }
    }
  }

  private void createBuildGradle(final File root) {
    StringBuilder buildGradle = new StringBuilder();
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
      for (GradleDependency dep : dependencies) {
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
      for (String plugin : allProjectPlugins) {
        buildGradle.append("  apply plugin: '").append(plugin).append("'\n");
      }
      buildGradle.append("}\n");
    }

    if (warnUsedUndeclared
        || warnUnusedDeclared
        || logDependencyInformationToFiles
        || warnCompileOnly) {
      buildGradle.append("tasks.named('analyzeClassesDependencies').configure {\n");
      if (warnCompileOnly) {
        buildGradle.append("  warnCompileOnly = ").append(true).append("\n");
      }
      if (warnUsedUndeclared) {
        buildGradle.append("  warnUsedUndeclared = ").append(true).append("\n");
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

    try (java.io.FileWriter writer = new java.io.FileWriter(new File(root, "build.gradle"))) {
      writer.write(buildGradle.toString());
    } catch (IOException e) {
      throw new RuntimeException("Could not write build.gradle file", e);
    }
  }
}

package ca.cutterslade.gradle.analyze;

import ca.cutterslade.gradle.analyze.util.ConfigurationSetupUtils;
import ca.cutterslade.gradle.analyze.util.GradleVersionUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.GradleVersion;

public class AnalyzeDependenciesPlugin implements Plugin<Project> {

  @Override
  public void apply(final Project project) {
    if (project.getRootProject() == project) {
      project
          .getRootProject()
          .getExtensions()
          .add(ProjectDependencyResolver.CACHE_NAME, new HashSetValuedHashMap<>());
    }

    project
        .getPlugins()
        .withId(
            "java",
            plugin -> {
              final TaskProvider<Task> commonTask =
                  project
                      .getTasks()
                      .register(
                          "analyzeDependencies",
                          task -> {
                            task.setGroup("Verification");
                            task.setDescription("Analyze project for dependency issues.");
                          });

              project
                  .getTasks()
                  .named("check")
                  .configure(
                      task -> {
                        task.dependsOn(commonTask);
                      });

              // Create a map to store all analyze tasks by source set
              final Map<String, Object> analyzeTasksBySourceSet = new HashMap<>();

              final SourceSetContainer sourceSets =
                  project.getExtensions().getByType(SourceSetContainer.class);
              sourceSets.all(
                  sourceSet -> {
                    // Create configurations
                    project
                        .getConfigurations()
                        .create(
                            sourceSet.getTaskName("permit", "unusedDeclared"),
                            config -> {
                              config.setCanBeConsumed(false);
                              config.setCanBeResolved(true);
                            });

                    project
                        .getConfigurations()
                        .create(
                            sourceSet.getTaskName("permit", "usedUndeclared"),
                            config -> {
                              config.setCanBeConsumed(false);
                              config.setCanBeResolved(true);
                            });

                    project
                        .getConfigurations()
                        .create(
                            sourceSet.getTaskName("permit", "aggregatorUse"),
                            config -> {
                              config.setCanBeConsumed(false);
                              config.setCanBeResolved(true);
                              config
                                  .getAttributes()
                                  .attribute(
                                      Usage.USAGE_ATTRIBUTE,
                                      project.getObjects().named(Usage.class, Usage.JAVA_API));
                            });

                    // Create helper configurations
                    final String apiHelperName = sourceSet.getTaskName("apiHelper", "");
                    project
                        .getConfigurations()
                        .create(
                            apiHelperName,
                            config -> {
                              config.setCanBeConsumed(false);
                              config.setCanBeResolved(true);
                            });

                    final String compileOnlyHelperName =
                        sourceSet.getTaskName("compileOnlyHelper", "");
                    project
                        .getConfigurations()
                        .create(
                            compileOnlyHelperName,
                            config -> {
                              config.setCanBeConsumed(false);
                              config.setCanBeResolved(true);
                            });

                    // IMPORTANT: Set up the configuration hierarchies immediately when everything
                    // is created
                    project.afterEvaluate(
                        p -> {
                          // We do this in afterEvaluate to ensure the source set configurations are
                          // fully created
                          ConfigurationSetupUtils.setupConfigurationHierarchy(
                              project, apiHelperName, sourceSet.getApiConfigurationName());

                          ConfigurationSetupUtils.setupConfigurationHierarchy(
                              project,
                              compileOnlyHelperName,
                              sourceSet.getCompileOnlyConfigurationName());
                        });

                    final TaskProvider<AnalyzeDependenciesTask> analyzeTask =
                        project
                            .getTasks()
                            .register(
                                sourceSet.getTaskName("analyze", "classesDependencies"),
                                AnalyzeDependenciesTask.class,
                                task -> {
                                  task.setGroup("Verification");
                                  task.setDescription(
                                      "Analyze project for dependency issues related to "
                                          + sourceSet.getName()
                                          + " source set.");

                                  // Set up task dependencies for ALL compilation tasks to ensure we
                                  // have all classes
                                  // First, depend on the classes task for this source set
                                  final String classesTaskName = sourceSet.getClassesTaskName();
                                  if (project.getTasks().getNames().contains(classesTaskName)) {
                                    task.dependsOn(classesTaskName);
                                  }

                                  // Make main analyze task depend on ALL compile tasks, lifecycle
                                  // tasks and test tasks
                                  if (sourceSet.getName().equals("main")) {
                                    // Depend on all test tasks
                                    if (project.getTasks().getNames().contains("test")) {
                                      task.dependsOn("test");
                                    }

                                    if (project.getTasks().getNames().contains("testClasses")) {
                                      task.dependsOn("testClasses");
                                    }

                                    // Find all tasks that end with Java, Groovy, Kotlin, or Scala
                                    // and have a compile prefix
                                    final List<String> allCompileTasks =
                                        project.getTasks().getNames().stream()
                                            .filter(
                                                name ->
                                                    (name.startsWith("compile")
                                                            && (name.endsWith("Java")
                                                                || name.endsWith("Groovy")
                                                                || name.endsWith("Kotlin")
                                                                || name.endsWith("Scala")))
                                                        || name.endsWith("Classes"))
                                            .collect(Collectors.toList());

                                    // Depend on all compile tasks
                                    for (final String compileTask : allCompileTasks) {
                                      task.dependsOn(compileTask);
                                    }

                                    // Also depend on all tests and compile test tasks
                                    final List<String> allTestTasks =
                                        project.getTasks().getNames().stream()
                                            .filter(
                                                name ->
                                                    name.contains("Test") || name.contains("test"))
                                            .collect(Collectors.toList());

                                    for (final String testTask : allTestTasks) {
                                      // Don't create circular dependencies with our own analyze
                                      // tasks
                                      if (!testTask.startsWith("analyze")) {
                                        task.dependsOn(testTask);
                                      }
                                    }
                                  }
                                  // For test tasks, make sure to depend on the compile tasks for
                                  // that source set
                                  else {
                                    final String compilePrefix = sourceSet.getCompileTaskName("");
                                    final List<String> specificCompileTasks =
                                        project.getTasks().getNames().stream()
                                            .filter(
                                                name ->
                                                    name.startsWith(compilePrefix)
                                                        && (name.endsWith("Java")
                                                            || name.endsWith("Groovy")
                                                            || name.endsWith("Kotlin")
                                                            || name.endsWith("Scala")))
                                            .collect(Collectors.toList());

                                    for (final String compileTask : specificCompileTasks) {
                                      task.dependsOn(compileTask);
                                    }
                                  }

                                  // Always depend on jar
                                  task.dependsOn(project.getTasks().named("jar"));

                                  // Use anonymous inner class instead of lambda for task action
                                  task.doFirst(
                                      new org.gradle.api.Action<Task>() {
                                        @Override
                                        public void execute(final Task t) {
                                          if (project
                                              .getConfigurations()
                                              .getNames()
                                              .contains("providedRuntime")) {
                                            final Provider<Configuration> providedRuntimeConfig =
                                                project
                                                    .getConfigurations()
                                                    .named("providedRuntime");
                                            // We need to get() here because we're checking
                                            // properties inside doFirst action
                                            if (!providedRuntimeConfig
                                                .get()
                                                .getResolvedConfiguration()
                                                .getFirstLevelModuleDependencies()
                                                .isEmpty()) {
                                              GradleVersionUtil
                                                  .warnAboutWarPluginBrokenWhenUsingProvidedRuntime(
                                                      GradleVersion.current(), project.getLogger());
                                            }
                                          }
                                        }
                                      });
                                });

                    // Store the task in our map
                    analyzeTasksBySourceSet.put(sourceSet.getName(), analyzeTask);

                    commonTask.configure(
                        task -> {
                          task.dependsOn(analyzeTask);
                        });

                    project.afterEvaluate(
                        p -> {
                          analyzeTask.configure(
                              task -> {
                                // Create a list with a single provider
                                final List<Provider<Configuration>> requireList = new ArrayList<>();
                                requireList.add(
                                    project
                                        .getConfigurations()
                                        .named(sourceSet.getCompileClasspathConfigurationName()));
                                task.setRequire(requireList);

                                // Set up the providers for helper configs
                                task.setCompileOnly(
                                    ConfigurationSetupUtils.wrapInList(
                                        project
                                            .getConfigurations()
                                            .named(
                                                sourceSet.getTaskName("compileOnlyHelper", ""))));

                                task.setApiHelperConfiguration(
                                    ConfigurationSetupUtils.wrapInList(
                                        project
                                            .getConfigurations()
                                            .named(sourceSet.getTaskName("apiHelper", ""))));

                                // List of allowed aggregators to use
                                task.setAllowedAggregatorsToUse(
                                    ConfigurationSetupUtils.wrapInList(
                                        project
                                            .getConfigurations()
                                            .named(
                                                sourceSet.getTaskName("permit", "aggregatorUse"))));

                                // List of allowed to use
                                final List<Provider<Configuration>> allowedToUseList =
                                    new ArrayList<>();
                                allowedToUseList.add(
                                    project
                                        .getConfigurations()
                                        .named(sourceSet.getTaskName("permit", "usedUndeclared")));

                                // List of allowed to declare
                                final List<Provider<Configuration>> allowedToDeclareList =
                                    new ArrayList<>();
                                allowedToDeclareList.add(
                                    project
                                        .getConfigurations()
                                        .named(sourceSet.getTaskName("permit", "unusedDeclared")));

                                if (sourceSet.getName().equals("test")) {
                                  allowedToUseList.add(
                                      project.getConfigurations().named("compileClasspath"));
                                  if (project
                                      .getConfigurations()
                                      .getNames()
                                      .contains("testFixturesCompileClasspath")) {
                                    allowedToUseList.add(
                                        project
                                            .getConfigurations()
                                            .named("testFixturesCompileClasspath"));
                                  }

                                  // Test depends on main analyze task
                                  if (analyzeTasksBySourceSet.containsKey("main")) {
                                    task.dependsOn(analyzeTasksBySourceSet.get("main"));
                                  }
                                }

                                if (sourceSet.getName().equals("testFixtures")) {
                                  allowedToUseList.add(
                                      project.getConfigurations().named("testCompileClasspath"));

                                  // TestFixtures depends on main analyze task
                                  if (analyzeTasksBySourceSet.containsKey("main")) {
                                    task.dependsOn(analyzeTasksBySourceSet.get("main"));
                                  }
                                }

                                task.setAllowedToUse(allowedToUseList);
                                task.setAllowedToDeclare(allowedToDeclareList);
                                task.setClassesDirs(sourceSet.getOutput().getClassesDirs());
                              });
                        });
                  });
            });
  }
}

# gradle-dependency-analyze

[![Download](https://api.bintray.com/packages/wesley/maven/gradle-dependency-analyze/images/download.svg) ](https://bintray.com/wesley/maven/gradle-dependency-analyze/_latestVersion)
[![Build Status](https://travis-ci.org/wfhartford/gradle-dependency-analyze.svg?branch=master)](https://travis-ci.org/wfhartford/gradle-dependency-analyze)

Dependency analysis plugin for gradle.

This plugin is based on the work in a gist at https://gist.github.com/kellyrob99/4334483. This plugin is possible thanks to this work by [Kelly Robinson](https://github.com/kellyrob99); thank you.

This plugin attempts to replicate the functionality of the maven dependency plugin's analyze goals which fail the build if dependencies are declared but not used or used but not declared.

The plugin is available from both JCenter and the gradle plugin repository, so it can be added to your build with the following:

Using the plugin DSL:
```gradle
plugins {
  id "ca.cutterslade.analyze" version "1.4.2"
}
```

Using legacy plugin application:
```gradle
buildscript {
  repositories {
    maven { url "https://plugins.gradle.org/m2/" }
    // If you prefer, replace this maven repo with `jcenter()`
  }
  dependencies {
    classpath 'ca.cutterslade.gradle:gradle-dependency-analyze:1.4.2'
  }
}

apply plugin: 'java'
apply plugin: 'ca.cutterslade.analyze'
```

When applying this plugin to a multi-project build, it should be applied the root project as well as all sub-projects for which dependency analysis is needed. A common pattern is to apply this plugin to all projects and the java plugin to only the sub-projects:
```gradle
allprojects {
  apply plugin: 'ca.cutterslade.analyze'
}
subprojects {
  apply plugin: 'java'
}
```

# Sample Output
If the dependency analysis finds issues, it will normally cause the build to fail, and print a list of the issues that were found, similar to the following:
```
* What went wrong:
Execution failed for task ':analyzeClassesDependencies'.
> Dependency analysis found issues.
  usedUndeclaredArtifacts: 
   - ch.qos.logback:logback-core:1.2.3@jar
  unusedDeclaredArtifacts: 
   - com.google.guava:guava:25.1-jre@jar
   - commons-io:commons-io:2.5@jar
   - commons-lang:commons-lang:2.4@jar
   - net.sf.json-lib:json-lib:2.3:jdk15@jar
```

# Tasks
This plugin will add three tasks to your project: `analyzeClassesDependencies`, `analyzeTestClassesDependencies`, and `analyzeDependencies`.
## analyzeClassesDependencies
This task depends on the `classes` task and analyzes the dependencies of the main source set's output directory. This ensures that all dependencies of the classes are declared in the `compile`, `compileOnly`, or [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) configuration. It also ensures the inverse, that all of the dependencies of these configurations are used by classes; use of the `permitUnusedDeclared` configuration allows for exceptions to this restriction.
## analyzeTestClassesDependencies
This task depends on the `testClasses` task and analyzes the dependencies of the test source set's output directory. This ensures that all dependencies of the classes are declared in the `testCompile` or `testCompileOnly` configuration. It also ensures the inverse, that all of the dependencies of these configurations are used by classes; use of the `permitTestUnusedDeclared` configuration allows for exceptions to this restriction.
## analyzeDependencies
This task depends on the `analyzeClassesDependencies` and `analyzeTestClassesDependencies` tasks, and does nothing on its own. A dependency on this task is added to the `check` task.

# Configurations
This plugin adds four configurations which may be used to define dependencies which should be handled in a special way. These configurations have no impact on the build outside of this plugin.
* `permitUnusedDeclared`
* `permitTestUnusedDeclared`
* `permitUsedUndeclared`
* `permitTestUsedUndeclared`

Adding dependencies to `permitUnusedDeclared` or `permitTestUnusedDeclared` causes the dependency analyzer to ignore cases where the dependencies are declared but not used. Adding dependencies to `permitUsedUndeclared` or `permitTestUsedUndeclared` causes the dependency analyzer to ignore cases where the dependencies used but not declared. 

## Examples
Using these configurations to allow exceptions to the rules is as simple as adding a dependency to your project. The snippet below will provide a compile-time dependency on the JSP API, but the plugin will not complain if it is not used.
```gradle
dependencies {
  compile 'javax.servlet:jsp-api:2.0'
  permitUnusedDeclared 'javax.servlet:jsp-api:2.0'
}
```

# Task Configuration
The plugin is not especially configurable, but each task can be configured to log a warning about dependency issues rather than breaking the build like so:
```gradle
analyzeClassesDependencies {
  justWarn = true
}

analyzeTestClassesDependencies {
  justWarn = true
}
```

## Disabling/enabling the plugin
In addition to using the `justWarn`-property, many cases want the build to fail only under given conditions (i.e nightly builds or integration builds). This can be achieved by disabling and enabling dependency analyzing in the following manner.

```gradle
if (!project.hasProperty('analyzeDependencies')) {
  tasks.analyzeClassesDependencies.enabled = false
  tasks.analyzeTestClassesDependencies.enabled = false
  tasks.analyzeDependencies.enabled = false
}
```

## Custom task instances
Applying the plugin creates and configures two instances of the `AnalyzeDependenciesPlugin` task. These two instances, `analyzeClassesDependencies` and `analyzeTestClassesDependencies`, are configured to verify the main and test source set dependencies respectively. Additional instances of this task type can be created and configured in addition to, or instead of, the instances created by the plugin. This may be appropriate when setting up more complex project configurations, or using other plugins which introduce their own configurations.

Example:
```gradle
task analyzeCustomClassesDependencies(type: AnalyzeDependenciesTask, dependsOn: customClasses) {
  // Set to true to print a warning rather than fail the build if the dependency analysis fails
  justWarn = false

  // List of configurations which the analyzed output is required to use 
  require = [ configurations.customCompile, configuration.customCompileOnly ]

  // List of configurations which the analyzed output may use but is not required to
  allowedToUse = [ configurations.compile, configurations.provided ]
  
  // List of configurations which the analyzed output is not required to use, even if dependencies are present in the 'require' list above
  allowedToDeclare = [ configurations.permitCustomUnusedDeclared ]
  
  // Location of class output directories to analyze
  classesDirs = sourceSets.custom.output.classesDirs
}

// Add the new task as a dependency of the main analyzeDependencies task
analyzeDependencies.dependsOn analyzeCustomClassesDependencies
```

Users of the `java-library` plugin no longer need to configure custom tasks, and should upgrade to version 1.4 as soon as practical.

For more practical examples, see the [plugin source](https://github.com/wfhartford/gradle-dependency-analyze/blob/master/src/main/groovy/ca/cutterslade/gradle/analyze/AnalyzeDependenciesPlugin.groovy).

# Version 1.4
Version 1.4 of this plugin adds built in support for the `java-library` plugin, which has been the recommended default for quite a while. Previously tasks had to be customised to analyze the correct configurations.

# Version 1.3
Version 1.3 of this plugin introduces only minor functional changes, but adds support for Java version 9, 10, and 11, while dropping support for Java versions 6 and 7.

The dependency analyzer has been upgraded to version `1.10`, this new version adds detection of inlined dependencies, which can cause some false positives (the lack of this detection used to cause false negatives). In order to assist in working around these false positives, two new configurations have been added to the plugin:
* `permitUsedUndeclared`
* `permitTestUsedUndeclared`

These configurations are described above.

# Version 1.2
Version 1.2 of this plugin introduces a couple significant changes.

* For multi project builds, the plugin must now be applied to the root project. If it has not been applied to the root project, the build will fail with the message `Dependency analysis plugin must also be applied to the root project`.
* The plugin will no longer fail to apply if the java plugin has not been applied. Applying this plugin to a project without the java plugin will have no effect.
* The plugin no longer caches caches dependency information in a static cache which would persist across executions when the gradle daemon was in use. It now caches dependency information in the root project. This represents a small performance penalty but avoids a potential issue if a dependency file is modified, and a potential memory leak if the path of dependency files changes regularly.
* The tasks now produce output files at `$buildDir/dependency-analyse/$taskName`. This contains the exception message if the task causes the build to fail, or is empty if the task does not cause the build to fail.
* The tasks now specify inputs and outputs allowing gradle to consider a task up-to-date if nothing has changed.
* The tasks allows caching of outputs on gradle versions which support the task output cache. This allows the task work to be skipped even on clean builds if an appropriate cached result exists.
* Tasks will now appear in the listing produced by `gradle tasks` under the Verification group.

## Migration from 1.1
Migrating from version 1.1 to version 1.2 of the plugin should be very simple. Most users will not have to make any changes, users with multi-project builds will have to ensure that the plugin is applied to the root project. This can be accomplished by applying the plugin in the `allprojects {}` block.

# Version 1.1
Version 1.1 of this plugin introduced a couple significant changes.

* The plugin now supports the `compileOnly` and `testCompileOnly` configurations introduced by gradle in version 2.12. This feature was discussed in detail in a [posting on the gradle blog](https://gradle.org/blog/compile-only-dependencies/). These configurations should generally be used where [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) would have been used.
* The `permitUnusedDeclared` and `permitTestUnusedDeclared` configurations were introduced to allow for specific exceptions to the restriction which requires all declared dependencies to be used.
* If the project makes use of the [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) configuration, these dependencies are now treated the same as the `compile` configuration; specifically, dependencies of the `provided` configuration must be used by compiled class files. Previously, the `provided` configuration was an exception to that rule, allowing for the type of exception now supported by the `permitUnusedDeclared` configuration.

## Migration from 1.0
If you previously made use of the [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) configuration, upgrading to version 1.1 of this plugin may cause dependency analysis failures, since the `provided` configuration is now treated in the same way as the `compile` configuration. After investigating these failures to ensure that they do not represent a misconfiguration of the project dependencies, the offending dependencies can be added to the `permitUnusedDeclared` configuration to suppress the failure.

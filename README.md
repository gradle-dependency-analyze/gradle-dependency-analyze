# gradle-dependency-analyze

[![Download](https://api.bintray.com/packages/wesley/maven/gradle-dependency-analyze/images/download.svg) ](https://bintray.com/wesley/maven/gradle-dependency-analyze/_latestVersion)
[![Build Status](https://travis-ci.org/wfhartford/gradle-dependency-analyze.svg?branch=master)](https://travis-ci.org/wfhartford/gradle-dependency-analyze)

Dependency analysis plugin for gradle.

This plugin is based on the work in a gist at https://gist.github.com/anonymous/4334439, if you're the original author, thank you, and please let me know.

This plugin attempts to replicate the functionality of the maven dependency plugin's analyze goals which fail the build if dependencies are declared but not used or used but not declared.

The plugin is available from the JCenter repository, so it can be added to your build with the following:
```gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'ca.cutterslade.gradle:gradle-dependency-analyze:1.1.1'
  }
}

apply plugin: 'java'
// Dependency analysis plugin must be applied after the java plugin.
apply plugin: 'ca.cutterslade.analyze'
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
This plugin adds two configurations which may be used to define dependencies which should be handled in a special way. These configurations have no impact on the build outside of this plugin.
* `permitUnusedDeclared`
* `permitTestUnusedDeclared`

Adding dependencies to one of these configurations causes the dependency analyzer to ignore cases where the dependencies declared but not used.

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

# Version 1.1
Version 1.1 of this plugin introduced a couple significant changes.

* The plugin now supports the `compileOnly` and `testCompileOnly` configurations introduced by gradle in version 2.12. This feature was discussed in detail in a [posting on the gradle blog](https://gradle.org/blog/compile-only-dependencies/). These configurations should generally be used where [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) would have been used.
* The `permitUnusedDeclared` and `permitTestUnusedDeclared` configurations were introduced to allow for specific exceptions to the restriction which requires all declared dependencies to be used.
* If the project makes use of the [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) configuration, these dependencies are now treated the same as the `compile` configuration; specifically, dependencies of the `provided` configuration must be used by compiled class files. Previously, the `provided` configuration was an exception to that rule, allowing for the type of exception now supported by the `permitUnusedDeclared` configuration.

## Migration from 1.0
If you previously made use of the [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) configuration, upgrading to version 1.1 of this plugin may cause dependency analysis failures, since the `provided` configuration is now treated in the same way as the `compile` configuration. After investigating these failures to ensure that they do not represent a misconfiguration of the project dependencies, the offending dependencies can be added to the `permitUnusedDeclared` configuration to suppress the failure.

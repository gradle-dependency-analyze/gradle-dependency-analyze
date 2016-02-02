# gradle-dependency-analyze

[![Build Status](https://travis-ci.org/wfhartford/gradle-dependency-analyze.svg?branch=master)](https://travis-ci.org/wfhartford/gradle-dependency-analyze)

Dependency analysis plugin for gradle

This plugin is based on the work in a gist at https://gist.github.com/anonymous/4334439, if you're the original author, thank you, and please let me know.

This plugin attempts to replicate the functionality of the maven dependency plugin's analyze goals which fail the build if dependencies are declared but not used or used but not declared.

The plugin is available from the JCenter repository, so it can be added to your build with the following:
```gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'ca.cutterslade.gradle:gradle-dependency-analyze:1.0.3'
  }
}

apply plugin: 'java'
// Dependency analysis plugin must be applied after the java plugin.
apply plugin: 'ca.cutterslade.analyze'
```
# Tasks
This plugin will add three tasks to your project: `analyzeClassesDependencies`, `analyzeTestClassesDependencies`, and `analyzeDependencies`.
## analyzeClassesDependencies
This task depends on the `classes` task and analyzes the dependencies of the main source set's output directory. This ensures that all dependencies declared in the `compile` configuration are used by classes, and that all dependencies of the classes are declared in the `compile` or `provided` configurations (see [Nebula Extra Configurations](https://github.com/nebula-plugins/gradle-extra-configurations-plugin)).
## analyzeTestClassesDependencies
This task depends on the `testClasses` task and analyzes the dependencies of the test source set's output directory. This ensures that all dependencies declared in the `testCompile` configuration are used by classes, and that all dependencies of the classes are declared in the `testCompile`, `compile`, or `provided` configurations.
## analyzeDependencies
This task depends on the `analyzeClassesDependencies` and `analyzeTestClassesDependencies` tasks, and does nothing on its own. A dependency on this task is added to the `check` task.

# Configuration
The plugin is not especially configurable, but each task can be configured to log a warning about dependency issues rather than breaking the build like so:
```gradle
analyzeClassesDependencies {
  justWarn = true
}
```

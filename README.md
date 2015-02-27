# gradle-dependency-anayze
Dependency analysis plugin for gradle

This was originally forked from https://gist.github.com/kellyrob99/4334483

This plugin attempts to replicate the functionality of the maven dependency plugin's analyze goals which fail the build if dependencies are declared but not used or used but not declared.

I haven't gotten around to publishing the plugin anywhere, but if you build it locally, it can be installed to your local maven repository with:
```
./gradlew publishToMavenLocal
```

You can then use the plugin in your project:
```gradle
buildscript {
  repositories {
    mavenLocal()
  }
  dependencies {
    classpath 'ca.cutterslade.gradle:gradle-dependency-analyze:1.0.0'
  }
  apply plugin: 'ca.cutterslade.analyze'
}
```
# Tasks
This plugin will add three tasks to your project: `analyzeClassesDependencies`, `analyzeClassesDependencies`, and `analyzeDependencies`.
## analyzeClassesDependencies
This task depends on the `classes` task and analyzes the dependencies of the main source set's output directory. This ensures that all dependencies declared in the `compile` configuration are used by classes, and that all dependencies of the classes are declared in the `compile` or `provided` configurations (see [Nebula Extra Configurations](https://github.com/nebula-plugins/gradle-extra-configurations-plugin)).
## analyzeTestClassesDependencies
This task depends on the `testClasses` task and analyzes the dependencies of the test source set's output directory. This ensures that all dependencies declared in the `testCompile` configuration are used by classes, and that all dependencies of the classes are declared in the `testCompile`, `compile`, or `provided` configurations.
## analyzeDependencies
This task depends on the `analyzeClassesDependencies` and `analyzeTestClassesDependencies` tasks, and does nothing on its own. A dependency on this task is added to the `check` task.

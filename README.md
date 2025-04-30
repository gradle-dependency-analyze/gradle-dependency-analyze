# gradle-dependency-analyze

![GitHub release (latest by date)](https://img.shields.io/github/v/release/gradle-dependency-analyze/gradle-dependency-analyze)
![Jacoco Result](/.github/badges/jacoco.svg?raw=true&sanitize=true)
![Branch coverage](/.github/badges/branches.svg?raw=true&sanitize=true)
![Java CI](https://github.com/gradle-dependency-analyze/gradle-dependency-analyze/workflows/Java%20CI/badge.svg?branch=master)
![GitHub Super-Linter](https://github.com/gradle-dependency-analyze/gradle-dependency-analyze/workflows/Lint%20Code%20Base/badge.svg)

Dependency analysis plugin for gradle.

This plugin is based on the work in a gist at <https://gist.github.com/kellyrob99/4334483>. This plugin is possible
thanks to this work by [Kelly Robinson](https://github.com/kellyrob99); thank you.

This plugin attempts to replicate the functionality of the maven dependency plugin's analyze goals which fail the build
if dependencies are declared but not used or used but not declared.

The plugin is available from the gradle plugin repository, so it can be added to your build with the following:

Using the plugin DSL:

```gradle
plugins {
  id "ca.cutterslade.analyze" version "2.0.0"
}
```

Using legacy plugin application:

```gradle
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "ca.cutterslade.gradle:gradle-dependency-analyze:2.0.0"
  }
}

apply plugin: 'java'
apply plugin: 'ca.cutterslade.analyze'
```

When applying this plugin to a multi-project build, it should be applied the root project as well as all sub-projects
for which dependency analysis is needed. A common pattern is to apply this plugin to all projects and the java plugin to
only the sub-projects:

```gradle
allprojects {
  apply plugin: 'ca.cutterslade.analyze'
}
subprojects {
  apply plugin: 'java'
}
```

## Compatibility

The plugin is build with JDK 1.8 and is tested against Gradle 5.3 up to Gradle 8.13.

| Plugin version | Gradle version |
| -------------- | -------------- |
| \<= 1.4.0      | \< 5.0         |
| 1.4.1 - 1.10.0 | \>= 5.0        |
| \>= 2.0.0      | \>=5.3         |

## Sample Output

If the dependency analysis finds issues, it will normally cause the build to fail, and print a list of the issues that
were found, similar to the following:

```text
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

## Restrictions

This plugin can not properly detect the following use cases and will issue a warning about a problematic dependency
declaration.

1. When a constant is used inside the code that is located in a dependency, and it is the only usage of anything from
   that dependency the plugin _might_ report a problem about an unused dependency
2. When a constant is used as a value in an annotation is located in a dependency, and it is the only usage of anything
   from that dependency the plugin is not able to detect that usage as in that case all references to the constant are
   erased by the java compiler
3. When a class hierarchy is in place across multiple gradle subprojects or different dependencies the plugin is not
   able to detect this kind of hierarchies as the byte code only contains references to "direct" classes that are used
4. When an exception is part of a method signature and this exception is in a dependency and this is the only used class
   from that dependency the class invoking this method and not using this exception but throwing a more general one the
   exception usage will not be detected as it is not part of the byte code
5. Annotations with `@Retention(SOURCE)` are not part of the byte code, so these usages will not be detected the
   plugin (e.g. `@Generated` from `jakarta-annotation-api` or `javax.annotation-api`)

In these situations a `permit*UnusedDeclared` must be added to not trigger a build failure or warning by this plugin

## Tasks

This plugin will add the following tasks to your project: `analyzeClassesDependencies`, `analyzeTestClassesDependencies`
, and `analyzeDependencies`.

### analyzeClassesDependencies

This task depends on the `classes` task and analyzes the dependencies of the main source set's output directory. This
ensures that all dependencies of the classes are declared in the `compile`, `api`, `implementation`, or `compileOnly`
configuration. It also ensures the inverse, that all the dependencies of these configurations are used by classes;
use of the `permitUnusedDeclared` configuration allows for exceptions to this restriction.

### analyzeTestClassesDependencies

This task depends on the `testClasses` task and analyzes the dependencies of the test source set's output directory.
This ensures that all dependencies of the classes are declared in the `testCompile`, `testApi`, `testImplementation`
or `testCompileOnly` configuration. It also ensures the inverse, that all the dependencies of these configurations
are used by classes; use of the `permitTestUnusedDeclared` configuration allows for exceptions to this restriction.

### analyzeDependencies

This task depends on the `analyzeClassesDependencies` and `analyzeTestClassesDependencies` tasks, and does nothing on
its own. A dependency on this task is added to the `check` task.

Additionally, the plugin will add analyze action for every custom sourceSet defined:

### analyze*SourceSet*ClassesDependencies

This task depends on the `*sourceSet*Classes` task and analyzes the dependencies of the _sourceSet_'s output directory.
This ensures that all dependencies of the classes are declared in the `*sourceSet*Compile`, `*sourceSet*api`
, `*sourceSet*implementation`, or `*sourceSet*CompileOnly` configuration. It also ensures the inverse, that all the
dependencies of these configurations are used by classes; use of the `permit*SourceSet*UnusedDeclared` configuration
allows for exceptions to this restriction.

## Configurations

This plugin adds the following configurations which may be used to define dependencies which should be handled in a
special way. These configurations have no impact on the build outside this plugin.

- `permitUnusedDeclared`
- `permitTestUnusedDeclared`
- `permitUsedUndeclared`
- `permitTestUsedUndeclared`
- `permit*SourceSet*UnusedDeclared`
- `permit*SourceSet*UsedUndeclared`

Adding dependencies to `permitUnusedDeclared` causes the dependency analyzer to ignore cases where the dependencies are
declared but not used. Adding dependencies to `permitUsedUndeclared` causes the dependency analyzer to ignore cases
where the dependencies used but not declared.

### Examples

Using these configurations to allow exceptions to the rules is as simple as adding a dependency to your project. The
snippet below will provide a compile-time dependency on the JSP API, but the plugin will not complain if it is not used.

```gradle
dependencies {
  compile 'javax.servlet:jsp-api:2.0'
  permitUnusedDeclared 'javax.servlet:jsp-api:2.0'
}
```

## Task Configuration

The plugin is not especially configurable, but each task can be configured to log a warning about dependency issues
rather than breaking the build. Each task can also be configured to log informational output into a file instead of the
Gradle console, this can be helpful for large projects where printing all the dependencies can cause high memory usage.
Informational messages are logged to `$builddir/reports/dependency-analyze/`.

```gradle
tasks.named('analyzeClassesDependencies').configure {
  warnUsedUndeclared = true
  warnUnusedDeclared = true
  warnSuperfluous = true
  logDependencyInformationToFiles = true
}

tasks.named('analyzeTestClassesDependencies').configure {
  warnUsedUndeclared = true
  warnUnusedDeclared = true
  warnSuperfluous = true
  logDependencyInformationToFiles = true
}
```

- `warnUsedUndeclared` - only warn if used undeclared dependencies are found, default: `false`
- `warnUnusedDeclared` - only warn if unused declared dependencies are found, default: `false`
- `warnSuperfluous` - only warn if superfluous declared dependencies are found, default: `false`
- `warnCompileOnly` - warn for compileOnly dependencies, default: `false`
- `logDependencyInformationToFiles` - logs dependency violations to log file, default: `false`

### Disabling/enabling the plugin

In addition to using the `warn`-property, many cases want the build to fail only under given conditions (i.e. nightly
builds or integration builds). This can be achieved by disabling and enabling dependency analyzing in the following
manner.

```gradle
if (!project.hasProperty('analyzeDependencies')) {
  tasks.named('analyzeClassesDependencies').configure {
    enabled = false
  }
  tasks.named('analyzeTestClassesDependencies').configure {
    enabled = false
  }
  tasks.named('analyzeDependencies').configure {
    enabled = false
  }
}
```

### compileOnly dependencies

Starting with version 1.9.0 `compileOnly` dependencies will be excluded from the `unusedDeclaredDependencies`.
Additionally, a new flag named `warnCompileOnly` has been introduced to list all `compileOnly` dependencies that
are declared and not needed in runtime.
If a `compileOnly` dependency is declared and needed based on the analysis in runtime it will fail the build, in that
case a `permitUsedUndeclared` must be added if this is not correct (mainly when developing libraries which are
performing safety checks to not access a class that is not in the classpath, e.g. `spring-boot-starter-*` projects).

To enable the logging for `compileOnly` dependencies the following needs to be added to the plugin configuration
block in `build.gradle`.

```gradle
tasks.named('analyzeClassesDependencies').configure {
    warnCompileOnly = true
}
```

### Aggregator projects

With version 1.6.0 a new feature has been added that allows the use of aggregator projects without the need to add
many `permit*` dependencies. This makes the life easier when for example a project heavily uses `spring-boot-starters`.
Normally you do not want to add all dependencies manually to one gradle project instead you want to dependent on the
starter and _trust_ the dependencies declared in that place. As this might be against the intention of this plugin we
still think it might be a good addition. As a benefit to still have a clean and small classpath the plugin tries to
optimize the aggregator usage by picking the one with the smallest overhead (less transitive dependencies).

_Example how to use:_

```gradle
dependencies {
  compile('org.springframework.boot:spring-boot-starter:2.3.6.RELEASE')

  permitAggregatorUse('org.springframework.boot:spring-boot-starter:2.3.6.RELEASE')
}
```

With that configuration the plugin will not "complain" about unused declared dependencies for `spring-boot-starter` and
also not about used undeclared dependencies for example when the code uses a class from `spring-core` which is a
dependency of the starter.

When a `permitAggregatorUse` and additionally a dependency from the aggregator is specified the dependency is marked as
a `superfluousDeclaredArtifacts`. This one does not need to be added as it is already allowed by the
`permitAggregatorUse`. If you still want to have these extra dependencies listed and do not want to fail the build, set
the option `warnSuperfluous = true` for the task.

_Example for the optimization when a smaller aggregator is a better fit:_

```gradle
dependencies {
  compile('org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE')

  permitAggregatorUse('org.springframework.boot:spring-boot-starter:2.3.6.RELEASE')
  permitAggregatorUse('org.springframework.boot:spring-boot-starter-web:2.3.6.RELEASE')
}
```

When the code in that gradle project now only use classes from `spring-core` and two `permitAggregatorUse` dependencies
have been declared the plugin will inform about the change that should be done to use `spring-boot-starter` instead
of `spring-boot-starter-web`. This optimization will only work when **multiple** `permitAggregatorUse` dependencies are
declared for one gradle project.

_Example how to use with platform plugin:_

As this feature makes the most sense when used together with the `platform-plugin` the following example shows haw this
can be achieved:

```gradle
plugins {
  id 'groovy'
  id 'ca.cutterslade.analyze'
  id 'java-library'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
  api('org.springframework:spring-web')
  implementation('org.springframework.boot:spring-boot-starter-web')

  permitAggregatorUse('org.springframework.boot:spring-boot-starter-web')
}

configurations {
    myPlatform {
        canBeResolved = false
        canBeConsumed = false
    }
}

configurations.all {
    if (canBeResolved) {
        extendsFrom(configurations.myPlatform)
    }
}
dependencies {
    myPlatform platform(project(':platform'))
}
```

### Custom task instances

Applying the plugin creates and configures two instances of the `AnalyzeDependenciesPlugin` task. These two
instances, `analyzeClassesDependencies` and `analyzeTestClassesDependencies`, are configured to verify the main and test
source set dependencies respectively. Additional instances of this task type can be created and configured in addition
to, or instead of, the instances created by the plugin. This may be appropriate when setting up more complex project
configurations, or using other plugins which introduce their own configurations.

Example:

```gradle
tasks.register('analyzeCustomClassesDependencies', AnalyzeDependenciesTask) {
  dependsOn customClasses

  // List of configurations which the analyzed output is required to use
  require = [ configurations.customCompile, configuration.customCompileOnly ]

  // List of configurations which the analyzed output may use but is not required to
  allowedToUse = [ configurations.compile, configurations.provided ]

  // List of configurations which the analyzed output is not required to use, even if dependencies are present in the
  // 'require' list above
  allowedToDeclare = [ configurations.permitCustomUnusedDeclared ]

  // Location of class output directories to analyze
  classesDirs = sourceSets.custom.output.classesDirs
}

// Add the new task as a dependency of the main analyzeDependencies task
tasks.named('analyzeDependencies').configure {
  dependsOn analyzeCustomClassesDependencies
}
```

Users of the `java-library` plugin no longer need to configure custom tasks, and should upgrade to version 1.4.0 as soon
as practical.

## Changelog

### Version 2.0.0

The `justWarn` flag is now removed, use the dedicated options `warnUsedUndeclared`, `warnUnusedDeclared`,
`warnSuperfluous` and `warnCompileOnly`.

A new option `warnSuperfluous` has been introduced to just warn instead of failing in case when a dependency is declared
additionally to an aggregator dependency (which includes also this dependency).

Gradle version >= 5.3 is now required to start supporting the configuration cache, also warnings related to unsafe
access to the project() have been removed.

Converted the code to plain Java to avoid various issues related to type mismatches in the implementation and between
gradle versions.

### Version 1.10.0

Switch from Plexus DirectoryScanner to our own implementation for finding class files

### Version 1.9.0

Version 1.9.0 adds support for excluding and warning about the usage of `compileOnly` dependencies. see
[compileOnly](#compileonly-dependencies)

### Version 1.8.0

Version 1.8.0 adds support for different handling when a dependency issue is found. This means if the plugin finds
an `unusedDeclared` dependency can can now fail while at the same time when it finds an `usedUndeclared` one it will
only print a warning. So the `justWarn` is now deprecated in favor of the more specific configuration options.

### Version 1.7.0

Version 1.7.0 adds support for writing all logging information (used artifacts/classes/dependencies) to a folder located
in `build/reports/dependency-analyze`.

### Version 1.6.0

Version 1.6.0 of this plugin adds support for aggregator projects. This feature is an experimental feature that needs to
be tested by more users to see if it works as expected. see [aggregator usage](#aggregator-projects)

### Version 1.5.0

Version 1.5.0 of this plugin adds built-in support for the `java-test-fixtures` plugin. Additionally, the plugin was
extended to automatically detect custom source sets and provides dedicated tasks for each of them.

### Version 1.4.0

Version 1.4.0 of this plugin adds built-in support for the `java-library` plugin, which has been the recommended default
for quite a while. Previously tasks had to be customised to analyze the correct configurations.

### Version 1.3.0

Version 1.3.0 of this plugin introduces only minor functional changes, but adds support for Java version 9, 10, and 11,
while dropping support for Java versions 6 and 7.

The dependency analyzer has been upgraded to version `1.10`, this new version adds detection of inlined dependencies,
which can cause some false positives (the lack of this detection used to cause false negatives). In order to assist in
working around these false positives, two new configurations have been added to the plugin:

- `permitUsedUndeclared`
- `permitTestUsedUndeclared`

These configurations are described above.

### Version 1.2.0

Version 1.2.0 of this plugin introduces a couple significant changes.

- For multi project builds, the plugin must now be applied to the root project. If it has not been applied to the root
  project, the build will fail with the message `Dependency analysis plugin must also be applied to the root project`.
- The plugin will no longer fail to apply if the java plugin has not been applied. Applying this plugin to a project
  without the java plugin will have no effect.
- The plugin no longer caches dependency information in a static cache which would persist across executions when the
  gradle daemon was in use. It now caches dependency information in the root project. This represents a small
  performance penalty but avoids a potential issue if a dependency file is modified, and a potential memory leak if the
  path of dependency files changes regularly.
- The tasks now produce output files at `$buildDir/dependency-analyse/$taskName`. This contains the exception message if
  the task causes the build to fail, or is empty if the task does not cause the build to fail.
- The tasks now specify inputs and outputs allowing gradle to consider a task up-to-date if nothing has changed.
- The tasks allows caching of outputs on gradle versions which support the task output cache. This allows the task work
  to be skipped even on clean builds if an appropriate cached result exists.
- Tasks will now appear in the listing produced by `gradle tasks` under the Verification group.

#### Migration from 1.1.0

Migrating from version 1.1.0 to version 1.2.0 of the plugin should be very simple. Most users will not have to make any
changes, users with multi-project builds will have to ensure that the plugin is applied to the root project. This can be
accomplished by applying the plugin in the `allprojects {}` block.

### Version 1.1.0

Version 1.1.0 of this plugin introduced a couple significant changes.

- The plugin now supports the `compileOnly` and `testCompileOnly` configurations introduced by gradle in version 2.12.
  This feature was discussed in detail in
  a [posting on the gradle blog](https://gradle.org/blog/compile-only-dependencies/). These configurations should
  generally be used where [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin) would have
  been used.
- The `permitUnusedDeclared` and `permitTestUnusedDeclared` configurations were introduced to allow for specific
  exceptions to the restriction which requires all declared dependencies to be used.
- If the project makes use of the [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin)
  configuration, these dependencies are now treated the same as the `compile` configuration; specifically, dependencies
  of the `provided` configuration must be used by compiled class files. Previously, the `provided` configuration was an
  exception to that rule, allowing for the type of exception now supported by the `permitUnusedDeclared` configuration.

#### Migration from 1.0.0

If you previously made use of the [`provided`](https://github.com/nebula-plugins/gradle-extra-configurations-plugin)
configuration, upgrading to version 1.1 of this plugin may cause dependency analysis failures, since the `provided`
configuration is now treated in the same way as the `compile` configuration. After investigating these failures to
ensure that they do not represent a misconfiguration of the project dependencies, the offending dependencies can be
added to the `permitUnusedDeclared` configuration to suppress the failure.

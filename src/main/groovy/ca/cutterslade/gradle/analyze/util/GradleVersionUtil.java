package ca.cutterslade.gradle.analyze.util;

import static java.lang.System.lineSeparator;

import org.gradle.api.logging.Logger;
import org.gradle.util.GradleVersion;

public final class GradleVersionUtil {
  private static final GradleVersion VERSION_7_0 = GradleVersion.version("7.0");
  private static final GradleVersion VERSION_7_3 = GradleVersion.version("7.3");

  private GradleVersionUtil() {}

  public static boolean isWarPluginBrokenWhenUsingProvidedRuntime(
      final GradleVersion gradleVersion) {
    return gradleVersion.compareTo(VERSION_7_0) >= 0 && gradleVersion.compareTo(VERSION_7_3) < 0;
  }

  public static void warnAboutWarPluginBrokenWhenUsingProvidedRuntime(
      final GradleVersion gradleVersion, final Logger logger) {
    if (logger != null && isWarPluginBrokenWhenUsingProvidedRuntime(gradleVersion)) {
      logger.warn(
          "The used Gradle version has a known bug when using the war plugin with a 'providedRuntime' "
              + lineSeparator()
              + "dependency where the gradle-dependency-analyze plugin will show a unusedDeclaredDependency warning. "
              + lineSeparator()
              + "Details see https://github.com/gradle/gradle/issues/17415 "
              + lineSeparator()
              + "Gradle version containing the bug: >=7.0 and < 7.3 "
              + lineSeparator()
              + lineSeparator()
              + "Consider upgrading Gradle to a non affected version."
              + lineSeparator());
    }
  }
}

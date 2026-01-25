package ca.cutterslade.gradle.analyze.util;

import ca.cutterslade.gradle.analyze.DependencyAnalysisException;
import ca.cutterslade.gradle.analyze.ProjectDependencyAnalysisResult;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.logging.Logger;

public final class ProjectDependencyAnalysisResultHandler {
  private ProjectDependencyAnalysisResultHandler() {}

  public static void warnAndLogOrFail(
      final ProjectDependencyAnalysisResult result,
      final boolean warnUsedUndeclared,
      final boolean ignoreUsedUndeclared,
      final boolean warnUnusedDeclared,
      final boolean warnCompileOnly,
      final boolean warnSuperfluous,
      final Path logFilePath,
      boolean logDependencyInformationToFiles,
      final Logger logger)
      throws IOException {
    final Set<ComponentIdentifier> usedUndeclared = result.getUsedUndeclaredArtifacts();
    final Set<ComponentIdentifier> unusedDeclared = result.getUnusedDeclaredArtifacts();
    final Set<ComponentIdentifier> superfluousDeclaredArtifacts =
        result.getSuperfluousDeclaredArtifacts();
    final Set<ComponentIdentifier> possiblyUnusedCompileOnly =
        result.getPossiblyUnusedCompileOnlyArtifacts();

    final String compileOnlyViolations =
        warnCompileOnly
            ? getSummary("compileOnlyDeclaredArtifacts", possiblyUnusedCompileOnly)
            : "";
    if (!warnCompileOnly) {
      usedUndeclared.removeAll(possiblyUnusedCompileOnly);
    }
    // If ignoreUsedUndeclared is true, we completely skip usedUndeclared violations
    final String usedUndeclaredViolations =
        ignoreUsedUndeclared ? "" : getSummary("usedUndeclaredArtifacts", usedUndeclared);
    final String unusedDeclaredViolations = getSummary("unusedDeclaredArtifacts", unusedDeclared);
    final String superfluousDeclaredViolations =
        getSummary("superfluousDeclaredArtifacts", superfluousDeclaredArtifacts);

    final String combinedViolations =
        join(
            join(usedUndeclaredViolations, unusedDeclaredViolations),
            superfluousDeclaredViolations);

    if (!combinedViolations.isEmpty()) {
      if (logDependencyInformationToFiles) {
        logToFile(logFilePath, combinedViolations, compileOnlyViolations);
      }

      if (!warnUsedUndeclared && !warnUnusedDeclared && !ignoreUsedUndeclared) {
        throw new DependencyAnalysisException(
            foundIssues(join(combinedViolations, compileOnlyViolations)));
      }

      if (!usedUndeclaredViolations.isEmpty()) {
        if (ignoreUsedUndeclared) {
          // Do nothing - completely ignore these violations
        } else if (warnUsedUndeclared) {
          logger.warn(foundIssues(usedUndeclaredViolations));
        } else {
          throw new DependencyAnalysisException(foundIssues(usedUndeclaredViolations));
        }
      }

      if (!unusedDeclaredViolations.isEmpty()) {
        if (warnUnusedDeclared) {
          logger.warn(foundIssues(unusedDeclaredViolations));
        } else {
          throw new DependencyAnalysisException(foundIssues(unusedDeclaredViolations));
        }
      }

      if (!superfluousDeclaredArtifacts.isEmpty()) {
        if (warnSuperfluous) {
          logger.warn(foundIssues(superfluousDeclaredViolations));
        } else {
          throw new DependencyAnalysisException(foundIssues(superfluousDeclaredViolations));
        }
      }
    }

    if (!compileOnlyViolations.isEmpty()) {
      logger.warn(foundIssues(compileOnlyViolations));
    }
  }

  private static void logToFile(
      final Path logFilePath, final String combinedViolations, final String compileOnlyViolations)
      throws IOException {
    try (final Writer w =
        Files.newBufferedWriter(
            logFilePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
      w.append(System.lineSeparator())
          .append("Final result")
          .append(System.lineSeparator())
          .append("============")
          .append(System.lineSeparator())
          .append(System.lineSeparator())
          .append(combinedViolations)
          .append(System.lineSeparator());
      if (!compileOnlyViolations.isEmpty()) {
        w.append(compileOnlyViolations).append(System.lineSeparator());
      }
    }
  }

  private static String join(String str1, String str2) {
    return String.join(
        (!str1.isEmpty() && !str2.isEmpty()) ? System.lineSeparator() : "", str1, str2);
  }

  private static String getSummary(
      final String sectionName, final Set<ComponentIdentifier> identifiers) {
    if (!identifiers.isEmpty()) {
      return identifiers.stream()
          .sorted(Comparator.comparing(ComponentIdentifier::getDisplayName))
          .map(identifier -> " - " + identifier.getDisplayName())
          .collect(
              Collectors.joining(System.lineSeparator(), sectionName + System.lineSeparator(), ""));
    } else {
      return "";
    }
  }

  private static String foundIssues(final String issues) {
    return "Dependency analysis found issues." + System.lineSeparator() + issues;
  }
}

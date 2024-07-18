package ca.cutterslade.gradle.analyze.util;

import ca.cutterslade.gradle.analyze.DependencyAnalysisException;
import ca.cutterslade.gradle.analyze.ProjectDependencyAnalysisResult;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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
      final boolean warnUnusedDeclared,
      final boolean warnCompileOnly,
      final Path logFilePath,
      final Logger logger)
      throws IOException {
    final Set<ComponentIdentifier> usedUndeclaredArtifacts = result.getUsedUndeclaredArtifacts();
    final Set<ComponentIdentifier> unusedDeclaredArtifacts = result.getUnusedDeclaredArtifacts();
    final Set<ComponentIdentifier> possiblyUnusedCompileOnlyArtifacts =
        result.getPossiblyUnusedCompileOnlyArtifacts();

    final String compileOnlyViolations =
        warnCompileOnly
            ? getArtifactSummary("compileOnlyDeclaredArtifacts", possiblyUnusedCompileOnlyArtifacts)
            : "";
    if (!warnCompileOnly) {
      usedUndeclaredArtifacts.removeAll(possiblyUnusedCompileOnlyArtifacts);
    }
    final String usedUndeclaredViolations =
        getArtifactSummary("usedUndeclaredArtifacts", usedUndeclaredArtifacts);
    final String unusedDeclaredViolations =
        getArtifactSummary("unusedDeclaredArtifacts", unusedDeclaredArtifacts);

    final String combinedViolations = usedUndeclaredViolations.concat(unusedDeclaredViolations);

    if (!combinedViolations.isEmpty()) {
      if (logFilePath != null) {
        Files.createDirectories(logFilePath.getParent());
        try (final Writer w = Files.newBufferedWriter(logFilePath)) {
          w.append(combinedViolations.concat(compileOnlyViolations));
        }
      }

      if (!warnUsedUndeclared && !warnUnusedDeclared) {
        throw new DependencyAnalysisException(
            foundIssues(combinedViolations.concat(compileOnlyViolations)));
      }

      if (!usedUndeclaredViolations.isEmpty()) {
        if (warnUsedUndeclared) {
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
    }

    if (!compileOnlyViolations.isEmpty()) {
      logger.warn(foundIssues(compileOnlyViolations));
    }
  }

  private static String getArtifactSummary(
      final String sectionName, final Set<ComponentIdentifier> resolvedArtifacts) {
    if (!resolvedArtifacts.isEmpty()) {
      return resolvedArtifacts.stream()
              .sorted(Comparator.comparing(ComponentIdentifier::getDisplayName))
              .map(resolvedArtifact -> " - " + resolvedArtifact.getDisplayName())
              .collect(
                  Collectors.joining(
                      System.lineSeparator(), sectionName + System.lineSeparator(), ""))
          + System.lineSeparator();
    } else {
      return "";
    }
  }

  private static String foundIssues(final String issues) {
    return "Dependency analysis found issues." + System.lineSeparator() + issues;
  }
}

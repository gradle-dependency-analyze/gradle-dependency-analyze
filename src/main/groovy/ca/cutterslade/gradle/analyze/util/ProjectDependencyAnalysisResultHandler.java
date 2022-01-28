package ca.cutterslade.gradle.analyze.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.logging.Logger;

import ca.cutterslade.gradle.analyze.DependencyAnalysisException;
import ca.cutterslade.gradle.analyze.ProjectDependencyAnalysisResult;

public final class ProjectDependencyAnalysisResultHandler {
    private ProjectDependencyAnalysisResultHandler() {
    }

    public static void warnAndLogOrFail(final ProjectDependencyAnalysisResult result,
                                        final boolean warnUsedUndeclared,
                                        final boolean warnUnusedDeclared,
                                        final Path logFilePath,
                                        final Logger logger) throws IOException {
        final String usedUndeclaredViolations = getArtifactSummary("usedUndeclaredArtifacts", result.getUsedUndeclaredArtifacts());
        final String unusedDeclaredViolations = getArtifactSummary("unusedDeclaredArtifacts", result.getUnusedDeclaredArtifacts());

        final String combinedViolations = usedUndeclaredViolations.concat(unusedDeclaredViolations);

        if (!combinedViolations.isEmpty()) {
            if (logFilePath != null) {
                Files.createDirectories(logFilePath.getParent());
                Files.newBufferedWriter(logFilePath).append(combinedViolations).close();
            }

            if (!warnUsedUndeclared && !warnUnusedDeclared) {
                throw new DependencyAnalysisException(foundIssues(combinedViolations));
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
    }

    private static String getArtifactSummary(final String sectionName,
                                             final Set<ResolvedArtifact> resolvedArtifacts) {
        if (!resolvedArtifacts.isEmpty()) {
            return resolvedArtifacts.stream()
                    .sorted(Comparator.comparing(resolvedArtifact -> resolvedArtifact.getModuleVersion().getId().toString()))
                    .map(resolvedArtifact -> " - "
                            + resolvedArtifact.getModuleVersion().getId()
                            + (resolvedArtifact.getClassifier() != null ? ":" + resolvedArtifact.getClassifier() : "") + "@"
                            + resolvedArtifact.getExtension())
                    .collect(Collectors.joining("\n", sectionName + "\n", "")) + "\n";
        } else {
            return "";
        }
    }

    private static String foundIssues(final String issues) {
        return "Dependency analysis found issues.\n" + issues;
    }
}

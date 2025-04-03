package ca.cutterslade.gradle.analyze.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;

public class PomDependencyDetector {

  private final Logger logger;

  public PomDependencyDetector(final Logger logger) {
    this.logger = logger;
  }

  /**
   * Detects POM-only dependencies in the provided configurations.
   *
   * @param configurations List of configuration providers to analyze
   * @return Set of identifiers for POM-only dependencies
   */
  public Set<ModuleComponentIdentifier> processDependencies(
      final List<Provider<Configuration>> configurations) {
    final Set<ModuleComponentIdentifier> pomOnlyDependencies = new HashSet<>();

    for (final Provider<Configuration> configProvider : configurations) {
      final Configuration configuration = configProvider.get();

      // Skip if configuration cannot be resolved
      if (!configuration.isCanBeResolved()) {
        logger.debug("Configuration {} cannot be resolved. Skipping.", configuration.getName());
        continue;
      }

      // Get all components from the resolution result
      final Set<ResolvedComponentResult> components =
          configuration.getIncoming().getResolutionResult().getAllComponents();

      logger.debug("Found {} components in {}", components.size(), configuration.getName());

      // Get all direct file dependencies to compare against
      final Set<ComponentIdentifier> componentsWithFiles =
          configuration.getResolvedConfiguration().getResolvedArtifacts().stream()
              .map(artifact -> artifact.getId().getComponentIdentifier())
              .collect(Collectors.toSet());

      // Find dependencies that are referenced but don't have direct artifacts
      for (final ResolvedComponentResult component : components) {
        if (component.getId() instanceof ModuleComponentIdentifier) {
          final ModuleComponentIdentifier id = (ModuleComponentIdentifier) component.getId();

          // Skip if the component has files
          if (componentsWithFiles.contains(id)) {
            continue;
          }

          // Check if it has dependencies itself (indicator of a POM-only dependency)
          final boolean hasDependencies =
              component.getDependencies().stream()
                  .filter(dep -> dep instanceof ResolvedDependencyResult)
                  .map(dep -> (ResolvedDependencyResult) dep)
                  .anyMatch(dep -> true); // True if any dependencies exist

          if (hasDependencies) {
            pomOnlyDependencies.add(id);
            logger.debug("Found POM-only dependency: {}", id.getDisplayName());
          }
        }
      }
    }

    return pomOnlyDependencies;
  }
}

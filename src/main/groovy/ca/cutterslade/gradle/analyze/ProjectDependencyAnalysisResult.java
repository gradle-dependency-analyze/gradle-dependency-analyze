package ca.cutterslade.gradle.analyze;

import java.util.Set;
import org.gradle.api.artifacts.component.ComponentIdentifier;

public class ProjectDependencyAnalysisResult {
  private final Set<ComponentIdentifier> usedDeclaredArtifacts;
  private final Set<ComponentIdentifier> usedUndeclaredArtifacts;
  private final Set<ComponentIdentifier> unusedDeclaredArtifacts;
  private final Set<ComponentIdentifier> possiblyUnusedCompileOnlyArtifacts;

  public ProjectDependencyAnalysisResult(
      final Set<ComponentIdentifier> usedDeclaredArtifacts,
      final Set<ComponentIdentifier> usedUndeclaredArtifacts,
      final Set<ComponentIdentifier> unusedDeclaredArtifacts,
      final Set<ComponentIdentifier> possiblyUnusedCompileOnlyArtifacts) {
    this.usedDeclaredArtifacts = usedDeclaredArtifacts;
    this.usedUndeclaredArtifacts = usedUndeclaredArtifacts;
    this.unusedDeclaredArtifacts = unusedDeclaredArtifacts;
    this.possiblyUnusedCompileOnlyArtifacts = possiblyUnusedCompileOnlyArtifacts;
  }

  public Set<ComponentIdentifier> getUsedDeclaredArtifacts() {
    return usedDeclaredArtifacts;
  }

  public Set<ComponentIdentifier> getUsedUndeclaredArtifacts() {
    return usedUndeclaredArtifacts;
  }

  public Set<ComponentIdentifier> getUnusedDeclaredArtifacts() {
    return unusedDeclaredArtifacts;
  }

  public Set<ComponentIdentifier> getPossiblyUnusedCompileOnlyArtifacts() {
    return possiblyUnusedCompileOnlyArtifacts;
  }
}

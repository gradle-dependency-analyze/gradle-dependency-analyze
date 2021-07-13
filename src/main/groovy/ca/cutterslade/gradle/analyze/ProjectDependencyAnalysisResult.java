package ca.cutterslade.gradle.analyze;

import java.util.Set;

import org.gradle.api.artifacts.ResolvedArtifact;

public class ProjectDependencyAnalysisResult {
    private final Set<ResolvedArtifact> usedDeclaredArtifacts;
    private final Set<ResolvedArtifact> usedUndeclaredArtifacts;
    private final Set<ResolvedArtifact> unusedDeclaredArtifacts;

    public ProjectDependencyAnalysisResult(final Set<ResolvedArtifact> usedDeclaredArtifacts,
                                           final Set<ResolvedArtifact> usedUndeclaredArtifacts,
                                           final Set<ResolvedArtifact> unusedDeclaredArtifacts) {
        this.usedDeclaredArtifacts = usedDeclaredArtifacts;
        this.usedUndeclaredArtifacts = usedUndeclaredArtifacts;
        this.unusedDeclaredArtifacts = unusedDeclaredArtifacts;
    }

    public Set<ResolvedArtifact> getUsedDeclaredArtifacts() {
        return usedDeclaredArtifacts;
    }

    public Set<ResolvedArtifact> getUsedUndeclaredArtifacts() {
        return usedUndeclaredArtifacts;
    }

    public Set<ResolvedArtifact> getUnusedDeclaredArtifacts() {
        return unusedDeclaredArtifacts;
    }
}

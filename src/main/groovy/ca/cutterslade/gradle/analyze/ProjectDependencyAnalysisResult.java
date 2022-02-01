package ca.cutterslade.gradle.analyze;

import java.util.Set;

import org.gradle.api.artifacts.ResolvedArtifact;

public class ProjectDependencyAnalysisResult {
    private final Set<ResolvedArtifact> usedDeclaredArtifacts;
    private final Set<ResolvedArtifact> usedUndeclaredArtifacts;
    private final Set<ResolvedArtifact> unusedDeclaredArtifacts;
    private final Set<ResolvedArtifact> possiblyUnusedCompileOnlyArtifacts;

    public ProjectDependencyAnalysisResult(final Set<ResolvedArtifact> usedDeclaredArtifacts,
                                           final Set<ResolvedArtifact> usedUndeclaredArtifacts,
                                           final Set<ResolvedArtifact> unusedDeclaredArtifacts,
                                           final Set<ResolvedArtifact> possiblyUnusedCompileOnlyArtifacts) {
        this.usedDeclaredArtifacts = usedDeclaredArtifacts;
        this.usedUndeclaredArtifacts = usedUndeclaredArtifacts;
        this.unusedDeclaredArtifacts = unusedDeclaredArtifacts;
        this.possiblyUnusedCompileOnlyArtifacts = possiblyUnusedCompileOnlyArtifacts;
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

    public Set<ResolvedArtifact> getPossiblyUnusedCompileOnlyArtifacts() {
        return possiblyUnusedCompileOnlyArtifacts;
    }
}

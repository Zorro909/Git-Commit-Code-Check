package de.zorro909.codecheck.core.watch;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DependencyInvalidationGraph {

    private final ConcurrentHashMap<Path, Set<Path>> dependentsByContext = new ConcurrentHashMap<>();

    public void recordDependency(Path contextFile, Path dependentValidatedFile) {
        dependentsByContext.computeIfAbsent(normalize(contextFile), _ -> ConcurrentHashMap.newKeySet())
            .add(normalize(dependentValidatedFile));
    }

    public Set<Path> dependents(Path contextFile) {
        return Set.copyOf(dependentsByContext.getOrDefault(normalize(contextFile), Set.of()));
    }

    public Set<Path> invalidate(Path contextFile, IncrementalValidationState state) {
        Set<Path> dependents = dependents(contextFile);
        dependents.forEach(state::markStale);
        return dependents;
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

}

package de.zorro909.codecheck.watcher;

import java.nio.file.Path;
import java.util.Set;

public record WatchScope(Set<WatchedPath> paths) {

    public WatchScope {
        paths = Set.copyOf(paths);
    }

    public boolean contains(Path path, WatchPathKind kind) {
        Path normalized = path.toAbsolutePath().normalize();
        return paths.stream()
                    .anyMatch(watchedPath -> watchedPath.path().equals(normalized)
                                             && watchedPath.kind() == kind);
    }
}

package de.zorro909.codecheck.core.watch;

import java.nio.file.Path;

public record WatchedPath(Path path, WatchPathKind kind, String reason) {

    public WatchedPath {
        path = path.toAbsolutePath().normalize();
    }
}

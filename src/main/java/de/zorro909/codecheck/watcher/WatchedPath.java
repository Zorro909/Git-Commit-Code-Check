package de.zorro909.codecheck.watcher;

import java.nio.file.Path;

public record WatchedPath(Path path,
                          WatchPathKind kind,
                          String reason) {

    public WatchedPath {
        path = path.toAbsolutePath().normalize();
    }
}

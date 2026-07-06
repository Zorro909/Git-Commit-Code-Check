package de.zorro909.codecheck.watcher;

import java.nio.file.Path;

public class DaemonRestartRequiredException extends RuntimeException {

    public DaemonRestartRequiredException(Path configPath) {
        super("Configuration changed; daemon restart required: " + configPath);
    }
}

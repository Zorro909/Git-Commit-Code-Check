package de.zorro909.codecheck.core.watch;

import java.nio.file.Path;
import java.util.Set;

public class ConfigChangeRestartSignal {

    private final Set<Path> configPaths;

    public ConfigChangeRestartSignal(Set<Path> configPaths) {
        this.configPaths = configPaths.stream().map(this::normalize).collect(java.util.stream.Collectors.toSet());
    }

    public void handleChange(Path path) {
        Path normalized = normalize(path);
        if (configPaths.contains(normalized)) {
            throw new DaemonRestartRequiredException(normalized);
        }
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

}

package de.zorro909.codecheck.core.watch;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DebouncedFileUpdateScheduler {

    private final Duration debounce;

    private final Map<Path, Instant> pending = new LinkedHashMap<>();

    public DebouncedFileUpdateScheduler(Duration debounce) {
        this.debounce = debounce;
    }

    public synchronized void recordSave(Path path, Instant savedAt) {
        pending.put(path.toAbsolutePath().normalize(), savedAt);
    }

    public synchronized List<Path> duePaths(Instant now) {
        List<Path> due = pending.entrySet()
            .stream()
            .filter(entry -> !entry.getValue().plus(debounce).isAfter(now))
            .map(Map.Entry::getKey)
            .toList();
        due.forEach(pending::remove);
        return due;
    }

    public synchronized List<Path> pendingPaths() {
        return List.copyOf(pending.keySet());
    }

}

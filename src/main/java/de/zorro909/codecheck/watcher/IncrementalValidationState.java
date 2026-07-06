package de.zorro909.codecheck.watcher;

import de.zorro909.codecheck.validation.Diagnostic;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IncrementalValidationState {

    private final Map<Path, FileValidationStatus> statuses = new ConcurrentHashMap<>();
    private final Map<Path, List<Diagnostic>> diagnostics = new ConcurrentHashMap<>();

    public void markChecking(Path path) {
        statuses.put(normalize(path), FileValidationStatus.CHECKING);
    }

    public void markStale(Path path) {
        statuses.put(normalize(path), FileValidationStatus.STALE);
    }

    public void updateCurrent(Path path, List<Diagnostic> latestDiagnostics) {
        Path normalized = normalize(path);
        diagnostics.put(normalized, List.copyOf(latestDiagnostics));
        statuses.put(normalized, FileValidationStatus.CURRENT);
    }

    public FileValidationStatus status(Path path) {
        return statuses.getOrDefault(normalize(path), FileValidationStatus.STALE);
    }

    public List<Diagnostic> diagnostics(Path path) {
        return diagnostics.getOrDefault(normalize(path), List.of());
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}

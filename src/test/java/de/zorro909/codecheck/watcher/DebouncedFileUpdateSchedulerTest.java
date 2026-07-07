package de.zorro909.codecheck.watcher;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DebouncedFileUpdateSchedulerTest {

    @Test
    void saveIsReleasedOnlyAfterConfiguredDebounce() {
        DebouncedFileUpdateScheduler scheduler = new DebouncedFileUpdateScheduler(Duration.ofSeconds(5));
        Instant savedAt = Instant.parse("2026-07-07T00:00:00Z");
        Path file = Path.of("src/main/java/Example.java");

        scheduler.recordSave(file, savedAt);

        assertThat(scheduler.duePaths(savedAt.plusSeconds(4))).isEmpty();
        assertThat(scheduler.pendingPaths()).hasSize(1);
        assertThat(scheduler.duePaths(savedAt.plusSeconds(5))).containsExactly(file.toAbsolutePath().normalize());
        assertThat(scheduler.pendingPaths()).isEmpty();
    }

}

package de.zorro909.codecheck.daemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DaemonProcessRegistryTest {

    @Test
    void repoIdIsStableHashAndMetadataPathDoesNotExposeRawRepoName(@TempDir Path tempDir)
            throws Exception {
        Path repo = tempDir.resolve("repo with spaces");
        Files.createDirectories(repo);
        DaemonProcessRegistry first = new DaemonProcessRegistry(repo, tempDir.resolve("cache"));
        DaemonProcessRegistry second = new DaemonProcessRegistry(repo, tempDir.resolve("cache"));

        assertThat(first.repoId()).isEqualTo(second.repoId());
        assertThat(first.repoId()).matches("[0-9a-f]{64}");
        assertThat(first.metadataDirectory().getFileName().toString()).isEqualTo(first.repoId());
        assertThat(first.metadataDirectory().toString()).doesNotContain("repo with spaces");
    }

    @Test
    void writesAndReadsAliveMetadata(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        DaemonProcessRegistry registry = new DaemonProcessRegistry(repo, tempDir.resolve("cache"));

        DaemonMetadata metadata = registry.createMetadata();
        registry.write(metadata);

        Optional<DaemonMetadata> loaded = registry.aliveMetadata();
        assertThat(loaded).isPresent();
        assertThat(loaded.get().pid()).isEqualTo(ProcessHandle.current().pid());
        assertThat(loaded.get().host()).isEqualTo("127.0.0.1");
        assertThat(loaded.get().port()).isPositive();
        assertThat(loaded.get().token()).isNotBlank();
        assertThat(registry.metadataDirectory().resolve("daemon.json")).exists();
        assertThat(registry.metadataDirectory().resolve("daemon.pid")).exists();
    }

    @Test
    void staleMetadataIsCleaned(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        DaemonProcessRegistry registry = new DaemonProcessRegistry(repo, tempDir.resolve("cache"));
        DaemonMetadata stale = new DaemonMetadata(Long.MAX_VALUE, repo, "websocket",
                                                  "127.0.0.1", 49152, "token",
                                                  java.time.Instant.now());
        registry.write(stale);

        Optional<DaemonMetadata> loaded = registry.aliveMetadata();

        assertThat(loaded).isEmpty();
        assertThat(registry.metadataDirectory()).doesNotExist();
    }
}

package de.zorro909.codecheck.daemon;

import java.nio.file.Path;
import java.time.Instant;

public record DaemonMetadata(long pid,
                             Path repoRoot,
                             String transport,
                             String host,
                             int port,
                             String token,
                             Instant startedAt) {
}

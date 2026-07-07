package de.zorro909.codecheck.daemon;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

final class DaemonMetadataStore {

    private static final String DAEMON_JSON = "daemon.json";
    private static final String DAEMON_PID = "daemon.pid";
    private static final boolean POSIX_PERMISSIONS_SUPPORTED = FileSystems.getDefault()
            .supportedFileAttributeViews()
            .contains("posix");

    private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    Optional<DaemonMetadata> read(Path metadataDirectory) {
        Path metadataFile = metadataDirectory.resolve(DAEMON_JSON);
        if (!Files.exists(metadataFile)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(metadataFile)) {
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map<?, ?> values)) {
                return Optional.empty();
            }
            return Optional.of(new DaemonMetadata(
                    number(values, "pid").longValue(),
                    Path.of(string(values, "repoRoot")),
                    string(values, "transport"),
                    string(values, "host"),
                    number(values, "port").intValue(),
                    string(values, "token"),
                    Instant.parse(string(values, "startedAt"))));
        } catch (RuntimeException | IOException e) {
            delete(metadataDirectory);
            return Optional.empty();
        }
    }

    void write(Path metadataDirectory, DaemonMetadata metadata) {
        try {
            createOwnerOnlyDirectories(metadataDirectory);
            writeOwnerOnly(metadataDirectory.resolve(DAEMON_JSON), toJson(metadata));
            writeOwnerOnly(metadataDirectory.resolve(DAEMON_PID),
                           Long.toString(metadata.pid()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write daemon metadata", e);
        }
    }

    private void createOwnerOnlyDirectories(Path directory) throws IOException {
        if (!POSIX_PERMISSIONS_SUPPORTED) {
            Files.createDirectories(directory);
            return;
        }
        Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rwx------");
        FileAttribute<Set<PosixFilePermission>> attribute =
                PosixFilePermissions.asFileAttribute(ownerOnly);
        Files.createDirectories(directory, attribute);
        Files.setPosixFilePermissions(directory, ownerOnly);
    }

    private void writeOwnerOnly(Path file, String content) throws IOException {
        if (POSIX_PERMISSIONS_SUPPORTED) {
            Files.deleteIfExists(file);
            Files.createFile(file, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------")));
        }
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    void delete(Path metadataDirectory) {
        if (!Files.exists(metadataDirectory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(metadataDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deletePath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete daemon metadata", e);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete " + path, e);
        }
    }

    private String toJson(DaemonMetadata metadata) {
        return """
                {
                  "pid": %d,
                  "repoRoot": "%s",
                  "transport": "%s",
                  "host": "%s",
                  "port": %d,
                  "token": "%s",
                  "startedAt": "%s"
                }
                """.formatted(metadata.pid(), escape(metadata.repoRoot().toString()),
                               escape(metadata.transport()), escape(metadata.host()),
                               metadata.port(), escape(metadata.token()), metadata.startedAt());
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Number number(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number;
        }
        throw new IllegalArgumentException("Missing numeric metadata field " + key);
    }

    private String string(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("Missing string metadata field " + key);
    }
}

package de.zorro909.codecheck.daemon;

import de.zorro909.codecheck.core.RepositoryPathProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Singleton
public class DaemonProcessRegistry {

    static final String TRANSPORT_WEBSOCKET = "websocket";

    private static final String HOST = "127.0.0.1";

    private final Path repositoryDirectory;

    private final Path cacheRoot;

    private final DaemonMetadataStore metadataStore;

    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public DaemonProcessRegistry(@Named(RepositoryPathProvider.REPOSITORY_DIRECTORY) Path repositoryDirectory) {
        this(repositoryDirectory, defaultCacheRoot());
    }

    public DaemonProcessRegistry(Path repositoryDirectory, Path cacheRoot) {
        this.repositoryDirectory = repositoryDirectory.toAbsolutePath().normalize();
        this.cacheRoot = cacheRoot.toAbsolutePath().normalize();
        this.metadataStore = new DaemonMetadataStore();
    }

    public String repoId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(repositoryDirectory.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public Path metadataDirectory() {
        return cacheRoot.resolve("repos").resolve(repoId());
    }

    public Optional<DaemonMetadata> aliveMetadata() {
        Optional<DaemonMetadata> metadata = metadataStore.read(metadataDirectory());
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        if (ProcessHandle.of(metadata.get().pid()).filter(ProcessHandle::isAlive).isPresent()) {
            return metadata;
        }
        clear();
        return Optional.empty();
    }

    public DaemonMetadata createMetadata() {
        return new DaemonMetadata(ProcessHandle.current().pid(), repositoryDirectory, TRANSPORT_WEBSOCKET, HOST,
                randomPort(), randomToken(), Instant.now());
    }

    public void write(DaemonMetadata metadata) {
        metadataStore.write(metadataDirectory(), metadata);
    }

    public void clear() {
        metadataStore.delete(metadataDirectory());
    }

    private int randomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to allocate daemon port", e);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Path defaultCacheRoot() {
        return Path.of(System.getProperty("user.home"), ".cache", "git-commit-code-check");
    }

}

package de.zorro909.codecheck;

import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Singleton
public class FileLoader {

    private final Optional<DaemonServer> daemonServer;
    private final Path rootDirectory;

    public FileLoader(Path rootDirectory, Optional<DaemonServer> daemonServer) {
        this.daemonServer = daemonServer;
        this.rootDirectory = rootDirectory;
    }

    public Path getPath(String filePath) {
        Path path = rootDirectory.resolve(filePath);
        daemonServer.ifPresent(daemonServer -> daemonServer.markFile(path));
        return path;
    }

    public File getFile(String filePath) {
        return getPath(filePath).toFile();
    }

    public byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(getPath(filePath));
    }

    public boolean fileExists(String filePath) {
        return getFile(filePath).exists();
    }

    public void markFile(Path path) {
        daemonServer.ifPresent(daemonServer -> daemonServer.markFile(path));
    }

    public boolean fileExists(Path path) {
        daemonServer.ifPresent(daemonServer -> daemonServer.markFile(path));
        return Files.exists(path);
    }
}

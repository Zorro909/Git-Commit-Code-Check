package de.zorro909.codecheck.daemon;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.zorro909.codecheck.RequiresCliOption;
import de.zorro909.codecheck.ValidationCheckPipeline;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.selector.FileSelector;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a daemon server that runs a file selection process, starts an HTTP server, and runs indefinitely.
 */
@RequiresCliOption("--daemon")
@Singleton
public class DaemonServer {

    private final FileSelector fileSelector;
    private final Provider<ValidationCheckPipeline> validationCheckPipeline;

    private final Map<Path, List<Path>> fileDependencies = new HashMap<>();
    private Path currentFile;

    public DaemonServer(FileSelector fileSelector,
                        Provider<ValidationCheckPipeline> validationCheckPipeline) {
        this.fileSelector = fileSelector;
        this.validationCheckPipeline = validationCheckPipeline;
    }

    /**
     * Runs the application by selecting files, updating files, starting an HTTP server, and running the server indefinitely.
     *
     * @throws IOException            if an I/O error occurs during the file selection process.
     * @throws InterruptedException   if the current thread is interrupted while sleeping.
     */
    public void run() throws IOException, InterruptedException {
        fileSelector.selectFiles().forEach(this::updateFile);
        HttpServer server = getHttpServer();
        server.start();
        runServerIndefinitely();
    }

    private HttpServer getHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(23464), 4);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/check", this::handleHttpExchange);
        return server;
    }

    private void handleHttpExchange(HttpExchange httpExchange) throws IOException {
        ValidationCheckPipeline vcp = validationCheckPipeline.get();
        String validationOutput = getValidationOutput(vcp);
        byte[] response = validationOutput.getBytes(StandardCharsets.UTF_8);
        httpExchange.sendResponseHeaders(200, response.length);
        httpExchange.getResponseBody().write(response);
    }

    private String getValidationOutput(ValidationCheckPipeline vcp) throws IOException {
        return fileSelector.selectFiles()
                           .map(vcp::checkFile)
                           .reduce(Stream::concat)
                           .orElse(Stream.empty())
                           .map(ValidationError::toString)
                           .collect(Collectors.joining("\n"));
    }

    private void runServerIndefinitely() {
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                System.err.println("Server interrupted: " + e.getMessage());
            }
        }
    }

    /**
     * Marks the specified file as a dependency for the currently computed File.
     *
     * @param path The path of the file to mark.
     */
    public void markFile(Path path) {
        if (currentFile.equals(path)) {
            return;
        }
        List<Path> paths = fileDependencies.getOrDefault(currentFile, new ArrayList<>());
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }

    /**
     * Updates the specified file and its dependencies.
     *
     * @param path The path of the file to update.
     */
    public synchronized void updateFile(Path path) {
        if (fileDependencies.containsKey(path)) {
            fileDependencies.get(path).forEach(this::updateFile);
        }
        this.currentFile = path;
        validationCheckPipeline.get()
                               .checkFile(path)
                               .map(ValidationError::toString)
                               .forEach(System.out::println);
    }
}

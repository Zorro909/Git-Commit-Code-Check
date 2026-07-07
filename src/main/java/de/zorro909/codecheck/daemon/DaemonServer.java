package de.zorro909.codecheck.daemon;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.zorro909.codecheck.legacy.ValidationCheckPipeline;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.legacy.selector.FileSelector;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a daemon server that runs a file selection process, starts an HTTP server,
 * and runs indefinitely.
 */
@Singleton
public class DaemonServer {

    private final FileSelector fileSelector;

    private final Provider<ValidationCheckPipeline> validationCheckPipeline;

    private final Map<Path, Set<Path>> fileDependencies = new HashMap<>();

    private Path currentFile;

    private final AtomicReference<Instant> lastActivity = new AtomicReference<>(Instant.now());

    public DaemonServer(FileSelector fileSelector, Provider<ValidationCheckPipeline> validationCheckPipeline) {
        this.fileSelector = fileSelector;
        this.validationCheckPipeline = validationCheckPipeline;
    }

    /**
     * Starts the daemon control server described by the metadata and blocks until it is
     * shut down via the control endpoint or the inactivity timeout elapses.
     * @param metadata host, port, and auth token the server binds and authorizes with.
     * @param inactivityTimeout idle time after which the server stops itself.
     * @throws IOException if the server cannot bind to the configured address.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public void run(DaemonMetadata metadata, Duration inactivityTimeout) throws IOException, InterruptedException {
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        HttpServer server = getHttpServer(metadata, shutdownLatch);
        ScheduledExecutorService idleMonitor = startIdleMonitor(server, shutdownLatch, inactivityTimeout);
        server.start();
        try {
            shutdownLatch.await();
        }
        finally {
            idleMonitor.shutdownNow();
            server.stop(0);
        }
    }

    private HttpServer getHttpServer(DaemonMetadata metadata, CountDownLatch shutdownLatch) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(metadata.host(), metadata.port()), 4);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/health",
                httpExchange -> handleAuthorized(metadata, httpExchange, () -> sendResponse(httpExchange, 204, "")));
        server.createContext("/check",
                httpExchange -> handleAuthorized(metadata, httpExchange, () -> handleCheck(httpExchange)));
        server.createContext("/shutdown", httpExchange -> handleAuthorized(metadata, httpExchange, () -> {
            sendResponse(httpExchange, 204, "");
            shutdownLatch.countDown();
        }));
        return server;
    }

    private ScheduledExecutorService startIdleMonitor(HttpServer server, CountDownLatch shutdownLatch,
            Duration inactivityTimeout) {
        ScheduledExecutorService idleMonitor = Executors.newSingleThreadScheduledExecutor();
        idleMonitor.scheduleAtFixedRate(() -> {
            if (Duration.between(lastActivity.get(), Instant.now()).compareTo(inactivityTimeout) >= 0) {
                server.stop(0);
                shutdownLatch.countDown();
            }
        }, 1, 1, TimeUnit.SECONDS);
        return idleMonitor;
    }

    private void handleAuthorized(DaemonMetadata metadata, HttpExchange httpExchange, ThrowingRunnable handler)
            throws IOException {
        if (metadata.token().isEmpty()
                || !metadata.token().equals(httpExchange.getRequestHeaders().getFirst("X-CodeCheck-Token"))) {
            sendResponse(httpExchange, 401, "Unauthorized");
            return;
        }
        refreshActivity();
        handler.run();
    }

    private void handleCheck(HttpExchange httpExchange) throws IOException {
        ValidationCheckPipeline vcp = validationCheckPipeline.get();
        String validationOutput = getValidationOutput(vcp);
        sendResponse(httpExchange, 200, validationOutput);
    }

    private void sendResponse(HttpExchange httpExchange, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        httpExchange.sendResponseHeaders(statusCode, response.length == 0 ? -1 : response.length);
        try (var responseBody = httpExchange.getResponseBody()) {
            responseBody.write(response);
        }
    }

    private String getValidationOutput(ValidationCheckPipeline vcp) throws IOException {
        return fileSelector.selectFiles()
            .map(vcp::checkFile)
            .reduce(Stream::concat)
            .orElse(Stream.empty())
            .map(ValidationError::toString)
            .collect(Collectors.joining("\n"));
    }

    /**
     * Marks the specified file as a dependency for the currently computed File.
     * @param path The path of the file to mark.
     */
    public void markFile(Path path) {
        if (currentFile.equals(path)) {
            return;
        }
        fileDependencies.computeIfAbsent(currentFile, _ -> new HashSet<>()).add(path);
    }

    /**
     * Updates the specified file and its dependencies.
     * @param path The path of the file to update.
     */
    public synchronized void updateFile(Path path) {
        refreshActivity();
        if (fileDependencies.containsKey(path)) {
            fileDependencies.get(path).forEach(this::updateFile);
        }
        this.currentFile = path;
        validationCheckPipeline.get().checkFile(path).map(ValidationError::toString).forEach(System.out::println);
    }

    private void refreshActivity() {
        lastActivity.set(Instant.now());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws IOException;

    }

}

package de.zorro909.codecheck;

import com.sun.net.httpserver.HttpServer;
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

    public void run() throws IOException, InterruptedException {
        fileSelector.selectFiles().forEach(this::updateFile);
        HttpServer server = HttpServer.create(new InetSocketAddress(23464), 4);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/check", (httpExchange) -> {
            ValidationCheckPipeline vcp = validationCheckPipeline.get();
            String output = fileSelector.selectFiles()
                                        .map(vcp::checkFile)
                                        .reduce(Stream::concat)
                                        .orElse(Stream.empty())
                                        .map(ValidationError::toString)
                                        .collect(Collectors.joining("\n"));
            byte[] response = output.getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(200, response.length);
            httpExchange.getResponseBody().write(response);
        });
        server.start();

        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                System.err.println("Server interrupted: " + e.getMessage());
            }
        }
    }

    public void markFile(Path path) {
        if (currentFile.equals(path)) {
            return;
        }
        List<Path> paths = fileDependencies.getOrDefault(currentFile, new ArrayList<>());
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }

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

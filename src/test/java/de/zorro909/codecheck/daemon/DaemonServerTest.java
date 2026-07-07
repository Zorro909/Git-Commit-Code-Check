package de.zorro909.codecheck.daemon;

import de.zorro909.codecheck.legacy.ValidationCheckPipeline;
import de.zorro909.codecheck.legacy.selector.FileSelector;
import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DaemonServerTest {

    @Test
    void controlEndpointsRequireToken(@TempDir Path tempDir) throws Exception {
        DaemonProcessRegistry registry = new DaemonProcessRegistry(tempDir.resolve("repo"), tempDir.resolve("cache"));
        DaemonMetadata metadata = registry.createMetadata();
        DaemonServer server = new DaemonServer(emptySelector(), emptyPipelineProvider());
        Thread serverThread = Thread.ofVirtual().start(() -> run(server, metadata));
        try {
            URI healthUri = URI.create("http://" + metadata.host() + ":" + metadata.port() + "/health");
            waitUntilReady(healthUri, metadata.token());

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> unauthorized = client.send(HttpRequest.newBuilder(healthUri).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> authorized = client.send(
                    HttpRequest.newBuilder(healthUri).header("X-CodeCheck-Token", metadata.token()).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(unauthorized.statusCode()).isEqualTo(401);
            assertThat(authorized.statusCode()).isEqualTo(204);
        }
        finally {
            shutdown(metadata);
            serverThread.join(Duration.ofSeconds(5));
        }
    }

    @Test
    void emptyTokenMetadataRejectsAllRequests(@TempDir Path tempDir) throws Exception {
        DaemonProcessRegistry registry = new DaemonProcessRegistry(tempDir.resolve("repo"), tempDir.resolve("cache"));
        DaemonMetadata withToken = registry.createMetadata();
        DaemonMetadata metadata = new DaemonMetadata(withToken.pid(), withToken.repoRoot(), withToken.transport(),
                withToken.host(), withToken.port(), "", withToken.startedAt());
        DaemonServer server = new DaemonServer(emptySelector(), emptyPipelineProvider());
        Thread serverThread = Thread.ofVirtual().start(() -> run(server, metadata));
        try {
            URI healthUri = URI.create("http://" + metadata.host() + ":" + metadata.port() + "/health");
            waitUntilResponding(healthUri);

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> withoutHeader = client.send(HttpRequest.newBuilder(healthUri).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> withEmptyHeader = client.send(
                    HttpRequest.newBuilder(healthUri).header("X-CodeCheck-Token", "").GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(withoutHeader.statusCode()).isEqualTo(401);
            assertThat(withEmptyHeader.statusCode()).isEqualTo(401);
        }
        finally {
            serverThread.interrupt();
            serverThread.join(Duration.ofSeconds(5));
        }
    }

    private void waitUntilResponding(URI healthUri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Instant deadline = Instant.now().plusSeconds(5);
        while (Instant.now().isBefore(deadline)) {
            try {
                client.send(HttpRequest.newBuilder(healthUri).GET().build(), HttpResponse.BodyHandlers.ofString());
                return;
            }
            catch (Exception ignored) {
                Thread.sleep(50);
            }
        }
        throw new AssertionError("Daemon server did not become ready");
    }

    private void run(DaemonServer server, DaemonMetadata metadata) {
        try {
            server.run(metadata, Duration.ofMinutes(5));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitUntilReady(URI healthUri, String token) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Instant deadline = Instant.now().plusSeconds(5);
        while (Instant.now().isBefore(deadline)) {
            try {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(healthUri).header("X-CodeCheck-Token", token).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 204) {
                    return;
                }
            }
            catch (Exception ignored) {
                Thread.sleep(50);
            }
        }
        throw new AssertionError("Daemon server did not become ready");
    }

    private void shutdown(DaemonMetadata metadata) throws Exception {
        HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(URI.create("http://" + metadata.host() + ":" + metadata.port() + "/shutdown"))
                .header("X-CodeCheck-Token", metadata.token())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.discarding());
    }

    private FileSelector emptySelector() {
        return () -> Stream.empty();
    }

    private Provider<ValidationCheckPipeline> emptyPipelineProvider() {
        return () -> {
            ValidationCheckPipeline pipeline = new ValidationCheckPipeline();
            setField(pipeline, "codeChecker", List.of());
            return pipeline;
        };
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

}

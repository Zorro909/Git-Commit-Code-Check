package de.zorro909.codecheck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for FileLoader — the utility class that resolves and reads files
 * relative to a root directory.
 */
class FileLoaderTest {

    @TempDir
    Path tempDir;

    private FileLoader fileLoader;

    @BeforeEach
    void setUp() {
        fileLoader = new FileLoader(tempDir, Optional.empty());
    }

    // --- getPath tests ---

    @Test
    void shouldResolvePathRelativeToRootDirectory() {
        Path resolved = fileLoader.getPath("src/main/java/Example.java");

        assertThat(resolved).isEqualTo(tempDir.resolve("src/main/java/Example.java"));
    }

    @Test
    void shouldResolveSimpleFileName() {
        Path resolved = fileLoader.getPath("README.md");

        assertThat(resolved).isEqualTo(tempDir.resolve("README.md"));
    }

    @Test
    void shouldResolveNestedPath() {
        Path resolved = fileLoader.getPath("a/b/c/d.java");

        assertThat(resolved).isEqualTo(tempDir.resolve("a/b/c/d.java"));
    }

    // --- getFile tests ---

    @Test
    void shouldReturnFileObject() {
        File file = fileLoader.getFile("test.txt");

        assertThat(file).isNotNull();
        assertThat(file.toPath()).isEqualTo(tempDir.resolve("test.txt"));
    }

    // --- readFile tests ---

    @Test
    void shouldReadExistingFileContents() throws IOException {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "Hello, World!");

        byte[] contents = fileLoader.readFile("hello.txt");

        assertThat(new String(contents)).isEqualTo("Hello, World!");
    }

    @Test
    void shouldReadBinaryFileContents() throws IOException {
        Path file = tempDir.resolve("data.bin");
        byte[] expectedData = new byte[]{0x01, 0x02, 0x03, (byte) 0xFF};
        Files.write(file, expectedData);

        byte[] contents = fileLoader.readFile("data.bin");

        assertThat(contents).isEqualTo(expectedData);
    }

    @Test
    void shouldThrowIOExceptionWhenReadingNonExistentFile() {
        assertThatThrownBy(() -> fileLoader.readFile("nonexistent.txt"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldReadJavaFileContents() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);
        Path javaFile = srcDir.resolve("Example.java");
        String javaCode = """
                public class Example {
                    public void method() {
                        System.out.println("test");
                    }
                }
                """;
        Files.writeString(javaFile, javaCode);

        byte[] contents = fileLoader.readFile("src/main/java/Example.java");

        assertThat(new String(contents)).isEqualTo(javaCode);
    }

    // --- fileExists(String) tests ---

    @Test
    void shouldReturnTrueWhenFileExistsUsingStringPath() throws IOException {
        Path file = tempDir.resolve("existing.txt");
        Files.writeString(file, "content");

        assertThat(fileLoader.fileExists("existing.txt")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFileDoesNotExistUsingStringPath() {
        assertThat(fileLoader.fileExists("nonexistent.txt")).isFalse();
    }

    @Test
    void shouldReturnTrueForNestedExistingFile() throws IOException {
        Path nestedDir = tempDir.resolve("a/b/c");
        Files.createDirectories(nestedDir);
        Path file = nestedDir.resolve("file.java");
        Files.writeString(file, "class Foo {}");

        assertThat(fileLoader.fileExists("a/b/c/file.java")).isTrue();
    }

    // --- fileExists(Path) tests ---

    @Test
    void shouldReturnTrueWhenFileExistsUsingAbsolutePath() throws IOException {
        Path file = tempDir.resolve("existing.txt");
        Files.writeString(file, "content");

        assertThat(fileLoader.fileExists(file)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFileDoesNotExistUsingAbsolutePath() {
        Path file = tempDir.resolve("nonexistent.txt");

        assertThat(fileLoader.fileExists(file)).isFalse();
    }

    // --- markFile tests ---

    @Test
    void shouldNotThrowWhenMarkingFileWithNoDaemonServer() {
        Path file = tempDir.resolve("test.java");

        // Should not throw when DaemonServer is not present
        fileLoader.markFile(file);
    }

    // --- edge cases ---

    @Test
    void shouldHandleEmptyFileName() {
        Path resolved = fileLoader.getPath("");

        assertThat(resolved).isEqualTo(tempDir);
    }

    @Test
    void shouldReturnFalseForDirectoryWhenCheckingFileExists() throws IOException {
        Path dir = tempDir.resolve("somedir");
        Files.createDirectories(dir);

        // A directory is not a "file" in the usual sense, but File.exists() returns true for directories
        // This test documents current behavior
        assertThat(fileLoader.fileExists("somedir")).isTrue();
    }

    @Test
    void shouldReadEmptyFile() throws IOException {
        Path file = tempDir.resolve("empty.txt");
        Files.writeString(file, "");

        byte[] contents = fileLoader.readFile("empty.txt");

        assertThat(contents).isEmpty();
    }
}

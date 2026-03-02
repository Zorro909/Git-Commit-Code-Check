package de.zorro909.codecheck.selector.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for AllFileSelector — selects all regular files from a given directory.
 */
class AllFileSelectorTest {

    @TempDir
    Path tempDir;

    // --- basic selection tests ---

    @Test
    void shouldSelectAllJavaFilesFromDirectory() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);
        Path file1 = srcDir.resolve("Example1.java");
        Path file2 = srcDir.resolve("Example2.java");
        Files.writeString(file1, "public class Example1 {}");
        Files.writeString(file2, "public class Example2 {}");

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).hasSize(2);
        assertThat(files).contains(file1, file2);
    }

    @Test
    void shouldSelectFilesRecursively() throws IOException {
        Path pkg1 = tempDir.resolve("src/main/java/com/example");
        Path pkg2 = tempDir.resolve("src/main/java/com/example/sub");
        Files.createDirectories(pkg1);
        Files.createDirectories(pkg2);

        Path file1 = pkg1.resolve("Parent.java");
        Path file2 = pkg2.resolve("Child.java");
        Files.writeString(file1, "class Parent {}");
        Files.writeString(file2, "class Child {}");

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).hasSize(2);
        assertThat(files).contains(file1, file2);
    }

    @Test
    void shouldReturnEmptyStreamForEmptyDirectory() throws IOException {
        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).isEmpty();
    }

    @Test
    void shouldReturnOnlyRegularFiles() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectories(dir);
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        // Should only contain the regular file, not the directory
        assertThat(files).hasSize(1);
        assertThat(files).containsExactly(file);
    }

    @Test
    void shouldSelectAllFileTypesNotJustJava() throws IOException {
        Path javaFile = tempDir.resolve("Example.java");
        Path xmlFile = tempDir.resolve("config.xml");
        Path txtFile = tempDir.resolve("README.txt");
        Files.writeString(javaFile, "class Example {}");
        Files.writeString(xmlFile, "<config/>");
        Files.writeString(txtFile, "readme text");

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).hasSize(3);
        assertThat(files).contains(javaFile, xmlFile, txtFile);
    }

    // --- directory structure tests ---

    @Test
    void shouldHandleDeeplyNestedDirectories() throws IOException {
        Path deepDir = tempDir.resolve("a/b/c/d/e/f");
        Files.createDirectories(deepDir);
        Path deepFile = deepDir.resolve("Deep.java");
        Files.writeString(deepFile, "class Deep {}");

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).hasSize(1);
        assertThat(files).containsExactly(deepFile);
    }

    @Test
    void shouldSelectFilesFromMultipleSiblingDirectories() throws IOException {
        Path mainDir = tempDir.resolve("src/main/java");
        Path testDir = tempDir.resolve("src/test/java");
        Files.createDirectories(mainDir);
        Files.createDirectories(testDir);

        Path mainFile = mainDir.resolve("Main.java");
        Path testFile = testDir.resolve("MainTest.java");
        Files.writeString(mainFile, "class Main {}");
        Files.writeString(testFile, "class MainTest {}");

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).hasSize(2);
        assertThat(files).contains(mainFile, testFile);
    }

    @Test
    void shouldHandleDirectoryWithOnlySubdirectories() throws IOException {
        Files.createDirectories(tempDir.resolve("empty1"));
        Files.createDirectories(tempDir.resolve("empty2"));
        Files.createDirectories(tempDir.resolve("empty3"));

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).isEmpty();
    }

    // --- error handling tests ---

    @Test
    void shouldThrowIOExceptionForNonExistentDirectory() {
        Path nonExistent = tempDir.resolve("does-not-exist");

        AllFileSelector selector = new AllFileSelector(nonExistent);

        assertThatThrownBy(() -> selector.selectFiles().toList())
                .isInstanceOf(IOException.class);
    }

    // --- multiple files in same directory ---

    @Test
    void shouldSelectMultipleFilesInSameDirectory() throws IOException {
        for (int i = 0; i < 5; i++) {
            Files.writeString(tempDir.resolve("File" + i + ".java"), "class File" + i + " {}");
        }

        AllFileSelector selector = new AllFileSelector(tempDir);
        List<Path> files = selector.selectFiles().toList();

        assertThat(files).hasSize(5);
    }
}

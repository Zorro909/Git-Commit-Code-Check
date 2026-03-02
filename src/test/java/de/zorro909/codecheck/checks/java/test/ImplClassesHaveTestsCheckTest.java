package de.zorro909.codecheck.checks.java.test;

import de.zorro909.codecheck.FileLoader;
import de.zorro909.codecheck.checks.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ImplClassesHaveTestsCheck which ensures every *Impl.java class
 * in src/main/java has a corresponding *ImplTest.java in src/test/java.
 */
class ImplClassesHaveTestsCheckTest {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";
    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";

    @TempDir
    Path tempDir;

    private FileLoader fileLoader;
    private ImplClassesHaveTestsCheck check;

    @BeforeEach
    void setUp() {
        fileLoader = new FileLoader(tempDir, Optional.empty());
        check = new ImplClassesHaveTestsCheck(fileLoader);
    }

    // --- isResponsible tests ---

    @Test
    void isResponsible_returnsTrueForImplFileInMain() {
        Path path = tempDir.resolve(MAIN_FOLDER).resolve("com/example/ServiceImpl.java");

        assertThat(check.isResponsible(path)).isTrue();
    }

    @Test
    void isResponsible_returnsFalseForNonImplFile() {
        Path path = tempDir.resolve(MAIN_FOLDER).resolve("com/example/Service.java");

        assertThat(check.isResponsible(path)).isFalse();
    }

    @Test
    void isResponsible_returnsFalseForImplFileInTest() {
        Path path = tempDir.resolve(TEST_FOLDER).resolve("com/example/ServiceImpl.java");

        assertThat(check.isResponsible(path)).isFalse();
    }

    // --- check(Path) tests ---

    @Test
    void check_returnsErrorWhenNoTestFile() throws IOException {
        Path mainDir = tempDir.resolve(MAIN_FOLDER).resolve("com/example");
        Files.createDirectories(mainDir);
        Path implFile = mainDir.resolve("ServiceImpl.java");
        Files.writeString(implFile, "package com.example; public class ServiceImpl {}");

        // Do NOT create the corresponding test file
        List<ValidationError> errors = check.check(implFile);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).severity()).isEqualTo(ValidationError.Severity.MEDIUM);
    }

    @Test
    void check_returnsEmptyWhenTestFileExists() throws IOException {
        Path mainDir = tempDir.resolve(MAIN_FOLDER).resolve("com/example");
        Path testDir = tempDir.resolve(TEST_FOLDER).resolve("com/example");
        Files.createDirectories(mainDir);
        Files.createDirectories(testDir);

        Path implFile = mainDir.resolve("ServiceImpl.java");
        Files.writeString(implFile, "package com.example; public class ServiceImpl {}");

        Path testFile = testDir.resolve("ServiceImplTest.java");
        Files.writeString(testFile, "package com.example; public class ServiceImplTest {}");

        List<ValidationError> errors = check.check(implFile);

        assertThat(errors).isEmpty();
    }

    @Test
    void check_errorContainsClassName() throws IOException {
        Path mainDir = tempDir.resolve(MAIN_FOLDER).resolve("com/example");
        Files.createDirectories(mainDir);
        Path implFile = mainDir.resolve("RepositoryImpl.java");
        Files.writeString(implFile, "package com.example; public class RepositoryImpl {}");

        List<ValidationError> errors = check.check(implFile);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorMessage()).contains("RepositoryImpl.java");
    }
}

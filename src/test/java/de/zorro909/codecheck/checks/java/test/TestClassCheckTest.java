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
 * Tests for TestClassCheck which enforces that test classes extend a base
 * class and carry a @Tests annotation.
 */
class TestClassCheckTest {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";
    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";

    @TempDir
    Path tempDir;

    private TestClassCheck check;

    @BeforeEach
    void setUp() {
        FileLoader fileLoader = new FileLoader(tempDir, Optional.empty());
        check = new TestClassCheck(fileLoader);
    }

    // --- isJavaResponsible tests ---

    @Test
    void isJavaResponsible_returnsTrueForTestFile() {
        Path path = tempDir.resolve(TEST_FOLDER).resolve("com/example/MyClassTest.java");

        assertThat(check.isJavaResponsible(path)).isTrue();
    }

    @Test
    void isJavaResponsible_returnsFalseForMainFile() {
        Path path = tempDir.resolve(MAIN_FOLDER).resolve("com/example/MyClass.java");

        assertThat(check.isJavaResponsible(path)).isFalse();
    }

    @Test
    void isJavaResponsible_returnsFalseForNonTestFile() {
        Path path = tempDir.resolve(TEST_FOLDER).resolve("com/example/TestHelper.java");

        assertThat(check.isJavaResponsible(path)).isFalse();
    }

    // --- check(CompilationUnit) tests ---

    @Test
    void check_detectsTestClassWithoutExtends() throws IOException {
        String source = """
                package com.example;

                @Tests
                public class MyServiceTest {
                    @Test
                    void shouldWork() {}
                }

                @interface Tests {}
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyServiceTest.java");

        assertThat(errors).anyMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_SHOULD_EXTEND));
    }

    @Test
    void check_allowsTestClassWithExtends() throws IOException {
        // Create a base test class first
        String baseSource = """
                package com.example;

                public class BaseTest {
                }
                """;
        Path testDir = tempDir.resolve(TEST_FOLDER).resolve("com/example");
        Files.createDirectories(testDir);
        Files.writeString(testDir.resolve("BaseTest.java"), baseSource);

        String source = """
                package com.example;

                @Tests
                public class MyServiceTest extends BaseTest {
                    @Test
                    void shouldWork() {}
                }

                @interface Tests {}
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyServiceTest.java");

        assertThat(errors).noneMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_SHOULD_EXTEND));
    }

    @Test
    void check_detectsTestClassWithoutTestsAnnotation() throws IOException {
        String source = """
                package com.example;

                public class MyServiceTest extends Object {
                    @Test
                    void shouldWork() {}
                }
                """;

        // Create the base class so the extends can resolve
        Path testDir = tempDir.resolve(TEST_FOLDER).resolve("com/example");
        Files.createDirectories(testDir);

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyServiceTest.java");

        assertThat(errors).anyMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_TESTS_ANNOTATION));
    }

    @Test
    void check_allowsTestClassWithTestsAnnotation() throws IOException {
        String source = """
                package com.example;

                @Tests
                public class MyServiceTest extends Object {
                    @Test
                    void shouldWork() {}
                }

                @interface Tests {}
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyServiceTest.java");

        assertThat(errors).noneMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_TESTS_ANNOTATION));
    }

    @Test
    void check_detectsBothErrors() throws IOException {
        String source = """
                package com.example;

                public class MyServiceTest {
                    @Test
                    void shouldWork() {}
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyServiceTest.java");

        assertThat(errors).anyMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_SHOULD_EXTEND));
        assertThat(errors).anyMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_TESTS_ANNOTATION));
    }

    @Test
    void check_noErrorsForCompliantTestClass() throws IOException {
        String source = """
                package com.example;

                @Tests
                public class MyServiceTest extends Object {
                    @Test
                    void shouldWork() {}
                }

                @interface Tests {}
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyServiceTest.java");

        assertThat(errors).noneMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_SHOULD_EXTEND));
        assertThat(errors).noneMatch(e -> e.errorMessage().contains(TestClassCheck.ERROR_TEST_CLASS_TESTS_ANNOTATION));
    }

    // --- helper ---

    private List<ValidationError> parseAndCheck(String source, String packageDir, String fileName)
            throws IOException {
        Path srcDir = tempDir.resolve(TEST_FOLDER).resolve(packageDir);
        Files.createDirectories(srcDir);
        Path filePath = srcDir.resolve(fileName);
        Files.writeString(filePath, source);
        check.resetCache(filePath);

        return check.check(filePath);
    }
}

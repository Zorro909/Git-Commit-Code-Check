package de.zorro909.codecheck.checks.java.doc;

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
 * Tests for JavaDocCheck which enforces JavaDoc comments on public classes,
 * public methods, interface methods, and test methods.
 */
class JavaDocCheckTest {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";
    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";

    @TempDir
    Path tempDir;

    private JavaDocCheck check;

    @BeforeEach
    void setUp() {
        FileLoader fileLoader = new FileLoader(tempDir, Optional.empty());
        check = new JavaDocCheck(fileLoader);
    }

    // --- isJavaResponsible tests ---

    @Test
    void isJavaResponsible_returnsTrueForMainFolder() {
        Path path = tempDir.resolve(MAIN_FOLDER).resolve("com/example/MyClass.java");

        assertThat(check.isJavaResponsible(path)).isTrue();
    }

    @Test
    void isJavaResponsible_returnsTrueForTestFile() {
        Path path = tempDir.resolve(TEST_FOLDER).resolve("com/example/MyClassTest.java");

        assertThat(check.isJavaResponsible(path)).isTrue();
    }

    @Test
    void isJavaResponsible_returnsFalseForNonTestFileInTestFolder() {
        Path path = tempDir.resolve(TEST_FOLDER).resolve("com/example/TestHelper.java");

        assertThat(check.isJavaResponsible(path)).isFalse();
    }

    // --- check(CompilationUnit) tests ---

    @Test
    void check_detectsPublicClassWithoutJavadoc() throws IOException {
        String source = """
                package com.example;

                public class MyClass {
                }
                """;

        List<ValidationError> errors = parseAndCheckMain(source, "com/example", "MyClass.java");

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_CLASS));
    }

    @Test
    void check_allowsPublicClassWithJavadoc() throws IOException {
        String source = """
                package com.example;

                /**
                 * This is a proper javadoc description for the class.
                 */
                public class MyClass {
                }
                """;

        List<ValidationError> errors = parseAndCheckMain(source, "com/example", "MyClass.java");

        assertThat(errors).noneMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_CLASS));
    }

    @Test
    void check_allowsPublicClassImplementingInterface() throws IOException {
        // Create the interface in the same package first
        String interfaceSource = """
                package com.example;

                public interface MyInterface {
                }
                """;
        Path srcDir = tempDir.resolve(MAIN_FOLDER).resolve("com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("MyInterface.java"), interfaceSource);

        String source = """
                package com.example;

                public class MyClass implements MyInterface {
                }
                """;

        List<ValidationError> errors = parseAndCheckMain(source, "com/example", "MyClass.java");

        // Class implementing an interface from the same package group should NOT require javadoc
        assertThat(errors).noneMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_CLASS));
    }

    @Test
    void check_detectsPublicMethodWithoutJavadoc() throws IOException {
        String source = """
                package com.example;

                /**
                 * This is a proper javadoc description for the class.
                 */
                public class MyClass {
                    public void doSomething(String param) {
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheckMain(source, "com/example", "MyClass.java");

        assertThat(errors).anyMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_METHOD));
    }

    @Test
    void check_allowsPublicMethodWithOverrideAnnotation() throws IOException {
        String source = """
                package com.example;

                /**
                 * This is a proper javadoc description for the class.
                 */
                public class MyClass {
                    @Override
                    public String toString() {
                        return "MyClass";
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheckMain(source, "com/example", "MyClass.java");

        assertThat(errors).noneMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_METHOD));
    }

    @Test
    void check_allowsSimpleGetter() throws IOException {
        String source = """
                package com.example;

                /**
                 * This is a proper javadoc description for the class.
                 */
                public class MyClass {
                    private String name;

                    public String getName() {
                        return name;
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheckMain(source, "com/example", "MyClass.java");

        assertThat(errors).noneMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_METHOD));
    }

    @Test
    void check_detectsTestMethodWithoutJavadoc() throws IOException {
        String source = """
                package com.example;

                /**
                 * This is a proper javadoc description for the test class.
                 */
                public class MyClassTest {
                    @Test
                    public void shouldDoSomething() {
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheckTest(source, "com/example", "MyClassTest.java");

        assertThat(errors).anyMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_TEST_METHOD));
    }

    @Test
    void check_allowsTestMethodWithJavadoc() throws IOException {
        String source = """
                package com.example;

                /**
                 * This is a proper javadoc description for the test class.
                 */
                public class MyClassTest {
                    /**
                     * Verifies that something works correctly as expected.
                     */
                    @Test
                    public void shouldDoSomething() {
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheckTest(source, "com/example", "MyClassTest.java");

        assertThat(errors).noneMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_TEST_METHOD));
    }

    @Test
    void check_rejectsJavadocWithShortDescription() throws IOException {
        String source = """
                package com.example;

                /**
                 * Short
                 */
                public class MyClass {
                }
                """;

        List<ValidationError> errors = parseAndCheckMain(source, "com/example", "MyClass.java");

        // "Short" is only 5 chars, minimum is 6, so this should still be flagged
        assertThat(errors).anyMatch(e -> e.errorMessage().contains(JavaDocCheck.ERROR_MESSAGE_CLASS));
    }

    // --- helpers ---

    private List<ValidationError> parseAndCheckMain(String source, String packageDir, String fileName)
            throws IOException {
        Path srcDir = tempDir.resolve(MAIN_FOLDER).resolve(packageDir);
        Files.createDirectories(srcDir);
        Path filePath = srcDir.resolve(fileName);
        Files.writeString(filePath, source);
        check.resetCache(filePath);

        return check.check(filePath);
    }

    private List<ValidationError> parseAndCheckTest(String source, String packageDir, String fileName)
            throws IOException {
        Path srcDir = tempDir.resolve(TEST_FOLDER).resolve(packageDir);
        Files.createDirectories(srcDir);
        Path filePath = srcDir.resolve(fileName);
        Files.writeString(filePath, source);
        check.resetCache(filePath);

        return check.check(filePath);
    }
}

package de.zorro909.codecheck.checks.java.code;

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
 * Tests for NoMagicValuesCheck which detects hardcoded literal values
 * passed as arguments to method calls and constructors.
 */
class NoMagicValuesCheckTest {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";
    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";

    @TempDir
    Path tempDir;

    private NoMagicValuesCheck check;

    @BeforeEach
    void setUp() {
        FileLoader fileLoader = new FileLoader(tempDir, Optional.empty());
        check = new NoMagicValuesCheck(fileLoader);
    }

    // --- isJavaResponsible tests ---

    @Test
    void isJavaResponsible_returnsTrueForMainFolder() {
        Path path = tempDir.resolve(MAIN_FOLDER).resolve("com/example/MyClass.java");

        assertThat(check.isJavaResponsible(path)).isTrue();
    }

    @Test
    void isJavaResponsible_returnsFalseForTestFolder() {
        Path path = tempDir.resolve(TEST_FOLDER).resolve("com/example/MyClassTest.java");

        assertThat(check.isJavaResponsible(path)).isFalse();
    }

    @Test
    void isJavaResponsible_returnsFalseForRandomPath() {
        Path path = Path.of("/some/random/path/MyClass.java");

        assertThat(check.isJavaResponsible(path)).isFalse();
    }

    // --- check(CompilationUnit) tests ---

    @Test
    void check_detectsMagicStringInMethodCall() throws IOException {
        String source = """
                package com.example;

                public class MyClass {
                    public void doWork() {
                        System.out.println("hardcoded string");
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyClass.java");

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.errorMessage().contains("Magic Values"));
    }

    @Test
    void check_detectsMagicIntegerInMethodCall() throws IOException {
        String source = """
                package com.example;

                public class MyClass {
                    public void doWork() {
                        doSomething(42);
                    }

                    private void doSomething(int value) {}
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyClass.java");

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.errorMessage().contains("42"));
        assertThat(errors).allMatch(e -> e.severity() == ValidationError.Severity.LOW);
    }

    @Test
    void check_allowsNullLiteral() throws IOException {
        String source = """
                package com.example;

                public class MyClass {
                    public void doWork() {
                        doSomething(null);
                    }

                    private void doSomething(Object value) {}
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyClass.java");

        assertThat(errors).isEmpty();
    }

    @Test
    void check_allowsBooleanLiteral() throws IOException {
        String source = """
                package com.example;

                public class MyClass {
                    public void doWork() {
                        doSomething(true);
                        doSomething(false);
                    }

                    private void doSomething(boolean value) {}
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyClass.java");

        assertThat(errors).isEmpty();
    }

    @Test
    void check_detectsMagicValueInConstructor() throws IOException {
        String source = """
                package com.example;

                public class MyClass {
                    public void doWork() {
                        Object obj = new StringBuilder("literal");
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyClass.java");

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.errorMessage().contains("literal"));
    }

    @Test
    void check_noErrorsWhenNoMagicValues() throws IOException {
        String source = """
                package com.example;

                public class MyClass {
                    private static final String CONSTANT = "value";

                    public void doWork() {
                        String local = CONSTANT;
                        doSomething(local);
                    }

                    private void doSomething(String value) {}
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyClass.java");

        assertThat(errors).isEmpty();
    }

    @Test
    void check_skipsGeneratedClasses() throws IOException {
        String source = """
                package com.example;

                import javax.annotation.processing.Generated;

                @Generated("some-generator")
                public class MyClass {
                    public void doWork() {
                        System.out.println("hardcoded");
                    }
                }
                """;

        List<ValidationError> errors = parseAndCheck(source, "com/example", "MyClass.java");

        assertThat(errors).isEmpty();
    }

    // --- helper ---

    private List<ValidationError> parseAndCheck(String source, String packageDir, String fileName)
            throws IOException {
        Path srcDir = tempDir.resolve(MAIN_FOLDER).resolve(packageDir);
        Files.createDirectories(srcDir);
        Path filePath = srcDir.resolve(fileName);
        Files.writeString(filePath, source);
        check.resetCache(filePath);

        return check.check(filePath);
    }
}

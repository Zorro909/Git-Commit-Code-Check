package de.zorro909.codecheck.actions.fix;

import com.github.javaparser.Position;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.editor.EditorExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ManualEditorFixAction — the fix action that opens an editor
 * for the user to manually fix validation errors.
 */
class ManualEditorFixActionTest {

    private ManualEditorFixAction action;
    private StubEditorExecutor stubEditorExecutor;

    @BeforeEach
    void setUp() {
        stubEditorExecutor = new StubEditorExecutor();
        action = new ManualEditorFixAction(stubEditorExecutor);
    }

    // --- canFixError tests ---

    @Test
    void canFixError_alwaysReturnsTrue() {
        ValidationError error = new ValidationError(
                Path.of("src/Example.java"),
                "Some error",
                new Position(1, 1),
                ValidationError.Severity.LOW);

        assertThat(action.canFixError(error)).isTrue();
    }

    @Test
    void canFixError_returnsTrueForDifferentSeverities() {
        Path filePath = Path.of("src/Example.java");

        ValidationError lowError = new ValidationError(
                filePath, "Low error", new Position(1, 1),
                ValidationError.Severity.LOW);
        ValidationError mediumError = new ValidationError(
                filePath, "Medium error", new Position(5, 10),
                ValidationError.Severity.MEDIUM);
        ValidationError highError = new ValidationError(
                filePath, "High error", new Position(20, 3),
                ValidationError.Severity.HIGH);

        assertThat(action.canFixError(lowError)).isTrue();
        assertThat(action.canFixError(mediumError)).isTrue();
        assertThat(action.canFixError(highError)).isTrue();
    }

    // --- fixError tests ---

    @Test
    void fixError_delegatesToEditorExecutor() {
        Path filePath = Path.of("src/Example.java");
        Position position = new Position(10, 5);
        ValidationError error = new ValidationError(
                filePath, "Test error", position,
                ValidationError.Severity.MEDIUM);

        stubEditorExecutor.setOpenAndWaitResult(true);
        action.fixError(error);

        assertThat(stubEditorExecutor.getLastOpenAndWaitPath()).isEqualTo(filePath);
        assertThat(stubEditorExecutor.getLastOpenAndWaitPosition()).isEqualTo(position);
        assertThat(stubEditorExecutor.getOpenAndWaitCallCount()).isEqualTo(1);
    }

    @Test
    void fixError_returnsTrueWhenEditorSucceeds() {
        ValidationError error = new ValidationError(
                Path.of("src/Example.java"),
                "Test error",
                new Position(1, 1),
                ValidationError.Severity.LOW);

        stubEditorExecutor.setOpenAndWaitResult(true);

        assertThat(action.fixError(error)).isTrue();
    }

    @Test
    void fixError_returnsFalseWhenEditorFails() {
        ValidationError error = new ValidationError(
                Path.of("src/Example.java"),
                "Test error",
                new Position(1, 1),
                ValidationError.Severity.LOW);

        stubEditorExecutor.setOpenAndWaitResult(false);

        assertThat(action.fixError(error)).isFalse();
    }

    @Test
    void fixError_passesCorrectPositionToEditor() {
        Position expectedPosition = new Position(42, 17);
        Path expectedPath = Path.of("src/deep/nested/MyClass.java");
        ValidationError error = new ValidationError(
                expectedPath, "Position test", expectedPosition,
                ValidationError.Severity.HIGH);

        stubEditorExecutor.setOpenAndWaitResult(true);
        action.fixError(error);

        assertThat(stubEditorExecutor.getLastOpenAndWaitPosition().line)
                .isEqualTo(42);
        assertThat(stubEditorExecutor.getLastOpenAndWaitPosition().column)
                .isEqualTo(17);
        assertThat(stubEditorExecutor.getLastOpenAndWaitPath())
                .isEqualTo(expectedPath);
    }

    @Test
    void fixError_printsValidationError() {
        ValidationError error = new ValidationError(
                Path.of("src/Example.java"),
                "Missing semicolon",
                new Position(5, 10),
                ValidationError.Severity.HIGH);

        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capturedOutput));

            stubEditorExecutor.setOpenAndWaitResult(true);
            action.fixError(error);
        } finally {
            System.setOut(originalOut);
        }

        String output = capturedOutput.toString().trim();
        assertThat(output).contains("Missing semicolon");
        assertThat(output).contains("HIGH");
        assertThat(output).contains("Example.java");
    }

    // --- Stub implementation of EditorExecutor ---

    private static class StubEditorExecutor implements EditorExecutor {

        private boolean openAndWaitResult;
        private Path lastOpenAndWaitPath;
        private Position lastOpenAndWaitPosition;
        private int openAndWaitCallCount;

        void setOpenAndWaitResult(boolean result) {
            this.openAndWaitResult = result;
        }

        Path getLastOpenAndWaitPath() {
            return lastOpenAndWaitPath;
        }

        Position getLastOpenAndWaitPosition() {
            return lastOpenAndWaitPosition;
        }

        int getOpenAndWaitCallCount() {
            return openAndWaitCallCount;
        }

        @Override
        public boolean open(Path path, Position line) {
            return false;
        }

        @Override
        public boolean openAndWait(Path path, Position line) {
            openAndWaitCallCount++;
            lastOpenAndWaitPath = path;
            lastOpenAndWaitPosition = line;
            return openAndWaitResult;
        }
    }
}

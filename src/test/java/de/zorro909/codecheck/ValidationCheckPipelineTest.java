package de.zorro909.codecheck;

import de.zorro909.codecheck.actions.FixAction;
import de.zorro909.codecheck.actions.PostAction;
import de.zorro909.codecheck.checks.CodeCheck;
import de.zorro909.codecheck.checks.ValidationError;
import com.github.javaparser.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the core ValidationCheckPipeline.
 * Uses manual construction to avoid Micronaut DI complexity
 * while thoroughly testing pipeline behavior.
 */
class ValidationCheckPipelineTest {

    private ValidationCheckPipeline pipeline;
    private List<CodeCheck> mockChecks;
    private List<FixAction> mockFixActions;
    private List<PostAction> mockPostActions;

    @BeforeEach
    void setUp() {
        mockChecks = new ArrayList<>();
        mockFixActions = new ArrayList<>();
        mockPostActions = new ArrayList<>();

        pipeline = new ValidationCheckPipeline();
        // Use reflection to set @Inject fields since they are package-private
        setField(pipeline, "codeChecker", mockChecks);
        setField(pipeline, "fixActions", mockFixActions);
        setField(pipeline, "postActions", mockPostActions);
    }

    // --- checkForErrors tests ---

    @Test
    void shouldReturnEmptyMapWhenNoFilesProvided() {
        Map<Path, List<ValidationError>> errors = pipeline.checkForErrors(Stream.empty());

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnEmptyMapWhenNoCheckersRegistered() {
        Path file = Path.of("/tmp/Example.java");

        Map<Path, List<ValidationError>> errors = pipeline.checkForErrors(Stream.of(file));

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldExecuteAllChecksOnEachFile(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");
        Files.writeString(file1, "public class File1 {}");
        Files.writeString(file2, "public class File2 {}");

        AtomicInteger checkCount = new AtomicInteger(0);

        CodeCheck alwaysResponsibleCheck = new CodeCheck() {
            @Override
            public boolean isResponsible(Path file) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path file) {
                checkCount.incrementAndGet();
                return List.of(new ValidationError(
                        file, "Test error", new Position(1, 1),
                        ValidationError.Severity.LOW));
            }

            @Override
            public void resetCache(Path file) {
            }
        };
        mockChecks.add(alwaysResponsibleCheck);

        Map<Path, List<ValidationError>> errors = pipeline.checkForErrors(Stream.of(file1, file2));

        assertThat(checkCount.get()).isEqualTo(2);
        assertThat(errors).hasSize(2);
        assertThat(errors).containsKey(file1);
        assertThat(errors).containsKey(file2);
    }

    @Test
    void shouldGroupErrorsByFilePath(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");
        Files.writeString(file1, "class File1 {}");
        Files.writeString(file2, "class File2 {}");

        CodeCheck multiErrorCheck = new CodeCheck() {
            @Override
            public boolean isResponsible(Path file) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path file) {
                return List.of(
                        new ValidationError(file, "Error A", new Position(1, 1),
                                ValidationError.Severity.LOW),
                        new ValidationError(file, "Error B", new Position(2, 1),
                                ValidationError.Severity.MEDIUM));
            }

            @Override
            public void resetCache(Path file) {
            }
        };
        mockChecks.add(multiErrorCheck);

        Map<Path, List<ValidationError>> errors = pipeline.checkForErrors(Stream.of(file1, file2));

        assertThat(errors).hasSize(2);
        assertThat(errors.get(file1)).hasSize(2);
        assertThat(errors.get(file2)).hasSize(2);
    }

    @Test
    void shouldOnlyInvokeResponsibleCheckers(@TempDir Path tempDir) throws Exception {
        Path javaFile = tempDir.resolve("Example.java");
        Path xmlFile = tempDir.resolve("config.xml");
        Files.writeString(javaFile, "class Example {}");
        Files.writeString(xmlFile, "<config/>");

        AtomicInteger javaCheckCount = new AtomicInteger(0);

        CodeCheck javaOnlyCheck = new CodeCheck() {
            @Override
            public boolean isResponsible(Path file) {
                return file.toString().endsWith(".java");
            }

            @Override
            public List<ValidationError> check(Path file) {
                javaCheckCount.incrementAndGet();
                return List.of(new ValidationError(
                        file, "Java error", new Position(1, 1),
                        ValidationError.Severity.LOW));
            }

            @Override
            public void resetCache(Path file) {
            }
        };
        mockChecks.add(javaOnlyCheck);

        Map<Path, List<ValidationError>> errors =
                pipeline.checkForErrors(Stream.of(javaFile, xmlFile));

        assertThat(javaCheckCount.get()).isEqualTo(1);
        assertThat(errors).hasSize(1);
        assertThat(errors).containsKey(javaFile);
    }

    @Test
    void shouldAggregateErrorsFromMultipleCheckers(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Example.java");
        Files.writeString(file, "class Example {}");

        CodeCheck check1 = createSimpleCheck("Error from check 1", ValidationError.Severity.LOW);
        CodeCheck check2 = createSimpleCheck("Error from check 2", ValidationError.Severity.HIGH);
        mockChecks.add(check1);
        mockChecks.add(check2);

        Map<Path, List<ValidationError>> errors = pipeline.checkForErrors(Stream.of(file));

        assertThat(errors).hasSize(1);
        assertThat(errors.get(file)).hasSize(2);
        assertThat(errors.get(file)).extracting(ValidationError::errorMessage)
                .containsExactlyInAnyOrder("Error from check 1", "Error from check 2");
    }

    // --- checkFile tests ---

    @Test
    void shouldResetCacheBeforeChecking(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Example.java");
        Files.writeString(file, "class Example {}");

        AtomicBoolean cacheReset = new AtomicBoolean(false);

        CodeCheck check = new CodeCheck() {
            @Override
            public boolean isResponsible(Path f) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path f) {
                return List.of();
            }

            @Override
            public void resetCache(Path f) {
                cacheReset.set(true);
            }
        };
        mockChecks.add(check);

        pipeline.checkFile(file).toList();

        assertThat(cacheReset.get()).isTrue();
    }

    @Test
    void shouldReturnEmptyStreamWhenNoCheckersAreResponsible(@TempDir Path tempDir)
            throws Exception {
        Path file = tempDir.resolve("config.xml");
        Files.writeString(file, "<config/>");

        CodeCheck neverResponsibleCheck = new CodeCheck() {
            @Override
            public boolean isResponsible(Path f) {
                return false;
            }

            @Override
            public List<ValidationError> check(Path f) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public void resetCache(Path f) {
            }
        };
        mockChecks.add(neverResponsibleCheck);

        List<ValidationError> errors = pipeline.checkFile(file).toList();

        assertThat(errors).isEmpty();
    }

    // --- executePostActions tests ---

    @Test
    void shouldExecuteAllPostActionsWithPath(@TempDir Path tempDir) {
        Path file = tempDir.resolve("Example.java");
        AtomicInteger executionCount = new AtomicInteger(0);

        PostAction action1 = files -> {
            executionCount.incrementAndGet();
            return true;
        };
        PostAction action2 = files -> {
            executionCount.incrementAndGet();
            return true;
        };
        mockPostActions.add(action1);
        mockPostActions.add(action2);

        pipeline.executePostActions(file);

        assertThat(executionCount.get()).isEqualTo(2);
    }

    @Test
    void shouldExecuteAllPostActionsWithPathSet(@TempDir Path tempDir) {
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");

        List<Set<Path>> receivedPaths = new ArrayList<>();
        PostAction trackingAction = files -> {
            receivedPaths.add(files);
            return true;
        };
        mockPostActions.add(trackingAction);

        Set<Path> pathSet = Set.of(file1, file2);
        pipeline.executePostActions(pathSet);

        assertThat(receivedPaths).hasSize(1);
        assertThat(receivedPaths.get(0)).containsExactlyInAnyOrderElementsOf(pathSet);
    }

    @Test
    void shouldHandleNullPostActions() {
        setField(pipeline, "postActions", null);

        // Should not throw
        pipeline.executePostActions(Path.of("/tmp/test.java"));
        pipeline.executePostActions(Set.of(Path.of("/tmp/test.java")));
    }

    // --- executeFixActions tests ---

    @Test
    void shouldExecuteFixActionsInOrder(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Example.java");
        Files.writeString(file, "class Example {}");

        List<String> executionOrder = new ArrayList<>();

        FixAction firstAction = new FixAction() {
            @Override
            public boolean canFixError(ValidationError error) {
                return true;
            }

            @Override
            public boolean fixError(ValidationError error) {
                executionOrder.add("first");
                return true;
            }
        };
        mockFixActions.add(firstAction);

        // The pipeline calls checkFile internally after a successful fix to see if errors remain.
        // The initial error was pre-constructed in the errorsMap, so the first actual call
        // to check() happens after the fix action runs. It should return empty (no more errors).
        CodeCheck check = new CodeCheck() {
            @Override
            public boolean isResponsible(Path f) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path f) {
                // After fix, there should be no more errors
                return List.of();
            }

            @Override
            public void resetCache(Path f) {
            }
        };
        mockChecks.add(check);

        ValidationError error = new ValidationError(
                file, "Fixable error", new Position(1, 1),
                ValidationError.Severity.LOW);

        Map<Path, List<ValidationError>> errorsMap = new HashMap<>();
        errorsMap.put(file, new ArrayList<>(List.of(error)));

        boolean result = pipeline.executeFixActions(errorsMap);

        assertThat(result).isTrue();
        assertThat(executionOrder).containsExactly("first");
    }

    @Test
    void shouldReturnFalseWhenFixActionCannotFix(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Example.java");
        Files.writeString(file, "class Example {}");

        FixAction unfixableAction = new FixAction() {
            @Override
            public boolean canFixError(ValidationError error) {
                return false;
            }

            @Override
            public boolean fixError(ValidationError error) {
                return false;
            }
        };
        mockFixActions.add(unfixableAction);

        // Check that always returns an error (unfixable)
        CodeCheck check = new CodeCheck() {
            @Override
            public boolean isResponsible(Path f) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path f) {
                return List.of(new ValidationError(
                        f, "Unfixable error", new Position(1, 1),
                        ValidationError.Severity.HIGH));
            }

            @Override
            public void resetCache(Path f) {
            }
        };
        mockChecks.add(check);

        ValidationError error = new ValidationError(
                file, "Unfixable error", new Position(1, 1),
                ValidationError.Severity.HIGH);

        Map<Path, List<ValidationError>> errorsMap = new HashMap<>();
        errorsMap.put(file, new ArrayList<>(List.of(error)));

        boolean result = pipeline.executeFixActions(errorsMap);

        assertThat(result).isFalse();
    }

    @Test
    void shouldHandleNullFixActions(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Example.java");
        Files.writeString(file, "class Example {}");

        setField(pipeline, "fixActions", null);

        // Add a check that always returns errors
        CodeCheck check = createSimpleCheck("Error", ValidationError.Severity.LOW);
        mockChecks.add(check);

        ValidationError error = new ValidationError(
                file, "Error", new Position(1, 1),
                ValidationError.Severity.LOW);

        Map<Path, List<ValidationError>> errorsMap = new HashMap<>();
        errorsMap.put(file, new ArrayList<>(List.of(error)));

        boolean result = pipeline.executeFixActions(errorsMap);

        assertThat(result).isFalse();
    }

    // --- helper methods ---

    private CodeCheck createSimpleCheck(String errorMessage, ValidationError.Severity severity) {
        return new CodeCheck() {
            @Override
            public boolean isResponsible(Path file) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path file) {
                return List.of(new ValidationError(
                        file, errorMessage, new Position(1, 1), severity));
            }

            @Override
            public void resetCache(Path file) {
            }
        };
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}

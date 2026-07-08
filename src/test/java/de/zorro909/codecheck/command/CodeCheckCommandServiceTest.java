package de.zorro909.codecheck.command;

import com.github.javaparser.Position;
import de.zorro909.codecheck.legacy.ValidationCheckPipeline;
import de.zorro909.codecheck.legacy.actions.FixAction;
import de.zorro909.codecheck.legacy.actions.PostAction;
import de.zorro909.codecheck.core.changeset.ChangeSet;
import de.zorro909.codecheck.core.changeset.ChangeSetEntry;
import de.zorro909.codecheck.core.changeset.ChangeSetService;
import de.zorro909.codecheck.core.changeset.GitFileStatus;
import de.zorro909.codecheck.legacy.checks.CodeCheck;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.config.CodeCheckConfig;
import de.zorro909.codecheck.core.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.core.config.ConfigException;
import de.zorro909.codecheck.core.config.ConfigOverrides;
import de.zorro909.codecheck.legacy.selector.FileSelector;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;
import de.zorro909.codecheck.core.diagnostic.DiagnosticKind;
import de.zorro909.codecheck.core.validation.FileValidationResult;
import de.zorro909.codecheck.core.validation.rule.RuleId;
import de.zorro909.codecheck.core.diagnostic.SourcePosition;
import de.zorro909.codecheck.core.validation.ValidationEngine;
import de.zorro909.codecheck.core.validation.ValidationMode;
import de.zorro909.codecheck.core.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CodeCheckCommandServiceTest {

    private RecordingDaemonController daemonController;

    private ValidationCheckPipeline pipeline;

    private List<CodeCheck> checks;

    private List<FixAction> fixActions;

    private List<PostAction> postActions;

    private ByteArrayOutputStream stdout;

    private ByteArrayOutputStream stderr;

    @BeforeEach
    void setUp() {
        daemonController = new RecordingDaemonController();
        pipeline = new ValidationCheckPipeline();
        checks = new ArrayList<>();
        fixActions = new ArrayList<>();
        postActions = new ArrayList<>();
        setField(pipeline, "codeChecker", checks);
        setField(pipeline, "fixActions", fixActions);
        setField(pipeline, "postActions", postActions);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
    }

    @Test
    void bareCommandStartsOrAttachesDaemonSilently() {
        CodeCheckCommandService service = createService(Stream::empty);

        CommandOutcome outcome = service.startAssistantDaemon();

        assertThat(outcome.exitCode()).isZero();
        assertThat(daemonController.startCalls).isEqualTo(1);
        assertThat(output()).isEmpty();
        assertThat(errorOutput()).isEmpty();
    }

    @Test
    void daemonStartupFailureIsPrintedClearly() {
        daemonController.startFailure = new IllegalStateException("port is already in use");
        CodeCheckCommandService service = createService(Stream::empty);

        CommandOutcome outcome = service.startAssistantDaemon();

        assertThat(outcome.exitCode()).isEqualTo(1);
        assertThat(errorOutput()).contains("Failed to start or attach to assistant daemon")
            .contains("port is already in use");
    }

    @Test
    void invalidConfigPreventsDaemonStartup() {
        CodeCheckCommandService service = createService(Stream::empty, new CodeCheckConfigLoader() {
            @Override
            public CodeCheckConfig load() {
                throw new ConfigException("Invalid config repo/.codecheck.yaml at git: expected");
            }

            @Override
            public CodeCheckConfig load(ConfigOverrides overrides) {
                throw new ConfigException("Invalid config repo/.codecheck.yaml at git: expected");
            }
        });

        CommandOutcome outcome = service.startAssistantDaemon();

        assertThat(outcome.exitCode()).isEqualTo(1);
        assertThat(daemonController.startCalls).isZero();
        assertThat(errorOutput()).contains("Invalid config repo/.codecheck.yaml at git");
    }

    @Test
    void batchCheckDoesNotInvokeFixActions(@TempDir Path tempDir) throws Exception {
        Path file = javaFile(tempDir);
        AtomicBoolean fixInvoked = new AtomicBoolean(false);
        checks.add(alwaysError("Low priority", ValidationError.Severity.LOW));
        fixActions.add(new FixAction() {
            @Override
            public boolean canFixError(ValidationError validationError) {
                return true;
            }

            @Override
            public boolean fixError(ValidationError validationError) {
                fixInvoked.set(true);
                return true;
            }
        });
        CodeCheckCommandService service = createService(() -> Stream.of(file));

        CommandOutcome outcome = service.runBatchCheck();

        assertThat(outcome.exitCode()).isZero();
        assertThat(fixInvoked).isFalse();
        assertThat(output()).contains("Low priority");
    }

    @Test
    void preCommitHidesLowDiagnosticsAndBlocksOnlyOnHigh(@TempDir Path tempDir) throws Exception {
        Path file = javaFile(tempDir);
        checks.add(new CodeCheck() {
            @Override
            public boolean isResponsible(Path checkedFile) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path checkedFile) {
                return List.of(error(checkedFile, "Low detail", ValidationError.Severity.LOW),
                        error(checkedFile, "Medium warning", ValidationError.Severity.MEDIUM),
                        error(checkedFile, "High failure", ValidationError.Severity.HIGH));
            }

            @Override
            public void resetCache(Path checkedFile) {
            }
        });
        CodeCheckCommandService service = createService(() -> Stream.of(file));

        CommandOutcome outcome = service.runPreCommit();

        assertThat(outcome.exitCode()).isEqualTo(1);
        assertThat(output()).doesNotContain("Low detail")
            .contains("Medium warning")
            .contains("High failure")
            .contains("Blocking HIGH diagnostics found");
    }

    @Test
    void preCommitDoesNotBlockOnMediumOnly(@TempDir Path tempDir) throws Exception {
        Path file = javaFile(tempDir);
        checks.add(alwaysError("Medium warning", ValidationError.Severity.MEDIUM));
        CodeCheckCommandService service = createService(() -> Stream.of(file));

        CommandOutcome outcome = service.runPreCommit();

        assertThat(outcome.exitCode()).isZero();
        assertThat(output()).contains("Medium warning");
    }

    @Test
    void highParseErrorFromValidationEngineBlocksPreCommit(@TempDir Path tempDir) throws Exception {
        Path file = javaFile(tempDir);
        CodeCheckCommandService service = createEngineService(file,
                List.of(diagnostic(file, "Parse failed", ValidationError.Severity.HIGH, DiagnosticKind.PARSE_ERROR)));

        CommandOutcome outcome = service.runPreCommit();

        assertThat(outcome.exitCode()).isEqualTo(1);
        assertThat(output()).contains("Parse failed").contains("Blocking HIGH diagnostics found");
    }

    @Test
    void mediumSymbolWarningFromValidationEnginePrintsWithoutBlockingPreCommit(@TempDir Path tempDir) throws Exception {
        Path file = javaFile(tempDir);
        CodeCheckCommandService service = createEngineService(file, List.of(diagnostic(file,
                "Symbol could not be resolved", ValidationError.Severity.MEDIUM, DiagnosticKind.SYMBOL_WARNING)));

        CommandOutcome outcome = service.runPreCommit();

        assertThat(outcome.exitCode()).isZero();
        assertThat(output()).contains("Symbol could not be resolved").doesNotContain("Blocking HIGH diagnostics found");
    }

    @Test
    void batchCheckFailsWhenHighDiagnosticsExist(@TempDir Path tempDir) throws Exception {
        Path file = javaFile(tempDir);
        checks.add(alwaysError("Blocking batch issue", ValidationError.Severity.HIGH));
        CodeCheckCommandService service = createService(() -> Stream.of(file));

        CommandOutcome outcome = service.runBatchCheck();

        assertThat(outcome.exitCode()).isEqualTo(1);
        assertThat(output()).contains("Blocking batch issue").contains("Blocking HIGH diagnostics found");
    }

    @Test
    void interactiveCheckInvokesFixActionsAndPostActionsAfterSuccessfulRecheck(@TempDir Path tempDir) throws Exception {
        Path file = javaFile(tempDir);
        AtomicInteger checkCalls = new AtomicInteger();
        AtomicBoolean fixInvoked = new AtomicBoolean(false);
        AtomicBoolean postActionInvoked = new AtomicBoolean(false);
        checks.add(new CodeCheck() {
            @Override
            public boolean isResponsible(Path checkedFile) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path checkedFile) {
                if (checkCalls.getAndIncrement() == 0) {
                    return List.of(error(checkedFile, "Fixable", ValidationError.Severity.HIGH));
                }
                return List.of();
            }

            @Override
            public void resetCache(Path checkedFile) {
            }
        });
        fixActions.add(new FixAction() {
            @Override
            public boolean canFixError(ValidationError validationError) {
                return true;
            }

            @Override
            public boolean fixError(ValidationError validationError) {
                fixInvoked.set(true);
                return true;
            }
        });
        postActions.add(files -> {
            postActionInvoked.set(files.equals(Set.of(file)));
            return true;
        });
        CodeCheckCommandService service = createService(() -> Stream.of(file));

        CommandOutcome outcome = service.runInteractiveCheck(false);

        assertThat(outcome.exitCode()).isZero();
        assertThat(fixInvoked).isTrue();
        assertThat(postActionInvoked).isTrue();
    }

    private CodeCheckCommandService createService(FileSelector fileSelector) {
        return createService(fileSelector, CodeCheckConfigLoader.defaultsOnly());
    }

    private CodeCheckCommandService createService(FileSelector fileSelector, CodeCheckConfigLoader configLoader) {
        return new CodeCheckCommandService(daemonController, pipeline, fileSelector, configLoader,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
    }

    private CodeCheckCommandService createEngineService(Path file, List<Diagnostic> diagnostics) {
        return new CodeCheckCommandService(daemonController, pipeline, new FixedValidationEngine(file, diagnostics),
                new SingleFileChangeSetService(file), CodeCheckConfigLoader.defaultsOnly(),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
    }

    private Path javaFile(Path tempDir) throws Exception {
        Path file = tempDir.resolve("Example.java");
        Files.writeString(file, "class Example {}");
        return file;
    }

    private CodeCheck alwaysError(String message, ValidationError.Severity severity) {
        return new CodeCheck() {
            @Override
            public boolean isResponsible(Path file) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path file) {
                return List.of(error(file, message, severity));
            }

            @Override
            public void resetCache(Path file) {
            }
        };
    }

    private ValidationError error(Path file, String message, ValidationError.Severity severity) {
        return new ValidationError(file, message, new Position(1, 1), severity);
    }

    private Diagnostic diagnostic(Path file, String message, ValidationError.Severity severity, DiagnosticKind kind) {
        return new Diagnostic(file, message, new SourcePosition(1, 1), severity, kind, new RuleId("test.rule"));
    }

    private String output() {
        return stdout.toString(StandardCharsets.UTF_8);
    }

    private String errorOutput() {
        return stderr.toString(StandardCharsets.UTF_8);
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

    private static final class RecordingDaemonController implements AssistantDaemonController {

        private int startCalls;

        private RuntimeException startFailure;

        @Override
        public void startOrAttach() {
            startCalls++;
            if (startFailure != null) {
                throw startFailure;
            }
        }

        @Override
        public void printStatus(PrintStream out) {
            out.println("status");
        }

        @Override
        public void applyFix(String diagnosticId) {
        }

    }

    private static final class FixedValidationEngine implements ValidationEngine {

        private final Path file;

        private final List<Diagnostic> diagnostics;

        private FixedValidationEngine(Path file, List<Diagnostic> diagnostics) {
            this.file = file;
            this.diagnostics = diagnostics;
        }

        @Override
        public ValidationResult validate(ChangeSet changeSet, ValidationMode mode) {
            return new ValidationResult(mode, List.of(new FileValidationResult(file, mode, diagnostics)));
        }

        @Override
        public FileValidationResult validateFile(Path file, ValidationMode mode) {
            return new FileValidationResult(file, mode, diagnostics);
        }

    }

    private static final class SingleFileChangeSetService implements ChangeSetService {

        private final Path file;

        private SingleFileChangeSetService(Path file) {
            this.file = file;
        }

        @Override
        public ChangeSet currentAssistantChangeSet() {
            return changeSet();
        }

        @Override
        public ChangeSet currentInteractiveCheckChangeSet() {
            return changeSet();
        }

        @Override
        public ChangeSet preCommitChangeSet() {
            return changeSet();
        }

        @Override
        public ChangeSet explicitFiles(java.util.Collection<Path> files) {
            return changeSet();
        }

        private ChangeSet changeSet() {
            return new ChangeSet(
                    List.of(new ChangeSetEntry(file, GitFileStatus.MODIFIED, true, false, false, false, "test file")));
        }

    }

}

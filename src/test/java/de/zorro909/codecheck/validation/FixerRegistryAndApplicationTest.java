package de.zorro909.codecheck.validation;

import com.github.javaparser.Position;
import de.zorro909.codecheck.actions.FixAction;
import de.zorro909.codecheck.actions.PostAction;
import de.zorro909.codecheck.actions.fix.ManualEditorFixAction;
import de.zorro909.codecheck.checks.CodeCheck;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.editor.EditorExecutor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FixerRegistryAndApplicationTest {

    @Test
    void manualEditorFixActionIsInteractiveAndUnavailableInBatch() {
        ManualEditorFixAction action = new ManualEditorFixAction(new NoopEditorExecutor());
        RuleRegistry registry = new DefaultRuleRegistry(List.of(), List.of(action));

        Fixer fixer = registry.activeFixers().get(0);

        assertThat(fixer.metadata().interaction()).isEqualTo(FixInteraction.INTERACTIVE);
        assertThat(registry.activeFixers(ValidationMode.INTERACTIVE)).containsExactly(fixer);
        assertThat(registry.activeFixers(ValidationMode.BATCH)).isEmpty();
        assertThat(registry.activeFixers(ValidationMode.PRE_COMMIT)).isEmpty();
    }

    @Test
    void userTriggeredFixRestagesAllAffectedFilesAfterSuccessfulRecheck() {
        Path source = Path.of("src/main/java/Example.java");
        Path test = Path.of("src/test/java/ExampleTest.java");
        Diagnostic diagnostic = diagnostic(source);
        Set<Path> affectedFiles = Set.of(source, test);
        FixAction fixAction = new FixAction() {
            @Override
            public FixerId fixerId() {
                return new FixerId("two-file-fix");
            }

            @Override
            public boolean canFixError(ValidationError validationError) {
                return true;
            }

            @Override
            public boolean fixError(ValidationError validationError) {
                return true;
            }

            @Override
            public Set<Path> affectedFiles(ValidationError validationError) {
                return affectedFiles;
            }
        };
        AtomicReference<Set<Path>> restaged = new AtomicReference<>();
        PostAction postAction = files -> {
            restaged.set(Set.copyOf(files));
            return true;
        };
        RuleRegistry registry = new DefaultRuleRegistry(List.of(passingCheck()), List.of(fixAction));
        FixApplicationService service = new FixApplicationService(
                registry, new DefaultValidationEngine(registry), List.of(postAction));

        FixResult result = service.applyUserSelectedFix(diagnostic, ValidationMode.INTERACTIVE,
                                                        new FixerId("two-file-fix"));

        assertThat(result.applied()).isTrue();
        assertThat(result.restaged()).isTrue();
        assertThat(result.affectedFiles()).containsExactlyInAnyOrderElementsOf(affectedFiles);
        assertThat(restaged.get()).containsExactlyInAnyOrderElementsOf(affectedFiles);
    }

    @Test
    void userTriggeredFixDoesNotRestageWhenAffectedFileRecheckFails() {
        Path source = Path.of("src/main/java/Example.java");
        Diagnostic diagnostic = diagnostic(source);
        AtomicReference<Set<Path>> restaged = new AtomicReference<>();
        FixAction fixAction = new FixAction() {
            @Override
            public FixerId fixerId() {
                return new FixerId("failing-fix");
            }

            @Override
            public boolean canFixError(ValidationError validationError) {
                return true;
            }

            @Override
            public boolean fixError(ValidationError validationError) {
                return true;
            }
        };
        RuleRegistry registry = new DefaultRuleRegistry(List.of(failingCheck()), List.of(fixAction));
        FixApplicationService service = new FixApplicationService(
                registry, new DefaultValidationEngine(registry), List.of(files -> {
                    restaged.set(files);
                    return true;
                }));

        FixResult result = service.applyUserSelectedFix(diagnostic, ValidationMode.INTERACTIVE,
                                                        new FixerId("failing-fix"));

        assertThat(result.applied()).isTrue();
        assertThat(result.restaged()).isFalse();
        assertThat(restaged.get()).isNull();
    }

    private Diagnostic diagnostic(Path file) {
        return new Diagnostic(file, "message", new SourcePosition(1, 1),
                              ValidationError.Severity.LOW, DiagnosticKind.RULE_VIOLATION,
                              new RuleId("rule"));
    }

    private CodeCheck passingCheck() {
        return check(List.of());
    }

    private CodeCheck failingCheck() {
        return check(List.of(new ValidationError(Path.of("src/main/java/Example.java"),
                                                 "still failing", new Position(1, 1),
                                                 ValidationError.Severity.HIGH)));
    }

    private CodeCheck check(List<ValidationError> errors) {
        return new CodeCheck() {
            @Override
            public boolean isResponsible(Path file) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path file) {
                return errors;
            }

            @Override
            public void resetCache(Path file) {
            }
        };
    }

    private static final class NoopEditorExecutor implements EditorExecutor {
        @Override
        public boolean open(Path path, Position position) {
            return true;
        }

        @Override
        public boolean openAndWait(Path file, Position position) {
            return true;
        }
    }
}

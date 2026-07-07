package de.zorro909.codecheck.validation;

import com.github.javaparser.Position;
import de.zorro909.codecheck.changeset.ChangeSet;
import de.zorro909.codecheck.changeset.ChangeSetEntry;
import de.zorro909.codecheck.changeset.GitFileStatus;
import de.zorro909.codecheck.checks.CodeCheck;
import de.zorro909.codecheck.checks.ValidationError;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultValidationEngineTest {

    @Test
    void validatesChangeSetWithStructuredDiagnosticsWithoutPrinting() {
        Path file = Path.of("src/main/java/Example.java");
        RuleRegistry registry = registry(List.of(new CodeCheck() {
            @Override
            public RuleId ruleId() {
                return new RuleId("test.rule");
            }

            @Override
            public boolean isResponsible(Path checkedFile) {
                return true;
            }

            @Override
            public List<ValidationError> check(Path checkedFile) {
                return List.of(new ValidationError(checkedFile, "structured", new Position(2, 3),
                        ValidationError.Severity.HIGH));
            }

            @Override
            public void resetCache(Path checkedFile) {
            }
        }), List.of());
        ValidationEngine engine = new DefaultValidationEngine(registry);

        ValidationResult result = engine.validate(
                new ChangeSet(
                        List.of(new ChangeSetEntry(file, GitFileStatus.UNKNOWN, false, false, false, false, "test"))),
                ValidationMode.PRE_COMMIT);

        assertThat(result.mode()).isEqualTo(ValidationMode.PRE_COMMIT);
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.file()).isEqualTo(file);
            assertThat(diagnostic.message()).isEqualTo("structured");
            assertThat(diagnostic.position()).isEqualTo(new SourcePosition(2, 3));
            assertThat(diagnostic.severity()).isEqualTo(ValidationError.Severity.HIGH);
            assertThat(diagnostic.kind()).isEqualTo(DiagnosticKind.RULE_VIOLATION);
            assertThat(diagnostic.ruleId()).isEqualTo(new RuleId("test.rule"));
        });
    }

    @Test
    void ruleInterestFiltersFilesBeforeLegacyResponsibilityCheck() {
        CountingCheck check = new CountingCheck();
        RuleRegistry registry = registry(List.of(check), List.of());
        ValidationEngine engine = new DefaultValidationEngine(registry);

        FileValidationResult result = engine.validateFile(Path.of("README.md"), ValidationMode.INTERACTIVE);

        assertThat(result.diagnostics()).isEmpty();
        assertThat(check.checkCalls).isZero();
    }

    @Test
    void registryWatchPlanIncludesValidatedAndContextInterests() {
        CodeCheck check = new CountingCheck();
        RuleRegistry registry = registry(List.of(check), List.of());

        WatchPlan watchPlan = registry.watchPlan();

        assertThat(watchPlan.validatedFiles()).singleElement()
            .satisfies(interest -> assertThat(interest.includeGlobs()).containsExactly("src/main/java/**/*.java"));
    }

    private RuleRegistry registry(List<CodeCheck> checks, List<de.zorro909.codecheck.actions.FixAction> fixActions) {
        return new DefaultRuleRegistry(checks, fixActions);
    }

    private static final class CountingCheck implements CodeCheck {

        private int checkCalls;

        @Override
        public FileInterest validatedFiles() {
            return FileInterest.javaMainSources();
        }

        @Override
        public boolean isResponsible(Path file) {
            return true;
        }

        @Override
        public List<ValidationError> check(Path file) {
            checkCalls++;
            return List.of();
        }

        @Override
        public void resetCache(Path file) {
        }

    }

}

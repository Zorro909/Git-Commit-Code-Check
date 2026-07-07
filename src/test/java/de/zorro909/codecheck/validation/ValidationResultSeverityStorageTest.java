package de.zorro909.codecheck.validation;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationResultSeverityStorageTest {

    @Test
    void assistantModeResultRetainsAllSeverities() {
        Path file = Path.of("Example.java");
        ValidationResult result = new ValidationResult(ValidationMode.ASSISTANT,
                List.of(new FileValidationResult(file, ValidationMode.ASSISTANT,
                        List.of(diagnostic(file, ValidationError.Severity.LOW),
                                diagnostic(file, ValidationError.Severity.MEDIUM),
                                diagnostic(file, ValidationError.Severity.HIGH)))));

        assertThat(result.diagnostics()).extracting(Diagnostic::severity)
            .containsExactly(ValidationError.Severity.LOW, ValidationError.Severity.MEDIUM,
                    ValidationError.Severity.HIGH);
    }

    private Diagnostic diagnostic(Path file, ValidationError.Severity severity) {
        return new Diagnostic(file, severity + " diagnostic", new SourcePosition(1, 1), severity,
                DiagnosticKind.RULE_VIOLATION, new RuleId("test.rule"));
    }

}

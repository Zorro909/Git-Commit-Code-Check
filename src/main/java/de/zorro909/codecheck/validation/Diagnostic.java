package de.zorro909.codecheck.validation;

import de.zorro909.codecheck.checks.ValidationError;

import java.nio.file.Path;

public record Diagnostic(Path file,
                         String message,
                         SourcePosition position,
                         ValidationError.Severity severity,
                         DiagnosticKind kind,
                         RuleId ruleId) {

    public ValidationError toValidationError() {
        return new ValidationError(file, message, position.toJavaParserPosition(), severity);
    }

    public static Diagnostic fromValidationError(ValidationError error, RuleId ruleId) {
        return new Diagnostic(error.filePath(), error.errorMessage(),
                              SourcePosition.from(error.position()), error.severity(),
                              DiagnosticKind.RULE_VIOLATION, ruleId);
    }
}

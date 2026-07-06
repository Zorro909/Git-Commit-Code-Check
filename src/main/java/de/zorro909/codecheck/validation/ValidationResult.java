package de.zorro909.codecheck.validation;

import java.util.List;

public record ValidationResult(ValidationMode mode,
                               List<FileValidationResult> fileResults) {

    public ValidationResult {
        fileResults = List.copyOf(fileResults);
    }

    public List<Diagnostic> diagnostics() {
        return fileResults.stream().flatMap(result -> result.diagnostics().stream()).toList();
    }

    public boolean passed() {
        return diagnostics().isEmpty();
    }
}

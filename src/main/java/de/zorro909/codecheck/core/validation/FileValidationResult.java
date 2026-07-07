package de.zorro909.codecheck.core.validation;

import de.zorro909.codecheck.core.diagnostic.Diagnostic;

import java.nio.file.Path;
import java.util.List;

public record FileValidationResult(Path file, ValidationMode mode, List<Diagnostic> diagnostics) {

    public FileValidationResult {
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean passed() {
        return diagnostics.isEmpty();
    }
}

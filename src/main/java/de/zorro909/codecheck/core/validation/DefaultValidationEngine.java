package de.zorro909.codecheck.core.validation;

import de.zorro909.codecheck.changeset.ChangeSet;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;
import de.zorro909.codecheck.core.validation.rule.RuleRegistry;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.List;

@Singleton
public class DefaultValidationEngine implements ValidationEngine {

    private final RuleRegistry ruleRegistry;

    public DefaultValidationEngine(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public ValidationResult validate(ChangeSet changeSet, ValidationMode mode) {
        List<FileValidationResult> fileResults = changeSet.paths().map(file -> validateFile(file, mode)).toList();
        return new ValidationResult(mode, fileResults);
    }

    @Override
    public FileValidationResult validateFile(Path file, ValidationMode mode) {
        ValidationContext context = new ValidationContext(mode);
        List<Diagnostic> diagnostics = ruleRegistry.activeRules()
            .stream()
            .filter(rule -> rule.validatedFiles().matches(file))
            .flatMap(rule -> rule.check(context, file).stream())
            .toList();
        return new FileValidationResult(file, mode, diagnostics);
    }

}

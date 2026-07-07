package de.zorro909.codecheck.legacy.adapter;

import de.zorro909.codecheck.legacy.checks.CodeCheck;
import de.zorro909.codecheck.validation.Diagnostic;
import de.zorro909.codecheck.validation.FileInterest;
import de.zorro909.codecheck.validation.Rule;
import de.zorro909.codecheck.validation.RuleId;
import de.zorro909.codecheck.validation.RuleMetadata;
import de.zorro909.codecheck.validation.ValidationContext;

import java.nio.file.Path;
import java.util.List;

public final class CodeCheckRuleAdapter implements Rule {

    private final CodeCheck codeCheck;

    public CodeCheckRuleAdapter(CodeCheck codeCheck) {
        this.codeCheck = codeCheck;
    }

    @Override
    public RuleId id() {
        return codeCheck.ruleId();
    }

    @Override
    public RuleMetadata metadata() {
        return codeCheck.ruleMetadata();
    }

    @Override
    public FileInterest validatedFiles() {
        return codeCheck.validatedFiles();
    }

    @Override
    public FileInterest contextFiles() {
        return codeCheck.contextFiles();
    }

    @Override
    public List<Diagnostic> check(ValidationContext context, Path file) {
        codeCheck.resetCache(file);
        if (!codeCheck.isResponsible(file)) {
            return List.of();
        }
        return codeCheck.check(file).stream().map(error -> Diagnostic.fromValidationError(error, id())).toList();
    }

}

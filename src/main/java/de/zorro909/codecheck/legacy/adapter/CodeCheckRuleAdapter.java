package de.zorro909.codecheck.legacy.adapter;

import de.zorro909.codecheck.legacy.checks.CodeCheck;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;
import de.zorro909.codecheck.core.validation.rule.FileInterest;
import de.zorro909.codecheck.core.validation.rule.Rule;
import de.zorro909.codecheck.core.validation.rule.RuleId;
import de.zorro909.codecheck.core.validation.rule.RuleMetadata;
import de.zorro909.codecheck.core.validation.ValidationContext;

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

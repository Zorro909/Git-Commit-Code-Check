package de.zorro909.codecheck.validation;

import de.zorro909.codecheck.actions.PostAction;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Singleton
public class FixApplicationService {

    private final RuleRegistry ruleRegistry;

    private final ValidationEngine validationEngine;

    private final List<PostAction> postActions;

    public FixApplicationService(RuleRegistry ruleRegistry, ValidationEngine validationEngine,
            List<PostAction> postActions) {
        this.ruleRegistry = ruleRegistry;
        this.validationEngine = validationEngine;
        this.postActions = postActions == null ? List.of() : List.copyOf(postActions);
    }

    public FixResult applyUserSelectedFix(Diagnostic diagnostic, ValidationMode mode, FixerId fixerId) {
        Fixer fixer = ruleRegistry.activeFixers()
            .stream()
            .filter(candidate -> candidate.id().equals(fixerId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown fixer " + fixerId.value()));
        if (!fixer.availableIn(mode) || !fixer.canFix(diagnostic)) {
            return FixResult.notApplied("Fixer is unavailable for " + mode);
        }

        FixResult fixResult = fixer.apply(fixer.plan(diagnostic));
        if (!fixResult.applied()) {
            return fixResult;
        }

        Set<Path> affectedFiles = fixResult.affectedFiles();
        boolean recheckPassed = affectedFiles.stream()
            .map(file -> validationEngine.validateFile(file, mode))
            .allMatch(FileValidationResult::passed);
        if (recheckPassed) {
            postActions.forEach(action -> action.perform(affectedFiles));
            return fixResult.withRestaged(true);
        }

        return fixResult.withRestaged(false);
    }

}

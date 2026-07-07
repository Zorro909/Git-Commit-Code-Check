package de.zorro909.codecheck.validation;

import de.zorro909.codecheck.actions.FixAction;

final class FixActionFixerAdapter implements Fixer {

    private final FixAction fixAction;

    FixActionFixerAdapter(FixAction fixAction) {
        this.fixAction = fixAction;
    }

    @Override
    public FixerId id() {
        return fixAction.fixerId();
    }

    @Override
    public FixerMetadata metadata() {
        return fixAction.fixerMetadata();
    }

    @Override
    public boolean canFix(Diagnostic diagnostic) {
        return fixAction.canFixError(diagnostic.toValidationError());
    }

    @Override
    public FixPlan plan(Diagnostic diagnostic) {
        return new FixPlan(diagnostic, id());
    }

    @Override
    public FixResult apply(FixPlan plan) {
        boolean fixed = fixAction.fixError(plan.diagnostic().toValidationError());
        return fixed ? FixResult.applied(fixAction.affectedFiles(plan.diagnostic().toValidationError()))
                : FixResult.notApplied("Fix action returned false");
    }

}

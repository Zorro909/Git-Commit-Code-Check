package de.zorro909.codecheck.validation;

public interface Fixer {

    FixerId id();

    FixerMetadata metadata();

    boolean canFix(Diagnostic diagnostic);

    FixPlan plan(Diagnostic diagnostic);

    FixResult apply(FixPlan plan);

    default boolean availableIn(ValidationMode mode) {
        return metadata().interaction() != FixInteraction.INTERACTIVE || mode.allowsInteractiveFixers();
    }
}

package de.zorro909.codecheck.core.validation.fix;

import de.zorro909.codecheck.core.diagnostic.Diagnostic;
import de.zorro909.codecheck.core.validation.ValidationMode;

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

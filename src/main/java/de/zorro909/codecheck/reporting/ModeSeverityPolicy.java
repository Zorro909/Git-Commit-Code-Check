package de.zorro909.codecheck.reporting;

import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.validation.ValidationMode;
import jakarta.inject.Singleton;

@Singleton
public class ModeSeverityPolicy {

    public boolean visible(ValidationMode mode, ValidationError.Severity severity) {
        return switch (mode) {
            case ASSISTANT, INTERACTIVE, BATCH -> true;
            case PRE_COMMIT -> severity != ValidationError.Severity.LOW;
        };
    }

    public boolean blocks(ValidationMode mode, ValidationError.Severity severity) {
        return switch (mode) {
            case BATCH, PRE_COMMIT -> severity == ValidationError.Severity.HIGH;
            case INTERACTIVE -> true;
            case ASSISTANT -> false;
        };
    }

}

package de.zorro909.codecheck.core.validation;

public enum ValidationMode {

    ASSISTANT, INTERACTIVE, BATCH, PRE_COMMIT;

    public boolean allowsInteractiveFixers() {
        return this == ASSISTANT || this == INTERACTIVE;
    }

}

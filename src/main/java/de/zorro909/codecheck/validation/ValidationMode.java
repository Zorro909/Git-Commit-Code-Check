package de.zorro909.codecheck.validation;

public enum ValidationMode {

    ASSISTANT, INTERACTIVE, BATCH, PRE_COMMIT;

    public boolean allowsInteractiveFixers() {
        return this == ASSISTANT || this == INTERACTIVE;
    }

}

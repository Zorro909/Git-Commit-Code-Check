package de.zorro909.codecheck.actions;

import de.zorro909.codecheck.checks.ValidationError;

public interface FixAction {

    boolean canFixError(ValidationError validationError);

    boolean fixError(ValidationError validationError);

}

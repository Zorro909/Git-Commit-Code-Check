package de.zorro909.codecheck.actions;

import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.validation.FixInteraction;
import de.zorro909.codecheck.validation.FixerId;
import de.zorro909.codecheck.validation.FixerMetadata;

import java.nio.file.Path;
import java.util.Set;

public interface FixAction {

    default FixerId fixerId() {
        return new FixerId(getClass().getSimpleName());
    }

    default FixerMetadata fixerMetadata() {
        return new FixerMetadata(getClass().getSimpleName(), FixInteraction.NONE);
    }

    boolean canFixError(ValidationError validationError);

    boolean fixError(ValidationError validationError);

    default Set<Path> affectedFiles(ValidationError validationError) {
        return Set.of(validationError.filePath());
    }

}

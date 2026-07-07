package de.zorro909.codecheck.legacy.actions;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.validation.fix.FixInteraction;
import de.zorro909.codecheck.core.validation.fix.FixerId;
import de.zorro909.codecheck.core.validation.fix.FixerMetadata;

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

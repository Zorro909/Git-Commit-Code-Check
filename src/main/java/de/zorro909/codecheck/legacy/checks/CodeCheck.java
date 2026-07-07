package de.zorro909.codecheck.legacy.checks;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.validation.FileInterest;
import de.zorro909.codecheck.validation.RuleId;
import de.zorro909.codecheck.validation.RuleMetadata;

import java.nio.file.Path;
import java.util.List;

/**
 * The CodeCheck interface provides methods to check code files for certain conditions and
 * validate them.
 */
public interface CodeCheck {

    default RuleId ruleId() {
        return new RuleId(getClass().getSimpleName());
    }

    default RuleMetadata ruleMetadata() {
        return new RuleMetadata(getClass().getSimpleName(), "Legacy validation check");
    }

    default FileInterest validatedFiles() {
        return FileInterest.any();
    }

    default FileInterest contextFiles() {
        return FileInterest.none();
    }

    boolean isResponsible(Path file);

    List<ValidationError> check(Path file);

    void resetCache(Path file);

}

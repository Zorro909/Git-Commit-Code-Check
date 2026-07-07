package de.zorro909.codecheck.core.validation;

import de.zorro909.codecheck.core.changeset.ChangeSet;

import java.nio.file.Path;

public interface ValidationEngine {

    ValidationResult validate(ChangeSet changeSet, ValidationMode mode);

    FileValidationResult validateFile(Path file, ValidationMode mode);

}

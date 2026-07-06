package de.zorro909.codecheck.validation;

import de.zorro909.codecheck.changeset.ChangeSet;

import java.nio.file.Path;

public interface ValidationEngine {

    ValidationResult validate(ChangeSet changeSet, ValidationMode mode);

    FileValidationResult validateFile(Path file, ValidationMode mode);
}

package de.zorro909.codecheck.validation;

import java.nio.file.Path;
import java.util.List;

public interface Rule {

    RuleId id();

    RuleMetadata metadata();

    FileInterest validatedFiles();

    FileInterest contextFiles();

    List<Diagnostic> check(ValidationContext context, Path file);
}

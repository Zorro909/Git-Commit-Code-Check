package de.zorro909.codecheck.actions;

import de.zorro909.codecheck.checks.ValidationError;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface PostAction {

    boolean perform(Map<Path, List<ValidationError>> validationErrors);

}

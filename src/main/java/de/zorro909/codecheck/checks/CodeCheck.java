package de.zorro909.codecheck.checks;

import java.nio.file.Path;
import java.util.List;

/**
 * The CodeCheck interface provides methods to check code files for certain conditions and validate them.
 */
public interface CodeCheck {

    boolean isResponsible(Path file);

    List<ValidationError> check(Path file);

    void resetCache(Path file);

}

package de.zorro909.codecheck;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * The CodeCheck interface provides methods to check code files for certain conditions and validate them.
 */
public interface CodeCheck {

    boolean isResponsible(Path file);

    List<ValidationError> check(Path file);

    // Test
    void resetCache(Path file);

}

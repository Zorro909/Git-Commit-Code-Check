package de.zorro909.codecheck;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface CodeCheck {

    boolean isResponsible(Path file);

    List<ValidationError> check(Path file);

    void resetCache(Path file);

}

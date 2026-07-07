package de.zorro909.codecheck.core.validation.fix;

import java.nio.file.Path;
import java.util.Set;

public record FixResult(boolean applied, Set<Path> affectedFiles, boolean restaged, String message) {

    public FixResult {
        affectedFiles = Set.copyOf(affectedFiles);
    }

    public static FixResult applied(Set<Path> affectedFiles) {
        return new FixResult(true, affectedFiles, false, "");
    }

    public static FixResult notApplied(String message) {
        return new FixResult(false, Set.of(), false, message);
    }

    public FixResult withRestaged(boolean restaged) {
        return new FixResult(applied, affectedFiles, restaged, message);
    }
}

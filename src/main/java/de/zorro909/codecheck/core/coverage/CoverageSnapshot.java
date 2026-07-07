package de.zorro909.codecheck.core.coverage;

import java.util.Map;
import java.util.Optional;

public record CoverageSnapshot(Map<String, ClassCoverage> classes) {

    public CoverageSnapshot {
        classes = Map.copyOf(classes);
    }

    public Optional<ClassCoverage> classCoverage(String className) {
        return Optional.ofNullable(classes.get(className));
    }
}

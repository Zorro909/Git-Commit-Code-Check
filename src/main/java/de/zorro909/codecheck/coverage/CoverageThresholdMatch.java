package de.zorro909.codecheck.coverage;

public record CoverageThresholdMatch(String annotation,
                                     String glob,
                                     String className,
                                     String packageName) {

    public CoverageThresholdMatch(String annotation, String glob) {
        this(annotation, glob, null, null);
    }
}

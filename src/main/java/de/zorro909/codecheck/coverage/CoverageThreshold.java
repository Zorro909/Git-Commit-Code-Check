package de.zorro909.codecheck.coverage;

public record CoverageThreshold(CoverageThresholdMatch match,
                                double line,
                                double branch) {
}

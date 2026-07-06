package de.zorro909.codecheck.coverage;

public record ClassCoverage(String className,
                            CoverageMetric line,
                            CoverageMetric branch) {
}

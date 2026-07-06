package de.zorro909.codecheck.coverage;

public record CoverageMetric(int missed,
                             int covered) {

    public double ratio() {
        int total = missed + covered;
        return total == 0 ? 1.0 : (double) covered / total;
    }
}

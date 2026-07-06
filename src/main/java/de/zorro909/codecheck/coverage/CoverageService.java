package de.zorro909.codecheck.coverage;

public interface CoverageService {

    CoverageSnapshot currentCoverage(CoverageRequest request);

    CoverageFreshness freshness(CoverageRequest request);

    CoverageSnapshot refreshCoverage(CoverageRequest request);
}
